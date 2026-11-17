package com.bookit.user.dto;

import com.bookit.user.entity.Role;
import com.bookit.user.entity.User;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Set<String> roles) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }
}
