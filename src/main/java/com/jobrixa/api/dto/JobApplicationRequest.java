package com.jobrixa.api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class JobApplicationRequest {
    private UUID userId; 
    
    @NotBlank
    private String companyName;
    
    private String companyDomain;
    
    @NotBlank
    private String jobTitle;
    
    private String jobUrl;
    private String location;
    private Boolean isRemote = false;
    private String status = "SAVED"; 
    private String source = "MANUAL";
    private String priority = "MEDIUM";
    private String jobDescription;
    private Integer salaryMin;
    private Integer salaryMax;
    private LocalDate appliedAt;
    private LocalDate deadline;
    private java.util.List<String> tags;
}
