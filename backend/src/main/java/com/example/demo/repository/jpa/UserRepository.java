package com.example.demo.repository.jpa;

import com.example.demo.entities.User;
import com.example.demo.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByIdAndStatus(Long id, EntityStatus status);
    List<User> findByStatus(EntityStatus status);
    List<User> findByStatusAndDeletedAtBefore(EntityStatus status, Instant deletedAt);
}
