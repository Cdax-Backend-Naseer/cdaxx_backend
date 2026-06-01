package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.LiveSession;
import com.example.cdaxVideo.Entity.LiveSessionStatus;
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

@Repository
public interface LiveSessionRepository extends JpaRepository<LiveSession, Long> {
    
    List<LiveSession> findByStatus(LiveSessionStatus status);
    Page<LiveSession> findByStatus(LiveSessionStatus status, Pageable pageable);
    
    @Query("SELECT l FROM LiveSession l WHERE l.status = 'SCHEDULED' AND l.scheduledStartTime > :now ORDER BY l.scheduledStartTime ASC")
    List<LiveSession> findUpcomingSessions(@Param("now") LocalDateTime now);
    
    @Query("SELECT l FROM LiveSession l WHERE l.status = 'LIVE' ORDER BY l.scheduledStartTime DESC")
    List<LiveSession> findCurrentLiveSessions();
    
    @Query("SELECT l FROM LiveSession l WHERE l.status = 'SCHEDULED' AND l.scheduledStartTime <= :now AND l.scheduledEndTime >= :now")
    List<LiveSession> findSessionsThatShouldBeLive(@Param("now") LocalDateTime now);
    
    @Query("SELECT l FROM LiveSession l WHERE l.status = 'LIVE' AND l.scheduledEndTime <= :now")
    List<LiveSession> findSessionsThatShouldBeEnded(@Param("now") LocalDateTime now);
    
    List<LiveSession> findByCourseId(Long courseId);
    
    @Modifying
    @Transactional
    @Query("UPDATE LiveSession l SET l.status = :newStatus WHERE l.id = :sessionId")
    int updateSessionStatus(@Param("sessionId") Long sessionId, 
                           @Param("newStatus") LiveSessionStatus newStatus);
    
    @Modifying
    @Transactional
    @Query("UPDATE LiveSession l SET l.currentAttendees = l.currentAttendees + 1 WHERE l.id = :sessionId AND l.currentAttendees < l.maxAttendees")
    int incrementAttendees(@Param("sessionId") Long sessionId);
    
    @Modifying
    @Transactional
    @Query("UPDATE LiveSession l SET l.currentAttendees = l.currentAttendees - 1 WHERE l.id = :sessionId AND l.currentAttendees > 0")
    int decrementAttendees(@Param("sessionId") Long sessionId);
    
    @Query("SELECT l FROM LiveSession l WHERE l.status = 'SCHEDULED' AND l.scheduledStartTime BETWEEN :start AND :end AND l.reminderSent = false")
    List<LiveSession> findSessionsNeedingReminder(@Param("start") LocalDateTime start, 
                                                  @Param("end") LocalDateTime end);
}