package com.jobrixa.api.service;

import com.jobrixa.api.entity.User;
import com.jobrixa.api.repository.JobApplicationRepository;
import com.jobrixa.api.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMigrationService {

    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;

    @PostConstruct
    @Transactional
    public void backfillApplicationCounts() {
        log.info("Starting backfill for totalApplicationsCreated field...");
        List<User> users = userRepository.findAll();
        int updatedCount = 0;
        for (User user : users) {
            if (user.getTotalApplicationsCreated() == null || user.getTotalApplicationsCreated() == 0) {
                int count = (int) applicationRepository.countByUserId(user.getId());
                user.setTotalApplicationsCreated(count);
                userRepository.save(user);
                updatedCount++;
            }
        }
        log.info("Backfill completed. Updated {} users.", updatedCount);
    }
}
