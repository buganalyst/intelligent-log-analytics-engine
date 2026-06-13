package com.loganalytics.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "threat_alerts")
public class ThreatAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String threatType;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String ipAddress;

    private String username;

    @Column(length = 2000, nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @Column(nullable = false)
    private Boolean resolved;

    private Integer occurrenceCount;

    private String affectedEndpoint;

    public ThreatAlert() {
        this.resolved = false;
        this.detectedAt = LocalDateTime.now();
        this.occurrenceCount = 1;
    }

    public ThreatAlert(String threatType, String severity, String ipAddress,
                       String username, String description, String affectedEndpoint) {
        this();
        this.threatType = threatType;
        this.severity = severity;
        this.ipAddress = ipAddress;
        this.username = username;
        this.description = description;
        this.affectedEndpoint = affectedEndpoint;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getThreatType() { return threatType; }
    public void setThreatType(String threatType) { this.threatType = threatType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    public String getAffectedEndpoint() { return affectedEndpoint; }
    public void setAffectedEndpoint(String affectedEndpoint) { this.affectedEndpoint = affectedEndpoint; }
}
