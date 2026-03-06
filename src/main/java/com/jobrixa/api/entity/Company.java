package com.jobrixa.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String domain;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String linkedinUrl;

    @Column(length = 100)
    private String industry;
    
    private Short trustScore;

    private LocalDateTime trustLastChecked;
}
