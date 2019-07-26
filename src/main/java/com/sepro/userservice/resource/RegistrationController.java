package com.sepro.userservice.resource;

import com.sepro.userservice.dto.UserDto;
import com.sepro.userservice.entity.Role;
import com.sepro.userservice.entity.User;
import com.sepro.userservice.model.CustomPrincipal;
import com.sepro.userservice.repository.RoleRepository;
import com.sepro.userservice.service.IUserService;
import com.sepro.userservice.util.GenericResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
public class RegistrationController {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    @Autowired
    private IUserService userService;
    @Autowired
    private RoleRepository roleRepository;

    @RequestMapping(value = "/registration", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public GenericResponse registerUserAccount(@Valid @RequestBody final UserDto accountDto, final HttpServletRequest request) {
        LOGGER.debug("Registering user account with information: {}", accountDto);

        final User registered = userService.registerNewUserAccount(accountDto);
        //eventPublisher.publishEvent(new OnRegistrationCompleteEvent(registered, request.getLocale(), getAppUrl(request)));
        return new GenericResponse("success");
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

}
