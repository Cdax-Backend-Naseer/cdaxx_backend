package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.LiveSessionAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiveSessionAttendanceRepository extends JpaRepository<LiveSessionAttendance, Long> {
    
    Optional<LiveSessionAttendance> findByLiveSessionIdAndUserId(Long sessionId, Long userId);
    
    boolean existsByLiveSessionIdAndUserId(Long sessionId, Long userId);
    
    List<LiveSessionAttendance> findByLiveSessionId(Long sessionId);
    
    List<LiveSessionAttendance> findByUserId(Long userId);
    
    @Query("SELECT COUNT(a) FROM LiveSessionAttendance a WHERE a.liveSession.id = :sessionId AND a.attended = true")
    Integer countAttendedAttendees(@Param("sessionId") Long sessionId);
    
    @Query("SELECT AVG(a.feedbackRating) FROM LiveSessionAttendance a WHERE a.liveSession.id = :sessionId AND a.feedbackRating IS NOT NULL")
    Double getAverageRatingForSession(@Param("sessionId") Long sessionId);
}