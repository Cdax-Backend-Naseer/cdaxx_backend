package com.example.cdaxVideo.Entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "live_session_attendance", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"live_session_id", "user_id"})
})
public class LiveSessionAttendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_session_id", nullable = false)
    private LiveSession liveSession;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
    
    @Column(name = "left_at")
    private LocalDateTime leftAt;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "attended")
    private Boolean attended = false;
    
    @Column(name = "feedback_rating")
    private Integer feedbackRating;
    
    @Column(name = "feedback_comment", length = 500)
    private String feedbackComment;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (attended == null) attended = false;
    }
    
    public void calculateDuration() {
        if (joinedAt != null && leftAt != null) {
            this.durationMinutes = (int) java.time.Duration.between(joinedAt, leftAt).toMinutes();
        }
    }
}