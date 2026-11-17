package com.bookit.auth.service.impl;

import com.bookit.auth.dto.LoginRequest;
import com.bookit.auth.security.JwtService;
import com.bookit.auth.service.AuthService;
import com.bookit.shared.error.ResourceNotFoundException;
import com.bookit.user.dto.UserResponse;
import com.bookit.user.entity.User;
import com.bookit.user.repository.UserRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public LoginResult login(LoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return new LoginResult(UserResponse.from(user), jwtService.issueToken(user));
    }

    @Override
    public UserResponse me(UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
