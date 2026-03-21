package com.mivestreaming.website;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Middleware (Filter) xác thực JWT.
 *
 * Hoạt động:
 *   1. Đọc header "Authorization: Bearer <token>"
 *   2. Nếu có token hợp lệ → gắn thông tin user (userId, email, role)
 *      vào request attribute để các Controller đọc được
 *   3. Nếu không có hoặc token lỗi → để request đi qua bình thường
 *      (các endpoint cần auth sẽ tự kiểm tra trong Controller/AdminGuard)
 *
 * Filter chạy đúng 1 lần cho mỗi request (OncePerRequestFilter).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Kiểm tra header có dạng "Bearer <token>" không
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Bỏ prefix "Bearer "

            if (jwtService.validateToken(token)) {
                // Gắn thông tin vào request attribute để Controller đọc
                request.setAttribute("jwt_userId", jwtService.getUserIdFromToken(token));
                request.setAttribute("jwt_email",  jwtService.getEmailFromToken(token));
                request.setAttribute("jwt_role",   jwtService.getRoleFromToken(token));
            }
        }

        // Cho request tiếp tục đi qua filter chain
        filterChain.doFilter(request, response);
    }
}
