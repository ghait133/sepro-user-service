package com.sepro.userservice.resource;


import com.sepro.userservice.entity.User;
import com.sepro.userservice.model.CustomPrincipal;
import com.sepro.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;

//Api Path is "/user"
@RestController
public class UserResource {

    @Autowired
    UserRepository usersRepository;

    @RequestMapping(
            method = RequestMethod.GET,
            path = "/{username}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("#username == authentication.principal.username")
    public User getUserByUserName (@PathVariable String username, CustomPrincipal principal){
        User user = usersRepository.findByUsername(username);
        return user;
    }

    @RequestMapping(
            method = RequestMethod.POST,
            path = "/create",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String createUser(@RequestBody User newUser){
        usersRepository.save(newUser);
        return "User " + newUser.getUsername() +" has been created";
    }

}
