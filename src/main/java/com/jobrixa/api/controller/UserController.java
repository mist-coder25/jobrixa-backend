package com.jobrixa.api.controller;

import com.jobrixa.api.dto.UserProfileRequest;
import com.jobrixa.api.dto.UserProfileResponse;
import com.jobrixa.api.entity.User;
import com.jobrixa.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        User user = (User) userDetails;
        return ResponseEntity.ok(mapToResponse(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMe(
            @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        User user = (User) userDetails;

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getCollege() != null) {
            user.setCollege(request.getCollege());
        }
        if (request.getGraduationYear() != null) {
            user.setGraduationYear(request.getGraduationYear());
        }
        if (request.getLinkedinUrl() != null) {
            user.setLinkedinUrl(request.getLinkedinUrl());
        }
        if (request.getTargetCtcMin() != null) {
            user.setTargetCtcMin(request.getTargetCtcMin());
        }
        if (request.getTargetCtcMax() != null) {
            user.setTargetCtcMax(request.getTargetCtcMax());
        }

        userRepository.save(user);
        return ResponseEntity.ok(mapToResponse(user));
    }

    private UserProfileResponse mapToResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .college(user.getCollege())
                .graduationYear(user.getGraduationYear())
                .linkedinUrl(user.getLinkedinUrl())
                .targetCtcMin(user.getTargetCtcMin())
                .targetCtcMax(user.getTargetCtcMax())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
