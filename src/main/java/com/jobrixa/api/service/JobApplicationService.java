package com.jobrixa.api.service;

import com.jobrixa.api.dto.JobApplicationRequest;
import com.jobrixa.api.dto.JobApplicationResponse;
import com.jobrixa.api.entity.Company;
import com.jobrixa.api.entity.JobApplication;
import com.jobrixa.api.entity.User;
import com.jobrixa.api.repository.CompanyRepository;
import com.jobrixa.api.repository.JobApplicationRepository;
import com.jobrixa.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationService {
    
    private final JobApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final com.jobrixa.api.repository.ApplicationEventRepository eventRepository;
    private final PlanLimitService planLimitService;

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Company getOrCreateCompany(String name) {
        return companyRepository.findByNameIgnoreCase(name)
            .orElseGet(() -> companyRepository.save(Company.builder().name(name).build()));
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }

    @Transactional
    public JobApplicationResponse createApplication(JobApplicationRequest request) {
        User user = getUser(request.getUserId());
        planLimitService.checkApplicationLimit(user); // enforce FREE plan cap
        Company company = getOrCreateCompany(sanitize(request.getCompanyName()));
        
        JobApplication app = JobApplication.builder()
            .user(user)
            .company(company)
            .companyName(company.getName())
            .jobTitle(sanitize(request.getJobTitle()))
            .jobUrl(sanitize(request.getJobUrl()))
            .location(sanitize(request.getLocation()))
            .isRemote(request.getIsRemote() != null ? request.getIsRemote() : false)
            .status(request.getStatus() != null ? sanitize(request.getStatus()) : "SAVED")
            .source(request.getSource() != null ? sanitize(request.getSource()) : "MANUAL")
            .priority(request.getPriority() != null ? sanitize(request.getPriority()) : "MEDIUM")
            .jobDescription(sanitize(request.getJobDescription()))
            .salaryMin(request.getSalaryMin())
            .salaryMax(request.getSalaryMax())
            .tags(request.getTags() != null && !request.getTags().isEmpty() ? sanitize(String.join(",", request.getTags())) : null)
            .appliedAt(request.getAppliedAt() != null ? request.getAppliedAt() : ("APPLIED".equals(request.getStatus()) ? LocalDate.now() : null))
            .deadline(request.getDeadline())
            .build();
            
        app = applicationRepository.save(app);

        // Increment total applications created counter
        user.setTotalApplicationsCreated(user.getTotalApplicationsCreated() + 1);
        userRepository.save(user);
        
        eventRepository.save(com.jobrixa.api.entity.ApplicationEvent.builder()
            .application(app)
            .eventType("status_change")
            .description("Application added")
            .newValue(app.getStatus())
            .build());
            
        return mapToResponse(app);
    }

    public List<JobApplicationResponse> getActiveApplications(UUID userId) {
        return applicationRepository.findByUserId(userId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public JobApplicationResponse updateApplication(UUID id, UUID userId, JobApplicationRequest request) {
        JobApplication app = applicationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Application not found"));
            
        if (!app.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
            
        app.setJobTitle(sanitize(request.getJobTitle()));
        app.setJobUrl(sanitize(request.getJobUrl()));
        app.setLocation(sanitize(request.getLocation()));
        app.setIsRemote(request.getIsRemote() != null ? request.getIsRemote() : false);
        app.setJobDescription(sanitize(request.getJobDescription()));
        app.setSalaryMin(request.getSalaryMin());
        app.setSalaryMax(request.getSalaryMax());
        app.setTags(request.getTags() != null && !request.getTags().isEmpty() ? sanitize(String.join(",", request.getTags())) : null);
        
        if (!app.getStatus().equals(request.getStatus())) {
            String oldStatus = app.getStatus();
            app.setStatus(request.getStatus());
            if ("APPLIED".equals(request.getStatus()) && app.getAppliedAt() == null && request.getAppliedAt() == null) {
                app.setAppliedAt(LocalDate.now());
            }
            eventRepository.save(com.jobrixa.api.entity.ApplicationEvent.builder()
                .application(app)
                .eventType("status_change")
                .description("Status updated")
                .oldValue(oldStatus)
                .newValue(app.getStatus())
                .build());
        }
        if (request.getAppliedAt() != null) {
            app.setAppliedAt(request.getAppliedAt());
        }
        if (request.getDeadline() != null) {
            app.setDeadline(request.getDeadline());
        }
        
        if (!app.getCompany().getName().equalsIgnoreCase(request.getCompanyName())) {
            Company newCompany = getOrCreateCompany(sanitize(request.getCompanyName()));
            app.setCompany(newCompany);
            app.setCompanyName(newCompany.getName());
        }
        
        return mapToResponse(applicationRepository.save(app));
    }
    
    @Transactional
    public JobApplicationResponse patchApplicationStatus(UUID id, UUID userId, String newStatus) {
        JobApplication app = applicationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Application not found"));
            
        if (!app.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (!app.getStatus().equals(newStatus)) {
            String oldStatus = app.getStatus();
            app.setStatus(newStatus);
            if ("APPLIED".equals(newStatus) && app.getAppliedAt() == null) {
                app.setAppliedAt(LocalDate.now());
            }
            eventRepository.save(com.jobrixa.api.entity.ApplicationEvent.builder()
                .application(app)
                .eventType("status_change")
                .description("Status updated")
                .oldValue(oldStatus)
                .newValue(newStatus)
                .build());
        }
        return mapToResponse(applicationRepository.save(app));
    }
    
    @Transactional
    public void deleteApplication(UUID id, UUID userId) {
        JobApplication app = applicationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Application not found"));
        if (!app.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        applicationRepository.deleteById(id);
    }
    
    public java.util.Map<String, Object> getAnalytics(UUID userId) {
        List<JobApplication> apps = applicationRepository.findByUserId(userId);
        
        int totalApplications = apps.size();
        java.util.Map<String, Long> byStatus = apps.stream()
            .collect(Collectors.groupingBy(JobApplication::getStatus, Collectors.counting()));
            
        long responses = 0;
        long interviews = 0;
        long offers = 0;
        
        for (JobApplication app : apps) {
            String s = app.getStatus().toUpperCase();
            if (!s.equals("SAVED") && !s.equals("APPLIED") && !s.equals("GHOSTED")) {
                responses++;
            }
            if (s.equals("INTERVIEW") || s.equals("OA")) {
                interviews++;
            }
            if (s.equals("OFFER")) {
                offers++;
            }
        }
        
        double responseRate = totalApplications == 0 ? 0 : Math.round(((double) responses / totalApplications * 100) * 10.0) / 10.0;
        double interviewRate = totalApplications == 0 ? 0 : Math.round(((double) interviews / totalApplications * 100) * 10.0) / 10.0;
        double offerRate = totalApplications == 0 ? 0 : Math.round(((double) offers / totalApplications * 100) * 10.0) / 10.0;
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalApplications", totalApplications);
        result.put("byStatus", byStatus);
        
        // Match the required format
        result.put("responseRate", responseRate);
        result.put("interviewRate", interviewRate);
        result.put("offerRate", offerRate);
        
        User user = getUser(userId);
        result.put("totalEverCreated", user.getTotalApplicationsCreated());
        
        return result;
    }
    
    public java.util.Map<String, Object> getMissedAnalytics(UUID userId) {
        long missedCount = applicationRepository.countByUserIdAndMissedTrue(userId);
        long totalRelevant = applicationRepository.countByUserIdAndStatusIn(userId, java.util.Arrays.asList("OA", "INTERVIEW"));
        
        List<JobApplicationResponse> missedApps = applicationRepository.findByUserIdAndMissedTrue(userId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
            
        double missedPercentage = totalRelevant == 0 ? 0 : Math.round(((double) missedCount / totalRelevant * 100) * 10.0) / 10.0;
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("missedCount", missedCount);
        result.put("totalAssessments", totalRelevant);
        result.put("missedPercentage", missedPercentage);
        result.put("missedApplications", missedApps);
        
        return result;
    }
    
    public List<com.jobrixa.api.dto.ApplicationEventResponse> getEventsForApplication(UUID id, String username) {
        JobApplication app = applicationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Application not found"));
        if (!app.getUser().getEmail().equals(username)) { // Assuming username is email
            throw new RuntimeException("Unauthorized");
        }
        return eventRepository.findByApplicationIdOrderByCreatedAtDesc(id)
            .stream()
            .map(e -> com.jobrixa.api.dto.ApplicationEventResponse.builder()
                .id(e.getId())
                .eventType(e.getEventType())
                .description(e.getDescription())
                .oldValue(e.getOldValue())
                .newValue(e.getNewValue())
                .createdAt(e.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }
    
    private JobApplicationResponse mapToResponse(JobApplication app) {
        return JobApplicationResponse.builder()
            .id(app.getId())
            .companyName(app.getCompanyName())
            .jobTitle(app.getJobTitle())
            .jobUrl(app.getJobUrl())
            .location(app.getLocation())
            .isRemote(app.getIsRemote())
            .status(app.getStatus())
            .source(app.getSource())
            .priority(app.getPriority())
            .salaryMin(app.getSalaryMin())
            .salaryMax(app.getSalaryMax())
            .tags(app.getTags() != null ? java.util.Arrays.asList(app.getTags().split(",")) : null)
            .appliedAt(app.getAppliedAt())
            .deadline(app.getDeadline())
            .missed(app.getMissed())
            .missedAt(app.getMissedAt())
            .createdAt(app.getCreatedAt())
            .updatedAt(app.getUpdatedAt())
            .build();
    }
}
