package com.example.cdaxVideo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
public class UserSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;
    
    @Column(name = "total_months")
    private Integer totalMonths; // Changed from subscriptionType to total months
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public UserSubscription() {}
    
    public UserSubscription(User user, Course course, Integer totalMonths) {
        this.user = user;
        this.course = course;
        this.totalMonths = totalMonths;
        this.startDate = LocalDateTime.now();
        this.expiryDate = calculateExpiryDate(totalMonths);
        this.isActive = true;
    }
    
    private LocalDateTime calculateExpiryDate(Integer months) {
        if (months == null) return LocalDateTime.now().plusMonths(1);
        return LocalDateTime.now().plusMonths(months);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { 
        this.isActive = isActive; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public Integer getTotalMonths() { return totalMonths; }
    public void setTotalMonths(Integer totalMonths) { 
        this.totalMonths = totalMonths; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { 
        this.startDate = startDate; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { 
        this.expiryDate = expiryDate; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isExpired() {
        if (expiryDate == null) return false;
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && !isExpired();
    }
    
    public long getDaysRemaining() {
        if (expiryDate == null || !isValid()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}