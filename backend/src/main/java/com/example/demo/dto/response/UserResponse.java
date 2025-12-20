package com.example.demo.dto.response;

import lombok.Data;
import java.time.Instant;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;
    private ProfileResponse profile; // Optional field
}
