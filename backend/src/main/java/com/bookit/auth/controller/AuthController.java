package com.bookit.auth.controller;

import com.bookit.auth.dto.LoginRequest;
import com.bookit.auth.dto.RegisterRequest;
import com.bookit.auth.security.AuthCookies;
import com.bookit.auth.security.CurrentUserId;
import com.bookit.auth.service.AuthService;
import com.bookit.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = "/register", version = "1")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.LoginResult result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, authCookies.accessToken(result.accessToken()).toString())
                .body(result.user());
    }

    @PostMapping(path = "/login", version = "1")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookies.accessToken(result.accessToken()).toString())
                .body(result.user());
    }

    @PostMapping(path = "/logout", version = "1")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.clearAccessToken().toString())
                .build();
    }

    @GetMapping(path = "/me", version = "1")
    public UserResponse me(@CurrentUserId UUID userId) {
        return authService.me(userId);
    }
}
