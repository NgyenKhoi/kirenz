package com.example.demo.entities;

import com.example.demo.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import java.time.Instant;
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ColumnDefault("false")
    @Column(name = "is_premium", nullable = false)
    private Boolean isPremium = false;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status = EntityStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "last_seen")
    private Instant lastSeen;

}