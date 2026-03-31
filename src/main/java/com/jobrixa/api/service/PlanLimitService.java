package com.jobrixa.api.service;

import com.jobrixa.api.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlanLimitService {

    private static final int FREE_APPLICATION_LIMIT = 30;

    /**
     * Returns true if the user is under the limit (can create a new application).
     * FREE plan: max FREE_APPLICATION_LIMIT applications.
     * PRO / CAMPUS (and not expired): unlimited.
     */
    public void checkApplicationLimit(User user) {
        // TEMPORARILY DISABLED FOR BETA LAUNCH
        // if (isPlanActive(user)) {
        //     return; // unlimited
        // }
        // int count = user.getTotalApplicationsCreated() != null ? user.getTotalApplicationsCreated() : 0;
        // if (count >= FREE_APPLICATION_LIMIT) {
        //     throw new RuntimeException(
        //         "Free plan limit reached (" + FREE_APPLICATION_LIMIT + " applications). " +
        //         "Upgrade to Pro for unlimited tracking."
        //     );
        // }
        return; // All users have unlimited access during beta
    }

    public boolean isPlanActive(User user) {
        if ("FREE".equals(user.getPlan())) return false;
        if (user.getPlanExpiresAt() == null) return false;
        return user.getPlanExpiresAt().isAfter(LocalDateTime.now());
    }
}
