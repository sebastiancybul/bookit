package com.bookit.auth.service;

import com.bookit.auth.dto.LoginRequest;
import com.bookit.auth.dto.RegisterRequest;
import com.bookit.user.dto.UserResponse;
import java.util.UUID;

public interface AuthService {

    LoginResult register(RegisterRequest request);

    LoginResult login(LoginRequest request);

    UserResponse me(UUID userId);

    String reissueToken(UUID userId);

    record LoginResult(UserResponse user, String accessToken) {
    }
}
