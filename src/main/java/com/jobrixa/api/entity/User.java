package com.jobrixa.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 200)
    private String college;

    private Short graduationYear;
    private Integer targetCtcMin;
    private Integer targetCtcMax;

    @Column(length = 500)
    private String linkedinUrl;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String plan = "FREE"; // FREE, PRO, CAMPUS

    private LocalDateTime planExpiresAt;
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Builder.Default
    private Boolean isCampusMode = false;

    @Builder.Default
    private Boolean gmailConnected = false;

    @Column(name = "is_early_adopter", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isEarlyAdopter = false;

    @Column(name = "early_adopter_expires_at")
    private LocalDateTime earlyAdopterExpiresAt;

    @Column(name = "gmail_refresh_token", length = 500)
    private String gmailRefreshToken;

    @Column(name = "gmail_last_scanned")
    private LocalDateTime gmailLastScanned;

    @Column(name = "total_applications_created", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer totalApplicationsCreated = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // UserDetails overrides
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
