package com.demo.myapp.filter;

import com.demo.myapp.pojo.LoginUser;
import com.demo.myapp.pojo.User;
import com.demo.myapp.service.impl.CachedUserService;
import com.demo.myapp.utils.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Author: Yupeng Li
 * @Date: 2/7/2024 14:36
 * @Description: IMPROVED - Filter to intercept the request and validate the JWT token before it reaches the controller
 * 
 * IMPROVEMENTS:
 * - Better error handling with proper HTTP responses
 * - Enhanced logging with correlation ID support
 * - Improved token extraction supporting multiple sources
 * - More secure password handling
 * - Better separation of concerns
 */
@Component
@Order(2) // Execute after CorrelationIdFilter
public class JwtRequestFilter extends OncePerRequestFilter {
    @Resource
    JwtUtil jwtUtil;
    @Resource
    CachedUserService cachedUserService;
    
    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        // IMPROVED: Skip JWT validation for public endpoints
        if (isPublicEndpoint(requestUri, method)) {
            logger.debug("Skipping JWT validation for public endpoint: {} {}", method, requestUri);
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // IMPROVED: Extract token from multiple sources (cookies and Authorization header)
        String token = jwtUtil.extractTokenFromRequest(request);

        // If token is null, clear context and pass to next filter for potential form-based auth
        if (token == null) {
            logger.debug("No JWT token found in request for: {} {}", method, requestUri);
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // Check if the token is valid
        if (!jwtUtil.isValidToken(token)){
            logger.warn("Invalid JWT token attempted for: {} {}", method, requestUri);
            SecurityContextHolder.clearContext();
            sendUnauthorizedResponse(response, "Invalid or expired token");
            return;
        }

        try {
            // Get the LoginUser after passed all the checks
            LoginUser loginUser = getLoginUser(token);

            // Create authentication token
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Store in security context
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            logger.debug("JWT authentication successful for user: {}", loginUser.getUser().getUsername());

            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Error during JWT authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            sendUnauthorizedResponse(response, "Authentication failed");
        }
    }
    
    /**
     * IMPROVED: Centralized check for public endpoints that don't require authentication
     */
    private boolean isPublicEndpoint(String uri, String method) {
        // Login and registration endpoints
        if ("/login".equals(uri) && "POST".equals(method)) return true;
        if ("/register".equals(uri) && "POST".equals(method)) return true;
        if ("/verify-code".equals(uri) && "POST".equals(method)) return true;
        if ("/forgot-password".equals(uri) && "POST".equals(method)) return true;
        if ("/change-password".equals(uri) && "POST".equals(method)) return true;
        
        // WebSocket endpoints
        if (uri.startsWith("/ws/")) return true;

        // OpenAPI Docs
        if (uri.startsWith("/swagger-ui")) return true;
        if (uri.startsWith("/v3")) return true;
        
        return false;
    }
    
    /**
     * IMPROVED: Proper error response with correct content type and structure
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\", \"status\": 401}", message);
        response.getWriter().write(jsonResponse);
    }

    /**
     * IMPROVED: Parse the token and get the user information to create a LoginUser object
     * @param token token to parse
     * @return LoginUser object
     * @throws UsernameNotFoundException if the user is not found
     * @Description: Get the newest user information from the database/cache with better error handling
     */
    private LoginUser getLoginUser(String token) {
        try {
            // Parse the token to get the user information
            Map<String, Object> claims = jwtUtil.parseToken(token);

            // IMPROVED: Better validation of token claims
            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                throw new UsernameNotFoundException("Invalid token: missing userId claim");
            }

            Long userId = Long.parseLong(userIdObj.toString());
            
            // Get the user information from cache/database
            User user = cachedUserService.getUserById(userId);
            if (user == null) {
                logger.warn("User not found for userId: {} from valid JWT token", userId);
                throw new UsernameNotFoundException("User not found with ID: " + userId);
            }
            
            // IMPROVED: Clear password immediately after retrieval for security
            user.setPassword(null);
            
            // Get user roles and permissions
            List<String> roles = cachedUserService.getRolesByUserId(userId);
            List<String> permissions = cachedUserService.getPermissionsByUserId(userId);

            logger.debug("Successfully loaded user data from token for userId: {}", userId);
            return new LoginUser(user, permissions, roles);
            
        } catch (NumberFormatException e) {
            logger.error("Invalid userId format in JWT token: {}", e.getMessage());
            throw new UsernameNotFoundException("Invalid token: malformed userId");
        } catch (Exception e) {
            logger.error("Error parsing JWT token or loading user: {}", e.getMessage());
            throw new UsernameNotFoundException("Authentication failed: " + e.getMessage());
        }
    }
}