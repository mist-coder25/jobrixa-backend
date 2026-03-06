package com.jobrixa.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JobApplicationResponse {
    private UUID id;
    private String companyName;
    private String jobTitle;
    private String jobUrl;
    private String location;
    private Boolean isRemote;
    private String status;
    private String source;
    private String priority;
    private Integer salaryMin;
    private Integer salaryMax;
    private LocalDate appliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private java.util.List<String> tags;
}
