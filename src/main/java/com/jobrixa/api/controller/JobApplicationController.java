package com.jobrixa.api.controller;

import com.jobrixa.api.dto.JobApplicationRequest;
import com.jobrixa.api.dto.JobApplicationResponse;
import com.jobrixa.api.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService applicationService;

    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(
            @RequestBody JobApplicationRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        com.jobrixa.api.entity.User user = (com.jobrixa.api.entity.User) userDetails;
        request.setUserId(user.getId());
        return ResponseEntity.ok(applicationService.createApplication(request));
    }

    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> getApplications(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        com.jobrixa.api.entity.User user = (com.jobrixa.api.entity.User) userDetails;
        return ResponseEntity.ok(applicationService.getActiveApplications(user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> updateApplication(
            @PathVariable UUID id, 
            @RequestBody JobApplicationRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        com.jobrixa.api.entity.User user = (com.jobrixa.api.entity.User) userDetails;
        return ResponseEntity.ok(applicationService.updateApplication(id, user.getId(), request));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> patchApplicationStatus(
            @PathVariable UUID id, 
            @RequestBody java.util.Map<String, String> statusMap,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        com.jobrixa.api.entity.User user = (com.jobrixa.api.entity.User) userDetails;
        String newStatus = statusMap.get("status");
        return ResponseEntity.ok(applicationService.patchApplicationStatus(id, user.getId(), newStatus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        com.jobrixa.api.entity.User user = (com.jobrixa.api.entity.User) userDetails;
        applicationService.deleteApplication(id, user.getId());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/analytics")
    public ResponseEntity<java.util.Map<String, Object>> getAnalytics(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        com.jobrixa.api.entity.User user = (com.jobrixa.api.entity.User) userDetails;
        return ResponseEntity.ok(applicationService.getAnalytics(user.getId()));
    }
    
    @GetMapping("/{id}/events")
    public ResponseEntity<List<com.jobrixa.api.dto.ApplicationEventResponse>> getEvents(
            @PathVariable UUID id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        return ResponseEntity.ok(applicationService.getEventsForApplication(id, userDetails.getUsername()));
    }
}
