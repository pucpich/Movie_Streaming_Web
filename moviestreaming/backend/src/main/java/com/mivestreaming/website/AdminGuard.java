package com.mivestreaming.website;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public final class AdminGuard {

    private AdminGuard() {} // Utility class, không được khởi tạo

    /**
     * Kiểm tra request có từ Admin đã xác thực chưa.
     *
     * @param request HTTP request (đã qua JwtAuthFilter)
     * @return null nếu hợp lệ là Admin — hoặc ResponseEntity lỗi 401/403
     */
    public static ResponseEntity<?> require(HttpServletRequest request) {
        String role = (String) request.getAttribute("jwt_role");

        // Chưa đăng nhập hoặc token không hợp lệ
        if (role == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bạn cần đăng nhập để thực hiện thao tác này."));
        }

        // Đã đăng nhập nhưng không phải Admin
        if (!User.Role.ROLE_ADMIN.name().equals(role)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Bạn không có quyền truy cập. Chỉ Admin mới được phép."));
        }

        // Hợp lệ → cho phép đi tiếp
        return null;
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return User.Role.ROLE_ADMIN.name().equals(request.getAttribute("jwt_role"));
    }
}
