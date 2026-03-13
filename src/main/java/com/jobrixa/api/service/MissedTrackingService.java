package com.jobrixa.api.service;

import com.jobrixa.api.entity.JobApplication;
import com.jobrixa.api.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissedTrackingService {

    private final JobApplicationRepository repository;

    /**
     * Runs daily at midnight to identify missed deadlines.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void trackMissedDeadlines() {
        log.info("Starting scheduled task: trackMissedDeadlines");
        
        List<String> trackableStatuses = Arrays.asList("OA", "INTERVIEW");
        LocalDate today = LocalDate.now();
        
        List<JobApplication> potentialMissed = repository.findByStatusInAndDeadlineBeforeAndMissedFalse(
                trackableStatuses, today);
        
        if (potentialMissed.isEmpty()) {
            log.info("No new missed deadlines identified.");
            return;
        }

        for (JobApplication app : potentialMissed) {
            app.setMissed(true);
            app.setMissedAt(LocalDateTime.now());
            log.info("Marking application {} as missed (Deadline was {})", app.getId(), app.getDeadline());
        }
        
        repository.saveAll(potentialMissed);
        log.info("Finished marking {} applications as missed.", potentialMissed.size());
    }
}
