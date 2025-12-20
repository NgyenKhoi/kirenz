package com.example.demo.repository.jpa;

import com.example.demo.entities.Profile;
import com.example.demo.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUser_Id(Long userId);
    Optional<Profile> findByUser_IdAndStatus(Long userId, EntityStatus status);
    List<Profile> findByStatusAndDeletedAtBefore(EntityStatus status, Instant deletedAt);
}
