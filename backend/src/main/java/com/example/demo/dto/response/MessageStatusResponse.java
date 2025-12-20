package com.example.demo.dto.response;

import com.example.demo.enums.DeliveryStatus;
import lombok.Data;
import java.time.Instant;

@Data
public class MessageStatusResponse {
    private Long userId;
    private DeliveryStatus status;
    private Instant timestamp;
}
