package com.example.demo.dto.response;

import lombok.Data;

@Data
public class ParticipantResponse {
    private Long userId;
    private String username;
    private String displayName;
}
