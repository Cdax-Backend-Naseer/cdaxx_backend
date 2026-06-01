package com.example.cdaxVideo.Config;

import com.example.cdaxVideo.Service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    // Cache of public endpoints to avoid repeated string operations
    private final Set<String> publicEndpoints = new HashSet<>();
    
    public JwtRequestFilter() {
        // Initialize public endpoints cache
        initializePublicEndpoints();
    }
    
    private void initializePublicEndpoints() {
        // Static public endpoints
        publicEndpoints.add("/api/dashboard/public");
        publicEndpoints.add("/swagger-ui.html");
        publicEndpoints.add("/actuator/health");
        publicEndpoints.add("/actuator/info");
        
        // Auth endpoints
        publicEndpoints.add("/api/auth/login");
        publicEndpoints.add("/api/auth/register");
        publicEndpoints.add("/api/auth/jwt/login");
        publicEndpoints.add("/api/auth/jwt/register");
        publicEndpoints.add("/api/auth/jwt/validate");
        publicEndpoints.add("/api/auth/jwt/refresh");
        publicEndpoints.add("/api/auth/forgot-password");
        publicEndpoints.add("/api/auth/reset-password");
        publicEndpoints.add("/api/auth/verify-email");
        publicEndpoints.add("/api/auth/firstName");
        publicEndpoints.add("/api/auth/getUserByEmail");
        publicEndpoints.add("/api/auth/logout");
        
        // Course endpoints
        publicEndpoints.add("/api/courses");
        publicEndpoints.add("/api/courses/tags/popular");
        publicEndpoints.add("/api/courses/search/suggestions");
        publicEndpoints.add("/api/courses/advanced-search");
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Let ALL requests go through doFilterInternal
        // We'll handle the public/protected logic there
        return false;
    }
    
    private boolean isPublicEndpoint(String path, String method) {
        // Always skip OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        
        // Remove query parameters for clean matching
        String cleanPath = path.split("\\?")[0];
        
        System.out.println("   Checking if endpoint is public: " + cleanPath + " [" + method + "]");
        
        // ============ PUBLIC RESOURCES (always public) ============
        if (cleanPath.startsWith("/api/public/") ||
            cleanPath.startsWith("/api/debug/") ||
            cleanPath.startsWith("/uploads/") ||
            cleanPath.startsWith("/swagger-ui/") ||
            cleanPath.startsWith("/v3/api-docs/") ||
            cleanPath.startsWith("/webjars/") ||
            cleanPath.startsWith("/swagger-resources/") ||
            cleanPath.startsWith("/api/videos/public/") ||
            cleanPath.startsWith("/api/test/")) {
            
            System.out.println("   ✅ Public resource match");
            return true;
        }
        
        // ============ PROTECTED PATHS - NEVER PUBLIC ============
        // These should NEVER be treated as public, regardless of method
        if (cleanPath.startsWith("/api/auth/profile/") ||
            cleanPath.startsWith("/api/auth/change-password") ||
            cleanPath.startsWith("/api/courses/subscribed/") ||
            cleanPath.startsWith("/api/courses/user/") ||
            cleanPath.startsWith("/api/purchase") ||
            cleanPath.startsWith("/api/cart/") ||
            cleanPath.startsWith("/api/favorites/") ||
            cleanPath.startsWith("/api/dashboard/") ||
            cleanPath.startsWith("/api/streak/") ||
            cleanPath.startsWith("/api/profile/streak") ||
            cleanPath.matches("/api/videos/\\d+/progress") ||
            cleanPath.matches("/api/videos/\\d+/complete")) {
            
            System.out.println("   ❌ Protected endpoint - NOT public");
            return false;  // These are NEVER public
        }
        
        // ============ AUTHENTICATION ENDPOINTS (public) ============
        if (cleanPath.startsWith("/api/auth/")) {
            // Specific public auth endpoints
            boolean isPublicAuth = publicEndpoints.contains(cleanPath);
            System.out.println("   Auth endpoint, is public: " + isPublicAuth);
            return isPublicAuth;
        }
        
        // ============ GET endpoints that are public ============
        if ("GET".equalsIgnoreCase(method)) {
            // Exact match public GET endpoints
            if (cleanPath.equals("/api/courses") ||
                cleanPath.equals("/api/courses/tags/popular") ||
                cleanPath.equals("/api/courses/search/suggestions") ||
                cleanPath.equals("/api/courses/advanced-search") ||
                
                // Pattern matches for GET
                cleanPath.matches("/api/courses/\\d+") ||              // /api/courses/{id}
                cleanPath.startsWith("/api/courses/public") ||
                cleanPath.startsWith("/api/courses/tag/") ||
                
                // Module endpoints - GET only
                cleanPath.matches("/api/modules/\\d+") ||              // /api/modules/{id}
                cleanPath.matches("/api/modules/course/\\d+") ||       // /api/modules/course/{courseId}
                cleanPath.matches("/api/modules/\\d+/videos") ||       // /api/modules/{moduleId}/videos
                cleanPath.matches("/api/modules/\\d+/assessments") ||  // /api/modules/{moduleId}/assessments
                
                // Assessment endpoints - GET only
                cleanPath.startsWith("/api/course/assessment/") ||
                cleanPath.startsWith("/api/assessments/")) {
                
                System.out.println("   ✅ Public GET endpoint");
                return true;
            }
        }
        
        // ============ Everything else requires authentication ============
        System.out.println("   🔒 Endpoint requires authentication");
        return false;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain)
            throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("🔐 JWT Filter processing: " + method + " " + requestPath);
        
        // First, check if this is a public endpoint
        if (isPublicEndpoint(requestPath, method)) {
            System.out.println("✅ Public endpoint - continuing without authentication");
            chain.doFilter(request, response);
            return;
        }
        
        // For protected endpoints, require authentication
        final String requestTokenHeader = request.getHeader("Authorization");
        
        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            System.out.println("❌ No Bearer token for protected endpoint: " + requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"status\":401}");
            return;
        }
        
        String jwtToken = requestTokenHeader.substring(7);
        String username = null;
        
        try {
            username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            System.out.println("Extracted Username: " + username);
        } catch (Exception e) {
            System.out.println("Error extracting username: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid token\",\"status\":401}");
            return;
        }
        
        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                    System.out.println("✅ Token validated for: " + username);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    System.out.println("❌ Token validation failed for: " + username);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Token validation failed\",\"status\":401}");
                    return;
                }
            } catch (Exception e) {
                System.out.println("Error setting authentication: " + e.getMessage());
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication error\",\"status\":401}");
                return;
            }
        }
        
        // Continue the filter chain
        chain.doFilter(request, response);
    }
}                                        