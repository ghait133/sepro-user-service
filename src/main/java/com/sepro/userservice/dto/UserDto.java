package com.sepro.userservice.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sepro.userservice.validation.PasswordMatches;
import com.sepro.userservice.validation.ValidEmail;
import com.sepro.userservice.validation.ValidPassword;

@PasswordMatches
public class UserDto {


    @ValidPassword
    private String password;

    @NotNull
    @Size(min = 1)
    private String confirmPassword;

    @ValidEmail
    @NotNull
    @Size(min = 1, message = "{Size.userDto.email}")
    private String email;

    private String role;


    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("UserDto [username=").append(email).append(", password=").append(password).append(", matchingPassword=").append(confirmPassword).append(", email=").append(email).append(", role=").append(role).append("]");
        return builder.toString();
    }

}
