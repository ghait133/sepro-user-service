package com.sepro.userservice.resource;

import com.sepro.userservice.dto.PasswordDto;
import com.sepro.userservice.dto.UserDto;
import com.sepro.userservice.entity.Role;
import com.sepro.userservice.entity.User;
import com.sepro.userservice.entity.VerificationToken;
import com.sepro.userservice.error.InvalidOldPasswordException;
import com.sepro.userservice.model.CustomPrincipal;
import com.sepro.userservice.registration.OnRegistrationCompleteEvent;
import com.sepro.userservice.repository.RoleRepository;
import com.sepro.userservice.service.ISecurityUserService;
import com.sepro.userservice.service.IUserService;
import com.sepro.userservice.util.GenericResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.util.*;

@RestController
public class RegistrationController {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    @Autowired
    private IUserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private Environment env;

    @Autowired
    private ISecurityUserService securityUserService;

    @RequestMapping(value = "/registration", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public GenericResponse registerUserAccount(@Valid @RequestBody final UserDto accountDto, final HttpServletRequest request) {
        LOGGER.debug("Registering user account with information: {}", accountDto);

        final User registered = userService.registerNewUserAccount(accountDto);
        eventPublisher.publishEvent(new OnRegistrationCompleteEvent(registered, request.getLocale(), getAppUrl(request)));
        return new GenericResponse("success");
    }
    // Confirme registration
    @RequestMapping(value = "/registrationConfirm", method = RequestMethod.GET)
    public String confirmRegistration(@RequestParam("token") final String token) throws UnsupportedEncodingException {
        final String result = userService.validateVerificationToken(token);
        if (result.equals("valid")) {
            final User user = userService.getUser(token);
            // if (user.isUsing2FA()) {
            // model.addAttribute("qr", userService.generateQRUrl(user));
            // return "redirect:/qrcode.html?lang=" + locale.getLanguage();
            // }
            authWithoutPassword(user);
            return "success";
        }
        return "error";
    }
    //Resend Registrationtoken
    @RequestMapping(value = "/user/resendRegistrationToken", method = RequestMethod.GET)
    @ResponseBody
    public GenericResponse resendRegistrationToken(final HttpServletRequest request, @RequestParam("token") final String existingToken) {
        final VerificationToken newToken = userService.generateNewVerificationToken(existingToken); //<<---- we have to put the right link in a Button in the UI
        final User user = userService.getUser(newToken.getToken());
        mailSender.send(constructResendVerificationTokenEmail(getAppUrl(request), request.getLocale(), newToken, user));
        return new GenericResponse("We will send an email with a new registration token to your email account");
    }

    // Reset password
    @RequestMapping(value = "/user/resetPassword", method = RequestMethod.POST)
    @ResponseBody
    public GenericResponse resetPassword(final HttpServletRequest request, @RequestParam("email") final String userEmail) {
        final User user = userService.findUserByEmail(userEmail);
        if (user != null) {
            final String token = UUID.randomUUID().toString();
            userService.createPasswordResetTokenForUser(user, token);
            mailSender.send(constructResetTokenEmail(getAppUrl(request), request.getLocale(), token, user));
        }
        return new GenericResponse("Reset Password");
    }

    @RequestMapping(value = "/user/changePassword", method = RequestMethod.GET)
    public String showChangePasswordPage(@RequestParam("id") final long id, @RequestParam("token") final String token) {
        final String result = securityUserService.validatePasswordResetToken(id, token);
        if (result != null) {

            return "error";
        }
        return "redirect to update password form";
    }

    @RequestMapping(value = "/user/savePassword", method = RequestMethod.POST)
    @ResponseBody
    public GenericResponse savePassword(@Valid @RequestBody PasswordDto passwordDto, @RequestParam final String token, @RequestParam final long id) {
        final String result = securityUserService.validatePasswordResetToken(id, token);
        if (result != null) {

            return new GenericResponse("error");
        }
        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.changeUserPassword(user, passwordDto.getNewPassword());
        return new GenericResponse("Password reset successfully");
    }

    // change user password
    @RequestMapping(value = "/user/updatePassword", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('role_partner','role_customer')")
    public GenericResponse changeUserPassword(@Valid @RequestBody PasswordDto passwordDto) {
        CustomPrincipal principal = (CustomPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = principal.getEmail();
        final User user = userService.findUserByEmail(email);
        if (!userService.checkIfValidOldPassword(user, passwordDto.getOldPassword())) {
            throw new InvalidOldPasswordException();
        }
        userService.changeUserPassword(user, passwordDto.getNewPassword());
        return new GenericResponse("Password updated successfully");
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = "/roles",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Role getUserByUserName (){
        Role user = roleRepository.findByName("role_admin");
        return user;
    }
//-------------------------------- UTILS ---------------------------------- //
    private String getAppUrl(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }

    public void authWithoutPassword(User user) {
        //List<Privilege> privileges = user.getRoles().stream().map(role -> role.getPrivileges()).flatMap(list -> list.stream()).distinct().collect(Collectors.toList());
        //List<GrantedAuthority> authorities = privileges.stream().map(p -> new SimpleGrantedAuthority(p.getName())).collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale, final VerificationToken newToken, final User user) {
        final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        final String message = "We will send an email with a new registration token to your email account";
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, user);
    }

    private SimpleMailMessage constructEmail(String subject, String body, User user) {
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(user.getEmail());
        email.setFrom(env.getProperty("support.email"));
        return email;
    }
    private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale, final String token, final User user) {
        final String url = contextPath + "/user/changePassword?id=" + user.getId() + "&token=" + token;
        final String message = "Reset Password";
        return constructEmail("Reset Password", message + " \r\n" + url, user);
    }
}
