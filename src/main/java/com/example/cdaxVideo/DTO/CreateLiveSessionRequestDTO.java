package com.example.cdaxVideo.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateLiveSessionRequestDTO {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;
    
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;
    
    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;
    
    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEndTime;
    
    private Long courseId;
    private Long moduleId;
    
    @Min(value = 1, message = "Max attendees must be at least 1")
    @Max(value = 1000, message = "Max attendees cannot exceed 1000")
    private Integer maxAttendees = 100;
    
    private Boolean chatEnabled = true;
    private Boolean recordSession = false;
    
    @AssertTrue(message = "End time must be after start time")
    private boolean isEndTimeAfterStartTime() {
        return scheduledEndTime != null && scheduledStartTime != null && 
               scheduledEndTime.isAfter(scheduledStartTime);
    }
}