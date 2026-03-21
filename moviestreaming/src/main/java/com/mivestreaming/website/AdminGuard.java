package com.mivestreaming.website;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * AdminGuard – Helper dùng trong Controller để kiểm tra quyền Admin.
 *
 * Cách sử dụng trong Controller:
 * <pre>
 *   ResponseEntity<?> guard = AdminGuard.require(request);
 *   if (guard != null) return guard; // Trả về 401/403 nếu không hợp lệ
 *   // ... logic cho Admin ...
 * </pre>
 */
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

    /**
     * Tương tự, nhưng trả về boolean (dùng trong các trường hợp đơn giản hơn).
     */
    public static boolean isAdmin(HttpServletRequest request) {
        return User.Role.ROLE_ADMIN.name().equals(request.getAttribute("jwt_role"));
    }
}
