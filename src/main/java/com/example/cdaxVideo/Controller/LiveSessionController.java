// package com.example.cdaxVideo.Controller;

// import com.example.cdaxVideo.DTO.CreateLiveSessionRequestDTO;
// import com.example.cdaxVideo.DTO.LiveSessionDTO;
// import com.example.cdaxVideo.Entity.User;
// import com.example.cdaxVideo.Repository.UserRepository;
// import com.example.cdaxVideo.Service.LiveSessionService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.web.bind.annotation.*;

// import jakarta.validation.Valid;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// @Slf4j
// @RestController
// @RequestMapping("/api/live")
// @RequiredArgsConstructor
// public class LiveSessionController {
    
//     private final LiveSessionService liveSessionService;
//     private final UserRepository userRepository;
    
//     private User getUserFromUserDetails(UserDetails userDetails) {
//         return userRepository.findByEmail(userDetails.getUsername())
//             .orElseThrow(() -> new RuntimeException("User not found"));
//     }
    
//     /**
//      * Create a new live session (Admin/Instructor only)
//      * POST /api/live/sessions
//      */
//     @PostMapping("/sessions")
//     @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
//     public ResponseEntity<?> createLiveSession(@Valid @RequestBody CreateLiveSessionRequestDTO request,
//                                                @AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             User user = getUserFromUserDetails(userDetails);
//             var session = liveSessionService.createLiveSession(request, user.getId());
//             Map<String, Object> response = new HashMap<>();
//             response.put("success", true);
//             response.put("message", "Live session created successfully");
//             response.put("session", session);
//             return ResponseEntity.status(HttpStatus.CREATED).body(response);
//         } catch (Exception e) {
//             log.error("Error creating live session", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Get upcoming sessions for current user
//      * GET /api/live/upcoming
//      */
//     @GetMapping("/upcoming")
//     public ResponseEntity<?> getUpcomingSessions(@AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             User user = getUserFromUserDetails(userDetails);
//             List<LiveSessionDTO> sessions = liveSessionService.getUpcomingSessions(user);
//             return ResponseEntity.ok(sessions);
//         } catch (Exception e) {
//             log.error("Error getting upcoming sessions", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Get current live sessions (no authentication required)
//      * GET /api/live/now
//      */
//     @GetMapping("/now")
//     public ResponseEntity<?> getCurrentLiveSessions() {
//         try {
//             List<LiveSessionDTO> sessions = liveSessionService.getCurrentLiveSessions();
//             return ResponseEntity.ok(sessions);
//         } catch (Exception e) {
//             log.error("Error getting live sessions", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Get specific session details
//      * GET /api/live/sessions/{sessionId}
//      */
//     @GetMapping("/sessions/{sessionId}")
//     public ResponseEntity<?> getSession(@PathVariable Long sessionId,
//                                         @AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             User user = getUserFromUserDetails(userDetails);
//             LiveSessionDTO session = liveSessionService.getLiveSessionWithAccess(sessionId, user);
//             return ResponseEntity.ok(session);
//         } catch (Exception e) {
//             log.error("Error getting session ", e);
//             return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                 .body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Get upcoming sessions for a specific course
//      * GET /api/live/courses/{courseId}/upcoming
//      */
//     @GetMapping("/courses/{courseId}/upcoming")
//     public ResponseEntity<?> getUpcomingSessionsForCourse(@PathVariable Long courseId,
//                                                           @AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             User user = getUserFromUserDetails(userDetails);
//             List<LiveSessionDTO> sessions = liveSessionService.getUpcomingSessionsForCourse(courseId, user);
//             return ResponseEntity.ok(sessions);
//         } catch (Exception e) {
//             log.error("Error getting course sessions", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Join a live session
//      * POST /api/live/sessions/{sessionId}/join
//      */
//     @PostMapping("/sessions/{sessionId}/join")
//     public ResponseEntity<?> joinSession(@PathVariable Long sessionId,
//                                         @AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             User user = getUserFromUserDetails(userDetails);
//             LiveSessionService.JoinSessionResult result = liveSessionService.joinLiveSession(sessionId, user);
            
//             Map<String, Object> response = new HashMap<>();
            
//             if (result.isSuccess()) {
//                 response.put("success", true);
//                 response.put("joinUrl", result.getJoinUrl());
//                 response.put("meetingId", result.getMeetingId());
//                 response.put("message", "Successfully joined session");
//                 return ResponseEntity.ok(response);
//             } else {
//                 response.put("success", false);
//                 response.put("errorCode", result.getErrorCode());
                
//                 switch (result.getErrorCode()) {
//                     case "NOT_SUBSCRIBED":
//                         response.put("message", "This live session is only available for subscribed users. Please subscribe to join.");
//                         response.put("requiresSubscription", true);
//                         break;
//                     case "NOT_LIVE":
//                         response.put("message", "This session is not currently live.");
//                         break;
//                     case "SESSION_FULL":
//                         response.put("message", "This session has reached maximum capacity.");
//                         break;
//                     case "ALREADY_JOINED":
//                         response.put("message", "You have already joined this session.");
//                         break;
//                     default:
//                         response.put("message", "Cannot join session at this time.");
//                 }
                
//                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
//             }
//         } catch (Exception e) {
//             log.error("Error joining session", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Leave a live session
//      * POST /api/live/sessions/{sessionId}/leave
//      */
//     @PostMapping("/sessions/{sessionId}/leave")
//     public ResponseEntity<?> leaveSession(@PathVariable Long sessionId,
//                                          @AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             User user = getUserFromUserDetails(userDetails);
//             liveSessionService.leaveLiveSession(sessionId, user);
//             return ResponseEntity.ok(Map.of("success", true, "message", "Left session successfully"));
//         } catch (Exception e) {
//             log.error("Error leaving session", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Add feedback for a session
//      * POST /api/live/sessions/{sessionId}/feedback
//      */
//     @PostMapping("/sessions/{sessionId}/feedback")
//     public ResponseEntity<?> addFeedback(@PathVariable Long sessionId,
//                                         @RequestParam Integer rating,
//                                         @RequestParam(required = false) String comment,
//                                         @AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             // Validate rating
//             if (rating < 1 || rating > 5) {
//                 return ResponseEntity.badRequest().body(Map.of("error", "Rating must be between 1 and 5"));
//             }
            
//             User user = getUserFromUserDetails(userDetails);
//             liveSessionService.addFeedback(sessionId, user, rating, comment);
//             return ResponseEntity.ok(Map.of("success", true, "message", "Feedback submitted successfully"));
//         } catch (Exception e) {
//             log.error("Error adding feedback", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Get session statistics (Admin/Instructor only)
//      * GET /api/live/sessions/{sessionId}/stats
//      */
//     @GetMapping("/sessions/{sessionId}/stats")
//     @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
//     public ResponseEntity<?> getSessionStats(@PathVariable Long sessionId) {
//         try {
//             LiveSessionService.SessionStats stats = liveSessionService.getSessionStats(sessionId);
//             return ResponseEntity.ok(stats);
//         } catch (Exception e) {
//             log.error("Error getting session stats", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Cancel a live session (Admin/Instructor only)
//      * POST /api/live/sessions/{sessionId}/cancel
//      */
//     @PostMapping("/sessions/{sessionId}/cancel")
//     @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
//     public ResponseEntity<?> cancelSession(@PathVariable Long sessionId,
//                                           @RequestParam String reason,
//                                           @AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             User user = getUserFromUserDetails(userDetails);
//             liveSessionService.cancelLiveSession(sessionId, user, reason);
//             return ResponseEntity.ok(Map.of("success", true, "message", "Session cancelled successfully"));
//         } catch (Exception e) {
//             log.error("Error cancelling session", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Get user's session history
//      * GET /api/live/my-sessions
//      */
//     @GetMapping("/my-sessions")
//     public ResponseEntity<?> getUserSessionHistory(@AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             User user = getUserFromUserDetails(userDetails);
//             List<LiveSessionDTO> sessions = liveSessionService.getUserLiveSessionHistory(user);
//             return ResponseEntity.ok(sessions);
//         } catch (Exception e) {
//             log.error("Error getting user history", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
    
//     /**
//      * Check if user can join a session (pre-flight check)
//      * GET /api/live/sessions/{sessionId}/can-join
//      */
//     @GetMapping("/sessions/{sessionId}/can-join")
//     public ResponseEntity<?> canJoinSession(@PathVariable Long sessionId,
//                                            @AuthenticationPrincipal UserDetails userDetails) {
//         try {
//             User user = getUserFromUserDetails(userDetails);
//             LiveSessionDTO session = liveSessionService.getLiveSessionWithAccess(sessionId, user);
            
//             Map<String, Object> response = new HashMap<>();
//             response.put("canJoin", session.getCanJoin());
//             response.put("isSubscribed", session.getIsSubscribed());
//             response.put("isLive", "LIVE".equals(session.getStatus()));
//             response.put("isFull", session.getCurrentAttendees() >= session.getMaxAttendees());
//             response.put("availableSeats", session.getAvailableSeats());
//             response.put("status", session.getStatus());
            
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             log.error("Error checking join eligibility", e);
//             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//         }
//     }
// }