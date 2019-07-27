package com.sepro.userservice.service;

import com.sepro.userservice.dto.UserDto;
import com.sepro.userservice.entity.PasswordResetToken;
import com.sepro.userservice.entity.Role;
import com.sepro.userservice.entity.User;
import com.sepro.userservice.entity.VerificationToken;
import com.sepro.userservice.error.UserAlreadyExistException;
import com.sepro.userservice.repository.PasswordResetTokenRepository;
import com.sepro.userservice.repository.RoleRepository;
import com.sepro.userservice.repository.UserRepository;
import com.sepro.userservice.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.UnsupportedEncodingException;
import java.util.*;

import javax.transaction.Transactional;


@Service
@Transactional
public class UserService implements IUserService {

    @Override
    public VerificationToken getVerificationToken(String VerificationToken) {
        return null;
    }

    @Override
    public VerificationToken generateNewVerificationToken(String existingVerificationToken) {
        VerificationToken vToken = tokenRepository.findByToken(existingVerificationToken);
        vToken.updateToken(UUID.randomUUID()
                .toString());
        vToken = tokenRepository.save(vToken);
        return vToken;
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    //@Autowired
    //private PasswordResetTokenRepository passwordTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordTokenRepository;

    //@Autowired
    //private SessionRegistry sessionRegistry;

    public static final String TOKEN_INVALID = "invalidToken";
    public static final String TOKEN_EXPIRED = "expired";
    public static final String TOKEN_VALID = "valid";

    public static String QR_PREFIX = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=";
    public static String APP_NAME = "SpringRegistration";

    // API

    @Override
    public User registerNewUserAccount(final UserDto accountDto) {
        if (emailExists(accountDto.getEmail())) {
            throw new UserAlreadyExistException("There is an account with that email adress: " + accountDto.getEmail());
        }
        final User user = new User();
        user.setPassword(passwordEncoder.encode(accountDto.getPassword()));
        user.setEmail(accountDto.getEmail());
        user.setUsername(accountDto.getEmail());
        user.setRoles(Arrays.asList(roleRepository.findByName("role_admin")));
        return userRepository.save(user);
    }
    private boolean emailExists(final String email) {
        return userRepository.findByEmail(email) != null;
    }
    @Override
    public User getUser(String verificationToken) {
        final VerificationToken token = tokenRepository.findByToken(verificationToken);
        if (token != null) {
            return token.getUser();
        }
        return null;
    }


    @Override
    public void saveRegisteredUser(final User user) {
        userRepository.save(user);
    }

    @Override
    public void deleteUser(User user) {
        final VerificationToken verificationToken = tokenRepository.findByUser(user);

        if (verificationToken != null) {
            tokenRepository.delete(verificationToken);
        }

        /*final PasswordResetToken passwordToken = passwordTokenRepository.findByUser(user);

        if (passwordToken != null) {
            passwordTokenRepository.delete(passwordToken);
        }*/

        userRepository.delete(user);

    }

    @Override
    public void createVerificationTokenForUser(User user, String token) {
        final VerificationToken mytoken = new VerificationToken( token, user);
        tokenRepository.save(mytoken);

    }

    @Override
    public void createPasswordResetTokenForUser(User user, String token) {
        final PasswordResetToken myToken = new PasswordResetToken(token, user);
        passwordTokenRepository.save(myToken);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User getUserByPasswordResetToken(String token) {
        return null;
    }

    @Override
    public Optional<User> getUserByID(long id) {
        return Optional.empty();
    }

    @Override
    public void changeUserPassword(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Override
    public boolean checkIfValidOldPassword(User user, String oldPassword) {

        return passwordEncoder.matches(oldPassword, user.getPassword());

    }

    @Override
    public String validateVerificationToken(String token) {
        final VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken == null) {
            return TOKEN_INVALID;
        }

        final User user = verificationToken.getUser();
        final Calendar cal = Calendar.getInstance();
        if ((verificationToken.getExpiryDate()
                .getTime()
                - cal.getTime()
                .getTime()) <= 0) {
            tokenRepository.delete(verificationToken);
            return TOKEN_EXPIRED;
        }

        user.setEnabled(true);
        // tokenRepository.delete(verificationToken);
        userRepository.save(user);
        return TOKEN_VALID;
    }

    @Override
    public String generateQRUrl(User user) throws UnsupportedEncodingException {
        return null;
    }

    @Override
    public User updateUser2FA(boolean use2FA) {
        return null;
    }

    @Override
    public List<String> getUsersFromSessionRegistry() {
        return null;
    }


}