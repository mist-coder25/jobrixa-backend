package com.jobrixa.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationEventResponse {
    private UUID id;
    private String eventType;
    private String description;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}
