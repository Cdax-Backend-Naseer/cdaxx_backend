package com.example.cdaxVideo.Entity;

public enum LiveSessionStatus {
    SCHEDULED("Scheduled", "Session is scheduled for future date"),
    LIVE("Live", "Session is currently active"),
    ENDED("Ended", "Session has ended"),
    CANCELLED("Cancelled", "Session has been cancelled");
    
    private final String displayName;
    private final String description;
    
    LiveSessionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == LIVE;
    }
    
    public boolean canJoin() {
        return this == LIVE;
    }
    
    public static LiveSessionStatus fromString(String status) {
        for (LiveSessionStatus s : LiveSessionStatus.values()) {
            if (s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + status);
    }
}