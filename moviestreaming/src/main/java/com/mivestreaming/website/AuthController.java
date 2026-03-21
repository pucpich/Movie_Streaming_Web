package com.mivestreaming.website;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller xác thực người dùng.
 *
 * Endpoints:
 *   POST  /api/auth/register   – Đăng ký tài khoản mới
 *   POST  /api/auth/login      – Đăng nhập, nhận JWT token
 *   GET   /api/auth/me         – Lấy thông tin user hiện tại (từ JWT)
 *   POST  /api/auth/logout     – Đăng xuất (xóa session phía server)
 *   POST  /api/auth/make-admin – Tự nâng quyền Admin (DEV ONLY - xóa trước khi production)
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService     jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService     = jwtService;
    }

    // =====================================================================
    // REGISTER
    // =====================================================================

    /**
     * POST /api/auth/register
     *
     * Body: { "fullName": "...", "email": "...", "password": "..." }
     * Response: { "id", "fullName", "email", "role", "token" }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request,
                                       HttpSession session) {
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email đã được sử dụng."));
        }

        // Tạo user mới
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Mã hóa BCrypt
        user.setRole(User.Role.ROLE_USER); // Mặc định là User thường

        User saved = userRepository.save(user);

        // Giữ session cũ để tương thích frontend
        session.setAttribute("USER_ID",    saved.getId());
        session.setAttribute("USER_NAME",  saved.getFullName());
        session.setAttribute("USER_EMAIL", saved.getEmail());

        // Tạo JWT token
        String token = jwtService.generateToken(saved);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildAuthResponse(saved, token));
    }

    // =====================================================================
    // LOGIN
    // =====================================================================

    /**
     * POST /api/auth/login
     *
     * Body: { "email": "...", "password": "..." }
     * Response: { "id", "fullName", "email", "role", "token" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                    HttpSession session) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email hoặc mật khẩu không đúng."));
        }

        User user = userOpt.get();
        String stored = user.getPassword() == null ? "" : user.getPassword();
        boolean ok;

        // Hỗ trợ dữ liệu cũ (plain text) song song BCrypt
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            ok = passwordEncoder.matches(request.getPassword(), stored);
        } else {
            ok = stored.equals(request.getPassword());
            // Nâng cấp lên BCrypt nếu đúng mật khẩu cũ
            if (ok) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);
            }
        }

        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email hoặc mật khẩu không đúng."));
        }

        // Giữ session cũ để tương thích frontend
        session.setAttribute("USER_ID",    user.getId());
        session.setAttribute("USER_NAME",  user.getFullName());
        session.setAttribute("USER_EMAIL", user.getEmail());

        // Tạo JWT token
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(buildAuthResponse(user, token));
    }

    // =====================================================================
    // ME – Thông tin user hiện tại
    // =====================================================================

    /**
     * GET /api/auth/me
     *
     * Hỗ trợ 2 cách xác thực:
     *   1. JWT Bearer token (ưu tiên)
     *   2. Session (fallback, tương thích frontend cũ)
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request, HttpSession session) {
        // --- Ưu tiên JWT ---
        String jwtUserId = (String) request.getAttribute("jwt_userId");
        if (jwtUserId != null) {
            Optional<User> userOpt = userRepository.findById(jwtUserId);
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                return ResponseEntity.ok(buildAuthResponse(u, null));
            }
        }

        // --- Fallback Session (cho frontend cũ chưa dùng JWT) ---
        Object sessionId = session.getAttribute("USER_ID");
        if (sessionId != null) {
            AuthResponse res = new AuthResponse();
            res.setId((String) sessionId);
            res.setFullName((String) session.getAttribute("USER_NAME"));
            res.setEmail((String) session.getAttribute("USER_EMAIL"));
            res.setRole(User.Role.ROLE_USER.name()); // Session cũ không lưu role
            return ResponseEntity.ok(res);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Chưa đăng nhập."));
    }

    // =====================================================================
    // LOGOUT
    // =====================================================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Đã đăng xuất thành công."));
    }

    // =====================================================================
    // MAKE-ADMIN (DEV ONLY – xóa trước khi deploy production!)
    // =====================================================================

    /**
     * POST /api/auth/make-admin
     *
     * Nâng quyền Admin cho email bất kỳ. Bảo vệ bằng dev-secret.
     * Body: { "email": "admin@example.com", "devSecret": "dev2026" }
     *
     * ⚠️ XÓA ENDPOINT NÀY TRƯỚC KHI DEPLOY LÊN PRODUCTION!
     */
    @PostMapping("/make-admin")
    public ResponseEntity<?> makeAdmin(@RequestBody MakeAdminRequest body) {
        // Bảo vệ bằng secret đơn giản (chỉ dùng trong dev)
        if (!"dev2026".equals(body.getDevSecret())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Sai dev secret."));
        }

        Optional<User> opt = userRepository.findByEmail(body.getEmail());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy user với email: " + body.getEmail()));
        }

        User user = opt.get();
        user.setRole(User.Role.ROLE_ADMIN);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Đã nâng quyền Admin thành công.",
                "email",   user.getEmail(),
                "role",    user.getRole().name()
        ));
    }

    // =====================================================================
    // Helper – Build response object
    // =====================================================================

    private AuthResponse buildAuthResponse(User user, String token) {
        AuthResponse r = new AuthResponse();
        r.setId(user.getId());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole() != null ? user.getRole().name() : User.Role.ROLE_USER.name());
        r.setToken(token); // null nếu gọi từ /me qua session
        return r;
    }

    // =====================================================================
    // DTO / Inner Classes
    // =====================================================================

    public static class RegisterRequest {
        private String fullName, email, password;
        public String getFullName()  { return fullName; }
        public void   setFullName(String v)  { this.fullName  = v; }
        public String getEmail()     { return email; }
        public void   setEmail(String v)     { this.email     = v; }
        public String getPassword()  { return password; }
        public void   setPassword(String v)  { this.password  = v; }
    }

    public static class LoginRequest {
        private String email, password;
        public String getEmail()    { return email; }
        public void   setEmail(String v)    { this.email    = v; }
        public String getPassword() { return password; }
        public void   setPassword(String v) { this.password = v; }
    }

    public static class MakeAdminRequest {
        private String email, devSecret;
        public String getEmail()     { return email; }
        public void   setEmail(String v)     { this.email     = v; }
        public String getDevSecret() { return devSecret; }
        public void   setDevSecret(String v) { this.devSecret = v; }
    }

    public static class AuthResponse {
        private String id;
        private String fullName, email, role, token;

        public String getId()       { return id; }
        public void   setId(String v)       { this.id       = v; }
        public String getFullName() { return fullName; }
        public void   setFullName(String v){ this.fullName = v; }
        public String getEmail()    { return email; }
        public void   setEmail(String v)  { this.email    = v; }
        public String getRole()     { return role; }
        public void   setRole(String v)   { this.role     = v; }
        public String getToken()    { return token; }
        public void   setToken(String v)  { this.token    = v; }
    }
}
