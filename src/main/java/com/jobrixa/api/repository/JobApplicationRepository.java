package com.jobrixa.api.repository;

import com.jobrixa.api.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByUserId(UUID userId);
    
    List<JobApplication> findByStatusInAndDeadlineBeforeAndMissedFalse(List<String> statuses, LocalDate deadline);
    
    List<JobApplication> findByUserIdAndMissedTrue(UUID userId);
    
    long countByUserIdAndMissedTrue(UUID userId);
    
    @Query("SELECT COUNT(a) FROM JobApplication a WHERE a.user.id = :userId AND a.status IN :statuses")
    long countByUserIdAndStatusIn(@Param("userId") UUID userId, @Param("statuses") List<String> statuses);

    long countByUserId(UUID userId);
}
