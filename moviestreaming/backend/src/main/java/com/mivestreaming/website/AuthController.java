package com.mivestreaming.website;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService     jwtService;
    private final MovieRepository movieRepository;
    private final MovieService movieService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository, JwtService jwtService, MovieRepository movieRepository, MovieService movieService) {
        this.userRepository = userRepository;
        this.jwtService     = jwtService;
        this.movieRepository = movieRepository;
        this.movieService = movieService;
    }

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

    private Optional<User> getCurrentUser(HttpServletRequest request, HttpSession session) {
        String jwtUserId = (String) request.getAttribute("jwt_userId");
        if (jwtUserId != null) {
            Optional<User> userOpt = userRepository.findById(jwtUserId);
            if (userOpt.isPresent()) {
                return userOpt;
            }
        }

        Object sessionId = session.getAttribute("USER_ID");
        if (sessionId != null) {
            return userRepository.findById((String) sessionId);
        }

        return Optional.empty();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(buildAuthResponse(userOpt.get(), null));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Chưa đăng nhập."));
    }

    @GetMapping("/favorites")
    public ResponseEntity<?> getFavorites(HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        User user = userOpt.get();
        java.util.List<Map<String, Object>> favorites = new java.util.ArrayList<>();

        if (user.isAdmin()) {
            for (User u : userRepository.findAll()) {
                for (String movieId : u.getFavoriteMovieIds()) {
                    try {
                        Map<String, Object> m = movieService.getMovieDetails(movieId);
                        if (m != null) favorites.add(m);
                    } catch (Exception e) {
                        // bỏ qua id không hợp lệ
                    }
                }
            }
        } else {
            for (String movieId : user.getFavoriteMovieIds()) {
                try {
                    Map<String, Object> m = movieService.getMovieDetails(movieId);
                    if (m != null) favorites.add(m);
                } catch (Exception e) {
                    // bỏ qua id không hợp lệ
                }
            }
        }

        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/favorites")
    public ResponseEntity<?> addFavorite(@RequestBody java.util.Map<String, String> body, HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        String movieId = body.get("movieId");
        if (movieId == null || movieId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "movieId bắt buộc."));
        }

        User user = userOpt.get();
        if (!user.getFavoriteMovieIds().contains(movieId)) {
            user.getFavoriteMovieIds().add(movieId);
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of("message", "Đã thêm phim yêu thích.", "favorites", user.getFavoriteMovieIds()));
    }

    @DeleteMapping("/favorites/{movieId}")
    public ResponseEntity<?> removeFavorite(@PathVariable String movieId, HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        User user = userOpt.get();
        if (user.getFavoriteMovieIds().remove(movieId)) {
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of("message", "Đã xoá khỏi yêu thích.", "favorites", user.getFavoriteMovieIds()));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        User user = userOpt.get();
        java.util.List<Map<String, Object>> history = new java.util.ArrayList<>();

        // Sắp xếp theo thời gian xem (mới nhất trước)
        user.getWatchedHistory().stream()
            .sorted((a, b) -> b.getWatchedAt().compareTo(a.getWatchedAt()))
            .forEach(watchedMovie -> {
                try {
                    Map<String, Object> m = movieService.getMovieDetails(watchedMovie.getMovieId());
                    if (m != null) {
                        m.put("watched_at", watchedMovie.getWatchedAt().toString());
                        history.add(m);
                    }
                } catch (Exception e) {
                    // bỏ qua id không hợp lệ
                }
            });

        return ResponseEntity.ok(history);
    }

    @PostMapping("/history")
    public ResponseEntity<?> addToHistory(@RequestBody java.util.Map<String, String> body, HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        String movieId = body.get("movieId");
        if (movieId == null || movieId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "movieId bắt buộc."));
        }

        User user = userOpt.get();

        // Xóa movie khỏi history nếu đã tồn tại (để thêm lại vào đầu)
        user.getWatchedHistory().removeIf(w -> w.getMovieId().equals(movieId));

        // Thêm vào đầu danh sách với thời gian hiện tại
        User.WatchedMovie watchedMovie = new User.WatchedMovie(movieId, java.time.LocalDateTime.now());
        user.getWatchedHistory().add(0, watchedMovie);

        // Giới hạn tối đa 100 phim trong lịch sử
        if (user.getWatchedHistory().size() > 100) {
            user.getWatchedHistory().subList(100, user.getWatchedHistory().size()).clear();
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đã thêm vào lịch sử xem."));
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> upsertReview(@RequestBody ReviewRequest body, HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        if (body.getMovieId() == null || body.getMovieId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "movieId bắt buộc."));
        }
        if (body.getRating() == null || body.getRating() < 0.5 || body.getRating() > 10) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rating phải nằm trong khoảng 0.5 - 10."));
        }

        User user = userOpt.get();
        User.Review existing = user.getReviews().stream()
                .filter(r -> r.getMovieId().equals(body.getMovieId()))
                .findFirst().orElse(null);

        if (existing == null) {
            existing = new User.Review();
            existing.setMovieId(body.getMovieId());
            existing.setCreatedAt(java.time.LocalDateTime.now());
            user.getReviews().add(existing);
        }

        existing.setRating(body.getRating());
        existing.setComment(body.getComment() == null ? "" : body.getComment().trim());
        existing.setUpdatedAt(java.time.LocalDateTime.now());

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đã lưu đánh giá và bình luận.",
                "review", Map.of(
                        "movieId", existing.getMovieId(),
                        "rating", existing.getRating(),
                        "comment", existing.getComment(),
                        "createdAt", existing.getCreatedAt(),
                        "updatedAt", existing.getUpdatedAt()
                )));
    }

    @GetMapping("/reviews/{movieId}")
    public ResponseEntity<?> getMyReview(@PathVariable String movieId, HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        User user = userOpt.get();
        User.Review review = user.getReviews().stream()
                .filter(r -> r.getMovieId().equals(movieId))
                .findFirst().orElse(null);

        if (review == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Chưa có đánh giá cho phim này."));
        }

        return ResponseEntity.ok(Map.of(
                "movieId", review.getMovieId(),
                "rating", review.getRating(),
                "comment", review.getComment(),
                "createdAt", review.getCreatedAt(),
                "updatedAt", review.getUpdatedAt()
        ));
    }


    @DeleteMapping("/history/{movieId}")
    public ResponseEntity<?> removeFromHistory(@PathVariable String movieId, HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        User user = userOpt.get();
        boolean removed = user.getWatchedHistory().removeIf(w -> w.getMovieId().equals(movieId));

        if (removed) {
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Đã xoá khỏi lịch sử xem."));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy phim trong lịch sử."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Đã đăng xuất thành công."));
    }

    @PostMapping("/upgrade-premium")
    public ResponseEntity<?> upgradePremium(HttpServletRequest request, HttpSession session) {
        Optional<User> userOpt = getCurrentUser(request, session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập."));
        }

        User user = userOpt.get();

        // Admin giữ nguyên
        if (user.isAdmin()) {
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(buildAuthResponse(user, token));
        }

        user.setRole(User.Role.ROLE_PREMIUM);
        userRepository.save(user);

        // Token mới để claim role cập nhật ngay
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(buildAuthResponse(user, token));
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("ok", true, "service", "auth"));
    }

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

    private AuthResponse buildAuthResponse(User user, String token) {
        AuthResponse r = new AuthResponse();
        r.setId(user.getId());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole() != null ? user.getRole().name() : User.Role.ROLE_USER.name());
        r.setToken(token); // null nếu gọi từ /me qua session
        r.setFavoriteMovieIds(user.getFavoriteMovieIds());
        return r;
    }


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

    public static class ReviewRequest {
        private String movieId;
        private Double rating;
        private String comment;

        public String getMovieId() { return movieId; }
        public void setMovieId(String movieId) { this.movieId = movieId; }

        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
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
        private java.util.List<String> favoriteMovieIds;

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

        public java.util.List<String> getFavoriteMovieIds() { return favoriteMovieIds; }
        public void setFavoriteMovieIds(java.util.List<String> favoriteMovieIds) { this.favoriteMovieIds = favoriteMovieIds; }
    }
}
