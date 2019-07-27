package com.sepro.userservice.service;

public interface ISecurityUserService {
    String validatePasswordResetToken(long id, String token);
}
