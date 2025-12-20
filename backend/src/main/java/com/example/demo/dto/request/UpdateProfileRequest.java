package com.example.demo.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String avatarUrl;
    private String bio;
    private LocalDate birthday;
}
