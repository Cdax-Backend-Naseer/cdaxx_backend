package com.example.cdaxVideo.Service;

import com.example.cdaxVideo.Entity.UserSubscription;
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Entity.Course;
import com.example.cdaxVideo.Repository.UserSubscriptionRepository;
import com.example.cdaxVideo.Repository.UserRepository;
import com.example.cdaxVideo.Repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubscriptionService {

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;

    // ==================== CHECK SUBSCRIPTIONS ====================

    public Boolean hasActiveSubscription(Long userId, Long courseId) {
        return userSubscriptionRepository.hasValidSubscription(userId, courseId, LocalDateTime.now());
    }

    public Boolean hasAnyActiveSubscription(Long userId) {
        List<UserSubscription> activeSubs = userSubscriptionRepository
                .findByUserIdAndIsActive(userId, true);
        
        return activeSubs.stream()
                .anyMatch(UserSubscription::isValid);
    }

    // ==================== GET SUBSCRIPTION DETAILS FOR DOWNLOAD ====================

    public LocalDateTime getSubscriptionEndDate(Long userId, Long courseId) {
        Optional<UserSubscription> subscription = userSubscriptionRepository
                .findValidSubscription(userId, courseId, LocalDateTime.now());
        
        return subscription.map(UserSubscription::getExpiryDate).orElse(null);
    }

    public boolean isDownloadAllowed(Long userId, Long courseId) {
        return hasActiveSubscription(userId, courseId);
    }

    public Map<String, Object> getDownloadInfo(Long userId, Long courseId) {
        Map<String, Object> downloadInfo = new HashMap<>();
        
        Optional<UserSubscription> subscription = userSubscriptionRepository
                .findValidSubscription(userId, courseId, LocalDateTime.now());
        
        if (subscription.isPresent()) {
            UserSubscription sub = subscription.get();
            downloadInfo.put("canDownload", true);
            downloadInfo.put("subscriptionEndDate", sub.getExpiryDate());
            downloadInfo.put("daysRemaining", sub.getDaysRemaining());
            downloadInfo.put("courseId", courseId);
            downloadInfo.put("userId", userId);
        } else {
            downloadInfo.put("canDownload", false);
            downloadInfo.put("message", "No active subscription for this course");
        }
        
        return downloadInfo;
    }

    // ==================== CREATE SUBSCRIPTION ====================

    public Map<String, Object> createSubscription(
            Long userId, 
            Long courseId, 
            Integer totalMonths) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        if (hasActiveSubscription(userId, courseId)) {
            throw new RuntimeException("User already has an active subscription for this course");
        }
        
        UserSubscription subscription = new UserSubscription(user, course, totalMonths);
        subscription = userSubscriptionRepository.save(subscription);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscriptionId", subscription.getId());
        response.put("message", "Subscription created successfully");
        response.put("startDate", subscription.getStartDate());
        response.put("expiryDate", subscription.getExpiryDate());
        response.put("totalMonths", subscription.getTotalMonths());
        response.put("isActive", true);
        
        return response;
    }

    // ==================== CANCEL SUBSCRIPTION ====================
    
    public Map<String, Object> cancelSubscription(Long userId, Long subscriptionId) {
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        if (!subscription.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to cancel this subscription");
        }
        
        subscription.setIsActive(false);
        subscription = userSubscriptionRepository.save(subscription);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscriptionId", subscription.getId());
        response.put("message", "Subscription cancelled successfully");
        response.put("isActive", false);
        response.put("cancelledAt", LocalDateTime.now());
        
        return response;
    }

    // ==================== GET ALL USER SUBSCRIPTIONS ====================
    
    public Map<String, Object> getAllUserSubscriptions(Long userId) {
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserId(userId);
        
        List<Map<String, Object>> subscriptionList = subscriptions.stream()
                .map(sub -> {
                    Map<String, Object> subMap = new HashMap<>();
                    subMap.put("subscriptionId", sub.getId());
                    subMap.put("courseId", sub.getCourse().getId());
                    subMap.put("courseTitle", sub.getCourse().getTitle());
                    subMap.put("totalMonths", sub.getTotalMonths());
                    subMap.put("startDate", sub.getStartDate());
                    subMap.put("expiryDate", sub.getExpiryDate());
                    subMap.put("isActive", sub.getIsActive());
                    subMap.put("isValid", sub.isValid());
                    subMap.put("isExpired", sub.isExpired());
                    subMap.put("daysRemaining", sub.getDaysRemaining());
                    subMap.put("createdAt", sub.getCreatedAt());
                    return subMap;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("totalSubscriptions", subscriptions.size());
        response.put("subscriptions", subscriptionList);
        
        return response;
    }

    // ==================== RENEW SUBSCRIPTION ====================
    
    public Map<String, Object> renewSubscription(Long userId, Long subscriptionId, Integer totalMonths) {
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        if (!subscription.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to renew this subscription");
        }
        
        // Calculate new expiry date
        LocalDateTime newExpiryDate;
        if (subscription.getExpiryDate() != null && subscription.getExpiryDate().isAfter(LocalDateTime.now())) {
            // Extend from current expiry date
            newExpiryDate = subscription.getExpiryDate().plusMonths(totalMonths);
        } else {
            // Start from now
            newExpiryDate = LocalDateTime.now().plusMonths(totalMonths);
        }
        
        // Update subscription
        subscription.setTotalMonths(totalMonths);
        subscription.setIsActive(true);
        subscription.setExpiryDate(newExpiryDate);
        subscription = userSubscriptionRepository.save(subscription);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscriptionId", subscription.getId());
        response.put("message", "Subscription renewed successfully");
        response.put("newExpiryDate", subscription.getExpiryDate());
        response.put("totalMonths", totalMonths);
        response.put("isActive", true);
        
        return response;
    }

    // ==================== GET SUBSCRIPTION HISTORY ====================
    
    public Map<String, Object> getSubscriptionHistory(Long userId, Long courseId) {
        List<UserSubscription> allSubscriptions = userSubscriptionRepository.findByUserId(userId);
        
        List<UserSubscription> courseSubscriptions = allSubscriptions.stream()
                .filter(sub -> sub.getCourse().getId().equals(courseId))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .collect(Collectors.toList());
        
        List<Map<String, Object>> history = courseSubscriptions.stream()
                .map(sub -> {
                    Map<String, Object> historyEntry = new HashMap<>();
                    historyEntry.put("subscriptionId", sub.getId());
                    historyEntry.put("totalMonths", sub.getTotalMonths());
                    historyEntry.put("startDate", sub.getStartDate());
                    historyEntry.put("expiryDate", sub.getExpiryDate());
                    historyEntry.put("isActive", sub.getIsActive());
                    historyEntry.put("status", getSubscriptionStatus(sub));
                    historyEntry.put("createdAt", sub.getCreatedAt());
                    return historyEntry;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("courseId", courseId);
        response.put("totalHistoryEntries", history.size());
        response.put("history", history);
        
        return response;
    }

    // ==================== VALIDATE SUBSCRIPTION ====================
    
    public Map<String, Object> validateSubscription(Long userId, Long subscriptionId) {
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        if (!subscription.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to validate this subscription");
        }
        
        boolean isValid = subscription.isValid();
        boolean isExpired = subscription.isExpired();
        long daysRemaining = subscription.getDaysRemaining();
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscriptionId", subscriptionId);
        response.put("isValid", isValid);
        response.put("isExpired", isExpired);
        response.put("isActive", subscription.getIsActive());
        response.put("daysRemaining", daysRemaining);
        response.put("expiryDate", subscription.getExpiryDate());
        response.put("status", getSubscriptionStatus(subscription));
        
        return response;
    }

    // ==================== GET SUBSCRIPTION DETAILS ====================

    public Map<String, Object> getSubscriptionDetails(Long userId) {
        Map<String, Object> details = new HashMap<>();
        
        List<UserSubscription> subscriptions = userSubscriptionRepository
                .findByUserId(userId);
        
        List<Map<String, Object>> validSubscriptions = new ArrayList<>();
        List<Map<String, Object>> expiredSubscriptions = new ArrayList<>();
        
        for (UserSubscription sub : subscriptions) {
            Map<String, Object> subDetails = new HashMap<>();
            subDetails.put("subscriptionId", sub.getId());
            subDetails.put("courseId", sub.getCourse().getId());
            subDetails.put("courseTitle", sub.getCourse().getTitle());
            subDetails.put("totalMonths", sub.getTotalMonths());
            subDetails.put("startDate", sub.getStartDate());
            subDetails.put("expiryDate", sub.getExpiryDate());
            subDetails.put("isActive", sub.getIsActive());
            subDetails.put("isValid", sub.isValid());
            subDetails.put("isExpired", sub.isExpired());
            subDetails.put("daysRemaining", sub.getDaysRemaining());
            
            if (sub.isValid()) {
                validSubscriptions.add(subDetails);
            } else if (Boolean.TRUE.equals(sub.getIsActive()) && sub.isExpired()) {
                expiredSubscriptions.add(subDetails);
            }
        }
        
        details.put("userId", userId);
        details.put("hasActiveSubscription", !validSubscriptions.isEmpty());
        details.put("activeSubscriptionCount", validSubscriptions.size());
        details.put("validSubscriptions", validSubscriptions);
        details.put("expiredSubscriptions", expiredSubscriptions);
        details.put("totalSubscriptions", subscriptions.size());
        
        return details;
    }

    // ==================== CLEANUP EXPIRED SUBSCRIPTIONS ====================

    @Transactional
    public int cleanupExpiredSubscriptions() {
        List<UserSubscription> expiredSubs = userSubscriptionRepository
                .findExpiredActiveSubscriptions(LocalDateTime.now());
        
        for (UserSubscription sub : expiredSubs) {
            sub.setIsActive(false);
        }
        
        userSubscriptionRepository.saveAll(expiredSubs);
        return expiredSubs.size();
    }

    // ==================== HELPER METHODS ====================
    
    private String getSubscriptionStatus(UserSubscription subscription) {
        if (!Boolean.TRUE.equals(subscription.getIsActive())) {
            return "CANCELLED";
        }
        
        if (subscription.isExpired()) {
            return "EXPIRED";
        }
        
        if (subscription.isValid()) {
            long daysRemaining = subscription.getDaysRemaining();
            if (daysRemaining <= 7) {
                return "EXPIRING_SOON";
            }
            return "ACTIVE";
        }
        
        return "INACTIVE";
    }
}