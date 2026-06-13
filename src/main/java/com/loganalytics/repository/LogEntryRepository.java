package com.loganalytics.repository;

import com.loganalytics.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    List<LogEntry> findByIpAddressAndActionAndTimestampAfter(String ipAddress, String action, LocalDateTime after);

    List<LogEntry> findByUsernameAndActionAndTimestampAfter(String username, String action, LocalDateTime after);

    List<LogEntry> findByIpAddressAndTimestampAfter(String ipAddress, LocalDateTime after);

    List<LogEntry> findByLogTypeOrderByTimestampDesc(String logType);

    List<LogEntry> findAllByOrderByTimestampDesc();

    List<LogEntry> findTop100ByOrderByTimestampDesc();

    @Query("SELECT l.logType, COUNT(l) FROM LogEntry l GROUP BY l.logType")
    List<Object[]> countByLogType();

    @Query("SELECT l.status, COUNT(l) FROM LogEntry l GROUP BY l.status")
    List<Object[]> countByStatus();

    @Query("SELECT l.ipAddress, COUNT(l) as cnt FROM LogEntry l WHERE l.timestamp > :since GROUP BY l.ipAddress ORDER BY cnt DESC")
    List<Object[]> topIpAddressesSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(l) FROM LogEntry l WHERE l.timestamp > :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT l FROM LogEntry l WHERE l.details LIKE %:pattern% ORDER BY l.timestamp DESC")
    List<LogEntry> findByDetailsContaining(@Param("pattern") String pattern);

    long countByLogTypeAndTimestampAfter(String logType, LocalDateTime after);

    long countByStatusAndTimestampAfter(String status, LocalDateTime after);
}
