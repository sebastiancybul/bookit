package com.bookit.user.service.impl;

import com.bookit.shared.error.ResourceNotFoundException;
import com.bookit.user.entity.Role;
import com.bookit.user.entity.User;
import com.bookit.user.repository.RoleRepository;
import com.bookit.user.repository.UserRepository;
import com.bookit.user.service.UserService;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public Optional<UUID> findIdByEmail(String email) {
        return userRepository.findIdByEmail(email.toLowerCase(Locale.ROOT));
    }

    @Override
    public Map<UUID, String> getEmails(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserRepository.IdAndEmail::getId, UserRepository.IdAndEmail::getEmail));
    }

    @Override
    @Transactional
    public void addRole(UUID userId, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role " + roleName + " is missing"));
        findUser(userId).addRole(role);
    }

    @Override
    @Transactional
    public void removeRole(UUID userId, String roleName) {
        findUser(userId).removeRole(roleName);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
