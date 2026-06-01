package com.example.cdaxVideo.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LiveSessionDTO {
    private Long id;
    private String title;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEndTime;
    
    private String meetingId;
    private String joinUrl;
    private String streamUrl;
    private String status;
    private Long courseId;
    private String courseName;
    private Long moduleId;
    private String moduleName;
    private Integer maxAttendees;
    private Integer currentAttendees;
    private Boolean chatEnabled;
    private String recordingUrl;
    
    private Long timeUntilStart;
    private Long timeUntilEnd;
    private Boolean canJoin = false;
    private Boolean isSubscribed = false;
    private Boolean isHost = false;
    private Integer availableSeats;
    
    public void calculateTimeUntilStart() {
        if (scheduledStartTime != null) {
            long diff = java.time.Duration.between(LocalDateTime.now(), scheduledStartTime).toMillis();
            this.timeUntilStart = Math.max(0, diff);
        }
    }
    
    public void calculateTimeUntilEnd() {
        if (scheduledEndTime != null && scheduledEndTime.isAfter(LocalDateTime.now())) {
            this.timeUntilEnd = java.time.Duration.between(LocalDateTime.now(), scheduledEndTime).toMillis();
        } else {
            this.timeUntilEnd = 0L;
        }
    }
    
    public void calculateAvailableSeats() {
        if (maxAttendees != null && currentAttendees != null) {
            this.availableSeats = maxAttendees - currentAttendees;
        }
    }
}