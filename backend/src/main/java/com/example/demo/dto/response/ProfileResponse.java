package com.example.demo.dto.response;

import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

@Data
public class ProfileResponse {
    private Long id;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private String location;
    private String website;
    private LocalDate dateOfBirth;
    private Instant createdAt;
    private Instant updatedAt;
}
