package com.bookit.auth.service;

import com.bookit.auth.dto.LoginRequest;
import com.bookit.user.dto.UserResponse;
import java.util.UUID;

public interface AuthService {

    LoginResult login(LoginRequest request);

    UserResponse me(UUID userId);

    record LoginResult(UserResponse user, String accessToken) {
    }
}
