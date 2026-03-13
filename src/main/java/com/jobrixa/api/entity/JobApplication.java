package com.jobrixa.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String jobTitle;

    @Column(length = 1000)
    private String jobUrl;

    @Column(length = 50)
    private String source;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(length = 20)
    private String priority;

    private LocalDate appliedAt;
    
    private LocalDate deadline;

    @Column(nullable = false)
    @Builder.Default
    private Boolean missed = false;

    private LocalDateTime missedAt;
    
    private LocalDate nextActionDate;

    private Integer salaryMin;
    private Integer salaryMax;

    @Column(length = 200)
    private String location;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRemote = false;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    private Short trustScore;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasBondWarning = false;

    @Column(columnDefinition = "TEXT")
    private String bondDetails;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasPaymentWarning = false;

    @Column(columnDefinition = "TEXT")
    private String tags; // comma-separated: "DSA,Java,System Design"

    @Column(columnDefinition = "TEXT")
    private String notes; // free-text notes and auto-detection log

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
