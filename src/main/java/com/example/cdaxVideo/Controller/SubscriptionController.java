package com.example.cdaxVideo.Controller;

import com.example.cdaxVideo.Service.AuthService;
import com.example.cdaxVideo.Service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private SubscriptionService subscriptionService;

    // ==================== GET SUBSCRIPTION STATUS ====================
    
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            Map<String, Object> subscriptionDetails = subscriptionService.getSubscriptionDetails(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subscription", subscriptionDetails);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== CHECK COURSE SUBSCRIPTION ====================
    
    @GetMapping("/check/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> checkCourseSubscription(@PathVariable Long courseId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            boolean hasSubscription = subscriptionService.hasActiveSubscription(userId, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasSubscription", hasSubscription);
            response.put("courseId", courseId);
            response.put("userId", userId);
            
            // Also return end date if subscription exists
            if (hasSubscription) {
                java.time.LocalDateTime endDate = subscriptionService.getSubscriptionEndDate(userId, courseId);
                response.put("subscriptionEndDate", endDate);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== CREATE SUBSCRIPTION ====================
    
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createSubscription(
            @RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            Long courseId = Long.valueOf(request.get("courseId").toString());
            Integer totalMonths = (Integer) request.get("totalMonths");
            
            // Validate totalMonths
            if (totalMonths == null || totalMonths <= 0) {
                totalMonths = 1; // Default to 1 month
            }
            
            Map<String, Object> result = subscriptionService.createSubscription(userId, courseId, totalMonths);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription created successfully");
            response.put("subscription", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== CANCEL SUBSCRIPTION ====================
    
    @PostMapping("/cancel/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> cancelSubscription(@PathVariable Long subscriptionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            Map<String, Object> result = subscriptionService.cancelSubscription(userId, subscriptionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription cancelled successfully");
            response.put("subscription", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== GET ALL USER SUBSCRIPTIONS ====================
    
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAllSubscriptions() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            Map<String, Object> result = subscriptionService.getAllUserSubscriptions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subscriptions", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== RENEW SUBSCRIPTION ====================
    
    @PostMapping("/renew/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> renewSubscription(
            @PathVariable Long subscriptionId,
            @RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            Integer totalMonths = (Integer) request.get("totalMonths");
            
            if (totalMonths == null || totalMonths <= 0) {
                totalMonths = 1;
            }
            
            Map<String, Object> result = subscriptionService.renewSubscription(userId, subscriptionId, totalMonths);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription renewed successfully");
            response.put("subscription", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== GET SUBSCRIPTION HISTORY ====================
    
    @GetMapping("/history/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSubscriptionHistory(@PathVariable Long courseId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            Map<String, Object> result = subscriptionService.getSubscriptionHistory(userId, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("history", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== CHECK SUBSCRIPTION VALIDITY ====================
    
    @GetMapping("/validate/{subscriptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> validateSubscription(@PathVariable Long subscriptionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            Map<String, Object> result = subscriptionService.validateSubscription(userId, subscriptionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("validation", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== DOWNLOAD INFO ====================
    
    @GetMapping("/download-info/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDownloadInfo(@PathVariable Long courseId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            Map<String, Object> downloadInfo = subscriptionService.getDownloadInfo(userId, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("downloadInfo", downloadInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @GetMapping("/can-download/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> canDownload(@PathVariable Long courseId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = authService.getUserByEmail(email).getId();
            boolean canDownload = subscriptionService.isDownloadAllowed(userId, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("canDownload", canDownload);
            response.put("courseId", courseId);
            
            if (canDownload) {
                java.time.LocalDateTime endDate = subscriptionService.getSubscriptionEndDate(userId, courseId);
                response.put("subscriptionEndDate", endDate);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}