package com.example.cdaxVideo.DTO;

import lombok.Data;
import java.util.List;

@Data
public class LiveSessionResponseDTO {
    private List<LiveSessionDTO> upcomingSessions;
    private List<LiveSessionDTO> liveSessions;
    private List<LiveSessionDTO> pastSessions;
    private int totalCount;
    private boolean hasActiveSubscription;
}