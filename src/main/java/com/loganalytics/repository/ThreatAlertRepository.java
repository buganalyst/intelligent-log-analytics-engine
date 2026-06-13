package com.loganalytics.repository;

import com.loganalytics.model.ThreatAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThreatAlertRepository extends JpaRepository<ThreatAlert, Long> {

    List<ThreatAlert> findAllByOrderByDetectedAtDesc();

    List<ThreatAlert> findByResolvedFalseOrderByDetectedAtDesc();

    List<ThreatAlert> findBySeverityOrderByDetectedAtDesc(String severity);

    Optional<ThreatAlert> findByThreatTypeAndIpAddressAndResolvedFalse(String threatType, String ipAddress);

    long countByResolvedFalse();

    long countBySeverityAndResolvedFalse(String severity);

    @Query("SELECT t.threatType, COUNT(t) FROM ThreatAlert t GROUP BY t.threatType")
    List<Object[]> countByThreatType();

    @Query("SELECT t.severity, COUNT(t) FROM ThreatAlert t GROUP BY t.severity")
    List<Object[]> countBySeverity();

    List<ThreatAlert> findByDetectedAtAfterOrderByDetectedAtDesc(LocalDateTime after);

    @Query("SELECT COUNT(t) FROM ThreatAlert t WHERE t.detectedAt > :since")
    long countSince(@Param("since") LocalDateTime since);
}
