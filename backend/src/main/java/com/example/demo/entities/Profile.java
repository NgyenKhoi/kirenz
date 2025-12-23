package com.example.demo.entities;

import com.example.demo.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "profiles")
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url", length = Integer.MAX_VALUE)
    private String avatarUrl;

    @Column(name = "bio", length = Integer.MAX_VALUE)
    private String bio;

    @Column(name = "location")
    private String location;

    @Column(name = "website")
    private String website;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status = EntityStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

}