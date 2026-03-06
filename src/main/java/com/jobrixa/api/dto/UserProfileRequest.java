package com.jobrixa.api.dto;

import lombok.Data;

@Data
public class UserProfileRequest {
    private String name;
    private String college;
    private Short graduationYear;
    private String linkedinUrl;
    private Integer targetCtcMin;
    private Integer targetCtcMax;
}
