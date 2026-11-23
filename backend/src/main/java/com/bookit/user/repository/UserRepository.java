package com.bookit.user.repository;

import com.bookit.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u.id from User u where u.email = :email")
    Optional<UUID> findIdByEmail(String email);

    List<IdAndEmail> findByIdIn(Collection<UUID> ids);

    interface IdAndEmail {

        UUID getId();

        String getEmail();
    }
}
