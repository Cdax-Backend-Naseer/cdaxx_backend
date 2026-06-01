// package com.example.cdaxVideo.Service;

// import com.example.cdaxVideo.DTO.CreateLiveSessionRequestDTO;
// import com.example.cdaxVideo.DTO.LiveSessionDTO;
// import com.example.cdaxVideo.Entity.*;
// import com.example.cdaxVideo.Repository.*;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import jakarta.persistence.EntityNotFoundException;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.UUID;
// import java.util.stream.Collectors;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class LiveSessionService {
    
//     private final LiveSessionRepository liveSessionRepository;
//     private final LiveSessionAttendanceRepository attendanceRepository;
//     private final UserSubscriptionRepository userSubscriptionRepository;
//     private final UserRepository userRepository;
//     private final CourseRepository courseRepository;
//     private final ModuleRepository moduleRepository;
    
//     /**
//      * Create a new live session
//      */
//     @Transactional
//     public LiveSession createLiveSession(CreateLiveSessionRequestDTO request, Long hostUserId) {
//         log.info("Creating live session: {}", request.getTitle());
        
//         LiveSession session = new LiveSession();
//         session.setTitle(request.getTitle());
//         session.setDescription(request.getDescription());
//         session.setScheduledStartTime(request.getScheduledStartTime());
//         session.setScheduledEndTime(request.getScheduledEndTime());
//         session.setMaxAttendees(request.getMaxAttendees());
//         session.setChatEnabled(request.getChatEnabled());
//         session.setRecordSession(request.getRecordSession());
//         session.setHostUserId(hostUserId);
        
//         // Generate meeting details
//         session.setMeetingId(generateMeetingId());
//         session.setJoinUrl(generateJoinUrl(session.getMeetingId()));
        
//         if (request.getCourseId() != null) {
//             Course course = courseRepository.findById(request.getCourseId())
//                 .orElseThrow(() -> new EntityNotFoundException("Course not found"));
//             session.setCourse(course);
//         }
        
//         if (request.getModuleId() != null) {
//             com.example.cdaxVideo.Entity.Module module = moduleRepository.findById(request.getModuleId())
//                 .orElseThrow(() -> new EntityNotFoundException("Module not found"));
//             session.setModule(module);
//         }
        
//         return liveSessionRepository.save(session);
//     }
    
//     /**
//      * Check if user has active subscription for a course
//      * Uses your existing UserSubscription entity structure
//      */
//     private boolean checkUserSubscription(User user, Long courseId) {
//         if (courseId == null) {
//             // If no specific course, check if user has ANY active subscription
//             List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserIdAndIsActive(user.getId(), true);
//             return !subscriptions.isEmpty();
//         }
        
//         // Check if user has active subscription for this specific course
//         return userSubscriptionRepository
//             .findActiveByUserIdAndCourseId(user.getId(), courseId)
//             .isPresent();
//     }
    
//     /**
//      * Get live session with access control
//      */
//     @Transactional(readOnly = true)
//     public LiveSessionDTO getLiveSessionWithAccess(Long sessionId, User user) {
//         LiveSession session = liveSessionRepository.findById(sessionId)
//             .orElseThrow(() -> new EntityNotFoundException("Live session not found"));
        
//         LiveSessionDTO dto = convertToDTO(session);
        
//         Long courseId = session.getCourse() != null ? session.getCourse().getId() : null;
//         boolean isSubscribed = checkUserSubscription(user, courseId);
//         dto.setIsSubscribed(isSubscribed);
        
//         // Check if user is host (admin/instructor can join even without subscription)
//         boolean isHost = session.getHostUserId() != null && session.getHostUserId().equals(user.getId());
//         boolean isAdmin = user.isAdmin();
//         boolean isInstructor = user.isInstructor();
        
//         dto.setIsHost(isHost || isAdmin || isInstructor);
        
//         // User can join if: (subscribed OR host/admin/instructor) AND session is live AND not full
//         if ((isSubscribed || isHost || isAdmin || isInstructor) && 
//             session.getStatus() == LiveSessionStatus.LIVE && 
//             !session.isFull()) {
//             dto.setCanJoin(true);
//         }
        
//         dto.calculateTimeUntilStart();
//         dto.calculateTimeUntilEnd();
//         dto.calculateAvailableSeats();
        
//         return dto;
//     }
    
//     /**
//      * Join a live session
//      */
//     @Transactional
//     public JoinSessionResult joinLiveSession(Long sessionId, User user) {
//         log.info("User {} attempting to join session {}", user.getId(), sessionId);
        
//         LiveSession session = liveSessionRepository.findById(sessionId)
//             .orElseThrow(() -> new EntityNotFoundException("Live session not found"));
        
//         Long courseId = session.getCourse() != null ? session.getCourse().getId() : null;
        
//         // Check if user is host/admin/instructor (they can join without subscription)
//         boolean isHost = session.getHostUserId() != null && session.getHostUserId().equals(user.getId());
//         boolean isAdmin = user.isAdmin();
//         boolean isInstructor = user.isInstructor();
        
//         // Check subscription (skip for host/admin/instructor)
//         boolean hasAccess = checkUserSubscription(user, courseId) || isHost || isAdmin || isInstructor;
        
//         if (!hasAccess) {
//             log.warn("User {} not subscribed, cannot join session {}", user.getId(), sessionId);
//             return JoinSessionResult.NOT_SUBSCRIBED;
//         }
        
//         // Check if session is live
//         if (session.getStatus() != LiveSessionStatus.LIVE) {
//             log.warn("Session {} is not live (status: {})", sessionId, session.getStatus());
//             return JoinSessionResult.NOT_LIVE;
//         }
        
//         // Check capacity
//         if (session.isFull()) {
//             log.warn("Session {} is full", sessionId);
//             return JoinSessionResult.SESSION_FULL;
//         }
        
//         // Check if already joined
//         if (attendanceRepository.existsByLiveSessionIdAndUserId(sessionId, user.getId())) {
//             log.info("User {} already joined session {}", user.getId(), sessionId);
//             return JoinSessionResult.ALREADY_JOINED;
//         }
        
//         // Record attendance
//         LiveSessionAttendance attendance = new LiveSessionAttendance();
//         attendance.setLiveSession(session);
//         attendance.setUser(user);
//         attendance.setJoinedAt(LocalDateTime.now());
//         attendanceRepository.save(attendance);
        
//         // Increment attendees count
//         liveSessionRepository.incrementAttendees(sessionId);
//         session = liveSessionRepository.findById(sessionId).get();
        
//         return new JoinSessionResult(true, session.getJoinUrl(), session.getMeetingId());
//     }
    
//     /**
//      * Leave a live session
//      */
//     @Transactional
//     public void leaveLiveSession(Long sessionId, User user) {
//         log.info("User {} leaving session {}", user.getId(), sessionId);
        
//         LiveSessionAttendance attendance = attendanceRepository
//             .findByLiveSessionIdAndUserId(sessionId, user.getId())
//             .orElseThrow(() -> new EntityNotFoundException("Attendance record not found"));
        
//         attendance.setLeftAt(LocalDateTime.now());
//         attendance.calculateDuration();
//         attendance.setAttended(true);
//         attendanceRepository.save(attendance);
        
//         liveSessionRepository.decrementAttendees(sessionId);
//     }
    
//     /**
//      * Get upcoming sessions for user
//      */
//     @Transactional(readOnly = true)
//     public List<LiveSessionDTO> getUpcomingSessions(User user) {
//         List<LiveSession> sessions = liveSessionRepository.findUpcomingSessions(LocalDateTime.now());
        
//         boolean isAdmin = user.isAdmin();
//         boolean isInstructor = user.isInstructor();
        
//         return sessions.stream()
//             .map(session -> {
//                 LiveSessionDTO dto = convertToDTO(session);
//                 Long courseId = session.getCourse() != null ? session.getCourse().getId() : null;
//                 boolean isSubscribed = checkUserSubscription(user, courseId);
//                 boolean isHost = session.getHostUserId() != null && session.getHostUserId().equals(user.getId());
                
//                 dto.setIsSubscribed(isSubscribed || isHost || isAdmin || isInstructor);
//                 dto.calculateTimeUntilStart();
//                 dto.calculateAvailableSeats();
//                 return dto;
//             })
//             .collect(Collectors.toList());
//     }
    
//     /**
//      * Get current live sessions (no auth needed)
//      */
//     @Transactional(readOnly = true)
//     public List<LiveSessionDTO> getCurrentLiveSessions() {
//         return liveSessionRepository.findCurrentLiveSessions().stream()
//             .map(this::convertToDTO)
//             .collect(Collectors.toList());
//     }
    
//     /**
//      * Get upcoming sessions for a specific course
//      */
//     @Transactional(readOnly = true)
//     public List<LiveSessionDTO> getUpcomingSessionsForCourse(Long courseId, User user) {
//         List<LiveSession> sessions = liveSessionRepository.findByCourseId(courseId)
//             .stream()
//             .filter(session -> session.getStatus() == LiveSessionStatus.SCHEDULED)
//             .collect(Collectors.toList());
        
//         boolean isSubscribed = checkUserSubscription(user, courseId);
//         boolean isAdmin = user.isAdmin();
//         boolean isInstructor = user.isInstructor();
        
//         boolean hasAccess = isSubscribed || isAdmin || isInstructor;
        
//         return sessions.stream()
//             .map(session -> {
//                 LiveSessionDTO dto = convertToDTO(session);
//                 dto.setIsSubscribed(hasAccess);
//                 dto.calculateTimeUntilStart();
//                 return dto;
//             })
//             .collect(Collectors.toList());
//     }
    
//     /**
//      * Get user's live session history (attended sessions)
//      */
//     @Transactional(readOnly = true)
//     public List<LiveSessionDTO> getUserLiveSessionHistory(User user) {
//         List<LiveSessionAttendance> attendances = attendanceRepository.findByUserId(user.getId());
        
//         return attendances.stream()
//             .map(attendance -> {
//                 LiveSessionDTO dto = convertToDTO(attendance.getLiveSession());
//                 dto.setIsSubscribed(true);
//                 return dto;
//             })
//             .collect(Collectors.toList());
//     }
    
//     /**
//      * Add feedback for a session
//      */
//     @Transactional
//     public void addFeedback(Long sessionId, User user, Integer rating, String comment) {
//         LiveSessionAttendance attendance = attendanceRepository
//             .findByLiveSessionIdAndUserId(sessionId, user.getId())
//             .orElseThrow(() -> new EntityNotFoundException("Attendance record not found"));
        
//         if (rating != null && (rating < 1 || rating > 5)) {
//             throw new IllegalArgumentException("Rating must be between 1 and 5");
//         }
        
//         attendance.setFeedbackRating(rating);
//         attendance.setFeedbackComment(comment);
//         attendanceRepository.save(attendance);
        
//         log.info("Feedback added for session {} by user {}", sessionId, user.getId());
//     }
    
//     /**
//      * Get session statistics for instructor/admin
//      */
//     @Transactional(readOnly = true)
//     public SessionStats getSessionStats(Long sessionId) {
//         LiveSession session = liveSessionRepository.findById(sessionId)
//             .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
//         SessionStats stats = new SessionStats();
//         stats.setSessionTitle(session.getTitle());
//         stats.setSessionStatus(session.getStatus().toString());
//         stats.setTotalAttendees(session.getCurrentAttendees());
//         stats.setMaxCapacity(session.getMaxAttendees());
//         stats.setAverageRating(attendanceRepository.getAverageRatingForSession(sessionId));
//         stats.setTotalAttendance(attendanceRepository.countAttendedAttendees(sessionId));
//         stats.setAvailableSeats(session.getMaxAttendees() - session.getCurrentAttendees());
        
//         return stats;
//     }
    
//     /**
//      * Cancel a live session (admin/instructor only)
//      */
//     @Transactional
//     public void cancelLiveSession(Long sessionId, User user, String reason) {
//         LiveSession session = liveSessionRepository.findById(sessionId)
//             .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
//         // Check if user is host or admin
//         boolean isHost = session.getHostUserId() != null && session.getHostUserId().equals(user.getId());
//         boolean isAdmin = user.isAdmin();
        
//         if (!isHost && !isAdmin) {
//             throw new SecurityException("Only host or admin can cancel this session");
//         }
        
//         session.setStatus(LiveSessionStatus.CANCELLED);
//         liveSessionRepository.save(session);
        
//         log.info("Session {} cancelled by user {}: {}", sessionId, user.getId(), reason);
//     }
    
//     /**
//      * Get all live sessions (admin only)
//      */
//     @Transactional(readOnly = true)
//     public List<LiveSessionDTO> getAllLiveSessions(User user) {
//         if (!user.isAdmin()) {
//             throw new SecurityException("Only admin can view all sessions");
//         }
//         return liveSessionRepository.findAll().stream()
//             .map(this::convertToDTO)
//             .collect(Collectors.toList());
//     }
    
//     /**
//      * Get sessions by status (admin only)
//      */
//     @Transactional(readOnly = true)
//     public List<LiveSessionDTO> getSessionsByStatus(LiveSessionStatus status, User user) {
//         if (!user.isAdmin()) {
//             throw new SecurityException("Only admin can view sessions by status");
//         }
//         return liveSessionRepository.findByStatus(status).stream()
//             .map(this::convertToDTO)
//             .collect(Collectors.toList());
//     }
    
//     /**
//      * Update session recording URL after session ends
//      */
//     @Transactional
//     public void updateRecordingUrl(Long sessionId, String recordingUrl, User user) {
//         if (!user.isAdmin() && !user.isInstructor()) {
//             throw new SecurityException("Only admin or instructor can update recording URL");
//         }
        
//         LiveSession session = liveSessionRepository.findById(sessionId)
//             .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
//         session.setRecordingUrl(recordingUrl);
//         liveSessionRepository.save(session);
        
//         log.info("Recording URL updated for session {}: {}", sessionId, recordingUrl);
//     }
    
//     /**
//      * Get attendance report for a session (instructor/admin only)
//      */
//     @Transactional(readOnly = true)
//     public AttendanceReport getAttendanceReport(Long sessionId, User user) {
//         if (!user.isAdmin() && !user.isInstructor()) {
//             throw new SecurityException("Only admin or instructor can view attendance reports");
//         }
        
//         LiveSession session = liveSessionRepository.findById(sessionId)
//             .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
//         List<LiveSessionAttendance> attendances = attendanceRepository.findByLiveSessionId(sessionId);
        
//         AttendanceReport report = new AttendanceReport();
//         report.setSessionTitle(session.getTitle());
//         report.setSessionDate(session.getScheduledStartTime());
//         report.setTotalRegistered(session.getCurrentAttendees());
//         report.setTotalAttended((int) attendances.stream().filter(a -> Boolean.TRUE.equals(a.getAttended())).count());
//         report.setAverageAttendanceDuration(calculateAverageDuration(attendances));
        
//         List<AttendeeDetail> attendeeDetails = attendances.stream()
//             .map(a -> {
//                 AttendeeDetail detail = new AttendeeDetail();
//                 detail.setUserName(a.getUser().getFullName());
//                 detail.setUserEmail(a.getUser().getEmail());
//                 detail.setJoinedAt(a.getJoinedAt());
//                 detail.setLeftAt(a.getLeftAt());
//                 detail.setDurationMinutes(a.getDurationMinutes());
//                 detail.setAttended(a.getAttended());
//                 detail.setFeedbackRating(a.getFeedbackRating());
//                 detail.setFeedbackComment(a.getFeedbackComment());
//                 return detail;
//             })
//             .collect(Collectors.toList());
        
//         report.setAttendees(attendeeDetails);
        
//         return report;
//     }
    
//     /**
//      * Get user's upcoming subscribed sessions
//      */
//     @Transactional(readOnly = true)
//     public List<LiveSessionDTO> getUserUpcomingSessions(User user) {
//         List<LiveSession> allUpcoming = liveSessionRepository.findUpcomingSessions(LocalDateTime.now());
        
//         return allUpcoming.stream()
//             .filter(session -> {
//                 Long courseId = session.getCourse() != null ? session.getCourse().getId() : null;
//                 return checkUserSubscription(user, courseId) || user.isAdmin() || user.isInstructor();
//             })
//             .map(this::convertToDTO)
//             .collect(Collectors.toList());
//     }
    
//     /**
//      * Check if user has access to a session
//      */
//     @Transactional(readOnly = true)
//     public boolean hasAccessToSession(Long sessionId, User user) {
//         LiveSession session = liveSessionRepository.findById(sessionId)
//             .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
//         Long courseId = session.getCourse() != null ? session.getCourse().getId() : null;
//         return checkUserSubscription(user, courseId) || user.isAdmin() || user.isInstructor();
//     }
    
//     /**
//      * Get session count for a course
//      */
//     @Transactional(readOnly = true)
//     public long getSessionCountForCourse(Long courseId) {
//         return liveSessionRepository.findByCourseId(courseId).size();
//     }
    
//     /**
//      * Get upcoming session count for user
//      */
//     @Transactional(readOnly = true)
//     public long getUpcomingSessionCount(User user) {
//         List<LiveSession> upcoming = liveSessionRepository.findUpcomingSessions(LocalDateTime.now());
        
//         return upcoming.stream()
//             .filter(session -> {
//                 Long courseId = session.getCourse() != null ? session.getCourse().getId() : null;
//                 return checkUserSubscription(user, courseId) || user.isAdmin() || user.isInstructor();
//             })
//             .count();
//     }
    
//     /**
//      * Scheduled task to update session statuses (runs every minute)
//      */
//     @Scheduled(fixedDelay = 60000)
//     @Transactional
//     public void updateSessionStatuses() {
//         LocalDateTime now = LocalDateTime.now();
        
//         // Start sessions that should be live
//         List<LiveSession> toStart = liveSessionRepository.findSessionsThatShouldBeLive(now);
//         for (LiveSession session : toStart) {
//             log.info("Starting live session: {}", session.getTitle());
//             session.setStatus(LiveSessionStatus.LIVE);
//             liveSessionRepository.save(session);
//         }
        
//         // End sessions that should be ended
//         List<LiveSession> toEnd = liveSessionRepository.findSessionsThatShouldBeEnded(now);
//         for (LiveSession session : toEnd) {
//             log.info("Ending live session: {}", session.getTitle());
//             session.setStatus(LiveSessionStatus.ENDED);
//             liveSessionRepository.save(session);
//         }
//     }
    
//     /**
//      * Scheduled task to send reminders (runs every hour)
//      */
//     @Scheduled(cron = "0 0 * * * *")
//     @Transactional
//     public void sendReminders() {
//         LocalDateTime now = LocalDateTime.now();
//         LocalDateTime oneHourFromNow = now.plusHours(1);
//         LocalDateTime twoHoursFromNow = now.plusHours(2);
        
//         // Sessions starting in 1-2 hours
//         List<LiveSession> sessions = liveSessionRepository.findSessionsNeedingReminder(oneHourFromNow, twoHoursFromNow);
        
//         for (LiveSession session : sessions) {
//             session.setReminderSent(true);
//             liveSessionRepository.save(session);
//             log.info("Sent reminder for session: {}", session.getTitle());
//         }
//     }
    
//     /**
//      * Helper methods
//      */
//     private String generateMeetingId() {
//         return "MEET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//     }
    
//     private String generateJoinUrl(String meetingId) {
//         return "https://meet.cdax.com/" + meetingId;
//     }
    
//     private double calculateAverageDuration(List<LiveSessionAttendance> attendances) {
//         return attendances.stream()
//             .filter(a -> a.getDurationMinutes() != null)
//             .mapToInt(LiveSessionAttendance::getDurationMinutes)
//             .average()
//             .orElse(0.0);
//     }
    
//     private LiveSessionDTO convertToDTO(LiveSession session) {
//         LiveSessionDTO dto = new LiveSessionDTO();
//         dto.setId(session.getId());
//         dto.setTitle(session.getTitle());
//         dto.setDescription(session.getDescription());
//         dto.setScheduledStartTime(session.getScheduledStartTime());
//         dto.setScheduledEndTime(session.getScheduledEndTime());
//         dto.setMeetingId(session.getMeetingId());
//         dto.setJoinUrl(session.getJoinUrl());
//         dto.setStreamUrl(session.getStreamUrl());
//         dto.setStatus(session.getStatus().toString());
//         dto.setMaxAttendees(session.getMaxAttendees());
//         dto.setCurrentAttendees(session.getCurrentAttendees());
//         dto.setChatEnabled(session.getChatEnabled());
//         dto.setRecordingUrl(session.getRecordingUrl());
        
//         if (session.getCourse() != null) {
//             dto.setCourseId(session.getCourse().getId());
//             dto.setCourseName(session.getCourse().getTitle());
//         }
        
//         if (session.getModule() != null) {
//             dto.setModuleId(session.getModule().getId());
//             dto.setModuleName(session.getModule().getTitle());
//         }
        
//         return dto;
//     }
    
//     // ==================== INNER CLASSES ====================
    
//     /**
//      * Join Session Result
//      */
//     public static class JoinSessionResult {
//         private final boolean success;
//         private final String joinUrl;
//         private final String meetingId;
        
//         public JoinSessionResult(boolean success, String joinUrl, String meetingId) {
//             this.success = success;
//             this.joinUrl = joinUrl;
//             this.meetingId = meetingId;
//         }
        
//         public static final JoinSessionResult NOT_SUBSCRIBED = new JoinSessionResult(false, null, null);
//         public static final JoinSessionResult NOT_LIVE = new JoinSessionResult(false, null, null);
//         public static final JoinSessionResult SESSION_FULL = new JoinSessionResult(false, null, null);
//         public static final JoinSessionResult ALREADY_JOINED = new JoinSessionResult(false, null, null);
        
//         public boolean isSuccess() { return success; }
//         public String getJoinUrl() { return joinUrl; }
//         public String getMeetingId() { return meetingId; }
        
//         public String getErrorCode() {
//             if (this == NOT_SUBSCRIBED) return "NOT_SUBSCRIBED";
//             if (this == NOT_LIVE) return "NOT_LIVE";
//             if (this == SESSION_FULL) return "SESSION_FULL";
//             if (this == ALREADY_JOINED) return "ALREADY_JOINED";
//             return null;
//         }
//     }
    
//     /**
//      * Session Statistics
//      */
//     public static class SessionStats {
//         private String sessionTitle;
//         private String sessionStatus;
//         private int totalAttendees;
//         private int maxCapacity;
//         private int availableSeats;
//         private Double averageRating;
//         private long totalAttendance;
        
//         // Getters and setters
//         public String getSessionTitle() { return sessionTitle; }
//         public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }
        
//         public String getSessionStatus() { return sessionStatus; }
//         public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }
        
//         public int getTotalAttendees() { return totalAttendees; }
//         public void setTotalAttendees(int totalAttendees) { this.totalAttendees = totalAttendees; }
        
//         public int getMaxCapacity() { return maxCapacity; }
//         public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
        
//         public int getAvailableSeats() { return availableSeats; }
//         public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
        
//         public Double getAverageRating() { return averageRating; }
//         public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
        
//         public long getTotalAttendance() { return totalAttendance; }
//         public void setTotalAttendance(long totalAttendance) { this.totalAttendance = totalAttendance; }
//     }
    
//     /**
//      * Attendance Report
//      */
//     public static class AttendanceReport {
//         private String sessionTitle;
//         private LocalDateTime sessionDate;
//         private int totalRegistered;
//         private int totalAttended;
//         private double averageAttendanceDuration;
//         private List<AttendeeDetail> attendees;
        
//         // Getters and setters
//         public String getSessionTitle() { return sessionTitle; }
//         public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }
        
//         public LocalDateTime getSessionDate() { return sessionDate; }
//         public void setSessionDate(LocalDateTime sessionDate) { this.sessionDate = sessionDate; }
        
//         public int getTotalRegistered() { return totalRegistered; }
//         public void setTotalRegistered(int totalRegistered) { this.totalRegistered = totalRegistered; }
        
//         public int getTotalAttended() { return totalAttended; }
//         public void setTotalAttended(int totalAttended) { this.totalAttended = totalAttended; }
        
//         public double getAverageAttendanceDuration() { return averageAttendanceDuration; }
//         public void setAverageAttendanceDuration(double averageAttendanceDuration) { this.averageAttendanceDuration = averageAttendanceDuration; }
        
//         public List<AttendeeDetail> getAttendees() { return attendees; }
//         public void setAttendees(List<AttendeeDetail> attendees) { this.attendees = attendees; }
//     }
    
//     /**
//      * Attendee Detail
//      */
//     public static class AttendeeDetail {
//         private String userName;
//         private String userEmail;
//         private LocalDateTime joinedAt;
//         private LocalDateTime leftAt;
//         private Integer durationMinutes;
//         private Boolean attended;
//         private Integer feedbackRating;
//         private String feedbackComment;
        
//         // Getters and setters
//         public String getUserName() { return userName; }
//         public void setUserName(String userName) { this.userName = userName; }
        
//         public String getUserEmail() { return userEmail; }
//         public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        
//         public LocalDateTime getJoinedAt() { return joinedAt; }
//         public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
        
//         public LocalDateTime getLeftAt() { return leftAt; }
//         public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }
        
//         public Integer getDurationMinutes() { return durationMinutes; }
//         public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        
//         public Boolean getAttended() { return attended; }
//         public void setAttended(Boolean attended) { this.attended = attended; }
        
//         public Integer getFeedbackRating() { return feedbackRating; }
//         public void setFeedbackRating(Integer feedbackRating) { this.feedbackRating = feedbackRating; }
        
//         public String getFeedbackComment() { return feedbackComment; }
//         public void setFeedbackComment(String feedbackComment) { this.feedbackComment = feedbackComment; }
//     }
// }