package com.jobrixa.api.service;

import com.jobrixa.api.entity.User;
import com.jobrixa.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlanLimitService {

    private final UserRepository userRepository;

    private static final int FREE_APPLICATION_LIMIT = 30;

    /**
     * Returns true if the user is under the limit (can create a new application).
     * FREE plan: max FREE_APPLICATION_LIMIT applications.
     * PRO / CAMPUS (and not expired): unlimited.
     */
    public void checkApplicationLimit(User user) {
        if (isPlanActive(user)) {
            return; // unlimited
        }
        
        if (isBetaEligible(user)) {
            return; // soft override beta access
        }

        int count = user.getTotalApplicationsCreated() != null ? user.getTotalApplicationsCreated() : 0;
        if (count >= FREE_APPLICATION_LIMIT) {
            throw new RuntimeException(
                "Free plan limit reached (" + FREE_APPLICATION_LIMIT + " applications). " +
                "Upgrade to Pro for unlimited tracking."
            );
        }
    }

    public boolean isPlanActive(User user) {
        if ("FREE".equals(user.getPlan())) return false;
        if (user.getPlanExpiresAt() == null) return false;
        return user.getPlanExpiresAt().isAfter(LocalDateTime.now());
    }

    public boolean isBetaEligible(User user) {
        if (user.getCreatedAt() == null) return false;
        if (user.getCreatedAt().isBefore(LocalDateTime.now().minusDays(90))) return false;
        long rank = userRepository.countByCreatedAtBefore(user.getCreatedAt());
        return rank < 1000;
    }

    public long getBetaDaysLeft(User user) {
        if (!isBetaEligible(user)) return 0;
        return Math.max(0, java.time.Duration.between(LocalDateTime.now(), user.getCreatedAt().plusDays(90)).toDays());
    }
}
