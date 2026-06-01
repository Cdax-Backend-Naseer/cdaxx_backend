package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    
    Optional<UserSubscription> findActiveByUserIdAndCourseId(Long userId, Long courseId);
    
    List<UserSubscription> findByUserIdAndIsActive(Long userId, Boolean isActive);
    
    List<UserSubscription> findByUserId(Long userId);
    
    @Query("SELECT s FROM UserSubscription s WHERE s.user.id = :userId AND s.course.id = :courseId AND s.isActive = true AND s.expiryDate > :now")
    Optional<UserSubscription> findValidSubscription(@Param("userId") Long userId, @Param("courseId") Long courseId, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM UserSubscription s WHERE s.expiryDate < :now AND s.isActive = true")
    List<UserSubscription> findExpiredActiveSubscriptions(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(s) > 0 FROM UserSubscription s WHERE s.user.id = :userId AND s.course.id = :courseId AND s.isActive = true AND s.expiryDate > :now")
    boolean hasValidSubscription(@Param("userId") Long userId, @Param("courseId") Long courseId, @Param("now") LocalDateTime now);
}