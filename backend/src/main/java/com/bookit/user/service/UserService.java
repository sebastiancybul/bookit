package com.bookit.user.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    Optional<UUID> findIdByEmail(String email);

    Map<UUID, String> getEmails(Collection<UUID> userIds);

    void addRole(UUID userId, String roleName);

    void removeRole(UUID userId, String roleName);
}
