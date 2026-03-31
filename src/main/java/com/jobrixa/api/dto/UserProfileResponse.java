package com.jobrixa.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String name;
    private String email;
    private String college;
    private Short graduationYear;
    private String linkedinUrl;
    private Integer targetCtcMin;
    private Integer targetCtcMax;
    private String avatarUrl;
    private String plan;
    private Boolean isEarlyAdopter;
<<<<<<< HEAD
    private LocalDateTime earlyAdopterExpiresAt;
=======
    private java.time.LocalDateTime earlyAdopterExpiresAt;
>>>>>>> b4b2e73 (feat: beta launch early adopter logic and no app limits)
}
