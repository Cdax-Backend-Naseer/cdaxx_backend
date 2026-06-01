package com.example.cdaxVideo.Controller;

import com.example.cdaxVideo.Service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/download")
public class DownloadController {

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Check if user can download videos for a course
     * Frontend calls this before enabling download button
     */
    @GetMapping("/check/{userId}/{courseId}")
    public ResponseEntity<Map<String, Object>> checkDownloadPermission(
            @PathVariable Long userId,
            @PathVariable Long courseId) {
        
        boolean canDownload = subscriptionService.isDownloadAllowed(userId, courseId);
        LocalDateTime endDate = subscriptionService.getSubscriptionEndDate(userId, courseId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("canDownload", canDownload);
        response.put("courseId", courseId);
        response.put("userId", userId);
        
        if (canDownload && endDate != null) {
            response.put("subscriptionEndDate", endDate);
            response.put("message", "Download allowed until " + endDate);
        } else {
            response.put("message", "No active subscription. Please purchase the course to download videos.");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get download info including end date for secure storage
     */
    @GetMapping("/info/{userId}/{courseId}")
    public ResponseEntity<Map<String, Object>> getDownloadInfo(
            @PathVariable Long userId,
            @PathVariable Long courseId) {
        
        Map<String, Object> downloadInfo = subscriptionService.getDownloadInfo(userId, courseId);
        return ResponseEntity.ok(downloadInfo);
    }
}