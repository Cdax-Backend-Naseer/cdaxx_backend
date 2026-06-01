package com.example.cdaxVideo.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "live_sessions", indexes = {
    @Index(name = "idx_status_start_time", columnList = "status, scheduled_start_time"),
    @Index(name = "idx_course_id", columnList = "course_id"),
    @Index(name = "idx_scheduled_start", columnList = "scheduled_start_time")
})
public class LiveSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "scheduled_start_time", nullable = false)
    private LocalDateTime scheduledStartTime;
    
    @Column(name = "scheduled_end_time", nullable = false)
    private LocalDateTime scheduledEndTime;
    
    @Column(name = "meeting_id", length = 100)
    private String meetingId;
    
    @Column(name = "meeting_password", length = 100)
    private String meetingPassword;
    
    @Column(name = "join_url", length = 500)
    private String joinUrl;
    
    @Column(name = "stream_url", length = 500)
    private String streamUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LiveSessionStatus status = LiveSessionStatus.SCHEDULED;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonBackReference
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @JsonBackReference
    private Module module;
    
    @OneToMany(mappedBy = "liveSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Set<LiveSessionAttendance> attendees = new HashSet<>();
    
    @Column(name = "max_attendees")
    private Integer maxAttendees = 100;
    
    @Column(name = "current_attendees")
    private Integer currentAttendees = 0;
    
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;
    
    @Column(name = "chat_enabled")
    private Boolean chatEnabled = true;
    
    @Column(name = "record_session")
    private Boolean recordSession = false;
    
    @Column(name = "host_user_id")
    private Long hostUserId;
    
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Integer version;
    
    @PrePersist
    protected void onCreate() {
        if (status == null) status = LiveSessionStatus.SCHEDULED;
        if (maxAttendees == null) maxAttendees = 100;
        if (currentAttendees == null) currentAttendees = 0;
        if (chatEnabled == null) chatEnabled = true;
        if (recordSession == null) recordSession = false;
        if (reminderSent == null) reminderSent = false;
    }
    
    public boolean isFull() {
        return currentAttendees >= maxAttendees;
    }
    
    public boolean isLive() {
        return status == LiveSessionStatus.LIVE;
    }
    
    public boolean isUpcoming() {
        return status == LiveSessionStatus.SCHEDULED && 
               scheduledStartTime.isAfter(LocalDateTime.now());
    }
    
    public boolean isOngoing() {
        LocalDateTime now = LocalDateTime.now();
        return status == LiveSessionStatus.LIVE && 
               now.isAfter(scheduledStartTime) && 
               now.isBefore(scheduledEndTime);
    }
}