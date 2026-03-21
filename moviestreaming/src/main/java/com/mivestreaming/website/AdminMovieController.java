package com.mivestreaming.website;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller CRUD phim dành cho Admin.
 *
 * Base path: /api/admin/movies
 *
 * Mọi endpoint đều được bảo vệ bởi AdminGuard:
 *   - 401 nếu chưa đăng nhập (không có JWT hoặc JWT không hợp lệ)
 *   - 403 nếu đã đăng nhập nhưng không phải ROLE_ADMIN
 */
@RestController
@RequestMapping("/api/admin/movies")
@CrossOrigin
public class AdminMovieController {

    private final MovieRepository movieRepository;

    public AdminMovieController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // =====================================================================
    // C – CREATE: Thêm phim mới
    // =====================================================================

    /**
     * POST /api/admin/movies
     *
     * Body JSON:
     * {
     *   "title":       "Avengers: Endgame",
     *   "description": "...",
     *   "genre":       "Hành động",
     *   "posterUrl":   "https://...",
     *   "videoUrl":    "https://...",
     *   "rating":      8.4,
     *   "releaseYear": 2019
     * }
     */
    @PostMapping
    public ResponseEntity<?> createMovie(@RequestBody MovieRequest body,
                                         HttpServletRequest request) {
        // ---- Kiểm tra quyền Admin ----
        ResponseEntity<?> guard = AdminGuard.require(request);
        if (guard != null) return guard;

        // ---- Validate dữ liệu đầu vào ----
        if (body.getTitle() == null || body.getTitle().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Tiêu đề phim không được để trống."));
        }

        // ---- Tạo entity và lưu DB ----
        Movie movie = new Movie();
        movie.setTitle(body.getTitle().trim());
        movie.setDescription(body.getDescription());
        movie.setGenre(body.getGenre());
        movie.setPosterUrl(body.getPosterUrl());
        movie.setVideoUrl(body.getVideoUrl());
        movie.setRating(body.getRating());
        movie.setReleaseYear(body.getReleaseYear());
        movie.setCreatedAt(LocalDateTime.now());
        // Lưu ID Admin đã thêm phim (lấy từ JWT)
        movie.setCreatedByUserId((String) request.getAttribute("jwt_userId"));

        Movie saved = movieRepository.save(movie);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // =====================================================================
    // R – READ: Đọc danh sách và chi tiết
    // =====================================================================

    /**
     * GET /api/admin/movies
     * Lấy toàn bộ danh sách phim (Admin xem để quản lý).
     */
    @GetMapping
    public ResponseEntity<?> getAllMovies(HttpServletRequest request) {
        ResponseEntity<?> guard = AdminGuard.require(request);
        if (guard != null) return guard;

        List<Movie> movies = movieRepository.findAll();
        return ResponseEntity.ok(movies);
    }

    /**
     * GET /api/admin/movies/{id}
     * Lấy chi tiết một phim theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMovieById(@PathVariable String id,
                                           HttpServletRequest request) {
        ResponseEntity<?> guard = AdminGuard.require(request);
        if (guard != null) return guard;

        Optional<Movie> opt = movieRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy phim với ID: " + id));
        }
        return ResponseEntity.ok(opt.get());
    }

    // =====================================================================
    // U – UPDATE: Cập nhật thông tin phim
    // =====================================================================

    /**
     * PUT /api/admin/movies/{id}
     * Cập nhật thông tin phim (chỉ các field được gửi lên mới thay đổi).
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMovie(@PathVariable String id,
                                          @RequestBody MovieRequest body,
                                          HttpServletRequest request) {
        ResponseEntity<?> guard = AdminGuard.require(request);
        if (guard != null) return guard;

        Optional<Movie> opt = movieRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy phim với ID: " + id));
        }

        Movie movie = opt.get();

        // Cập nhật có chọn lọc: chỉ thay những field có giá trị mới
        if (body.getTitle() != null && !body.getTitle().isBlank()) {
            movie.setTitle(body.getTitle().trim());
        }
        if (body.getDescription() != null) movie.setDescription(body.getDescription());
        if (body.getGenre()       != null) movie.setGenre(body.getGenre());
        if (body.getPosterUrl()   != null) movie.setPosterUrl(body.getPosterUrl());
        if (body.getVideoUrl()    != null) movie.setVideoUrl(body.getVideoUrl());
        if (body.getRating()      != null) movie.setRating(body.getRating());
        if (body.getReleaseYear() != null) movie.setReleaseYear(body.getReleaseYear());

        movie.setUpdatedAt(LocalDateTime.now());

        Movie updated = movieRepository.save(movie);
        return ResponseEntity.ok(updated);
    }

    // =====================================================================
    // D – DELETE: Xoá phim
    // =====================================================================

    /**
     * DELETE /api/admin/movies/{id}
     * Xóa phim khỏi hệ thống.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(@PathVariable String id,
                                          HttpServletRequest request) {
        ResponseEntity<?> guard = AdminGuard.require(request);
        if (guard != null) return guard;

        if (!movieRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy phim với ID: " + id));
        }

        movieRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa phim ID " + id + " thành công."));
    }

    // =====================================================================
    // Inner class: Request body DTO
    // =====================================================================

    /** DTO nhận dữ liệu phim từ request body */
    public static class MovieRequest {
        private String  title;
        private String  description;
        private String  genre;
        private String  posterUrl;
        private String  videoUrl;
        private Double  rating;
        private Integer releaseYear;

        public String  getTitle()       { return title; }
        public void    setTitle(String title) { this.title = title; }

        public String  getDescription()  { return description; }
        public void    setDescription(String d) { this.description = d; }

        public String  getGenre()        { return genre; }
        public void    setGenre(String genre)   { this.genre = genre; }

        public String  getPosterUrl()    { return posterUrl; }
        public void    setPosterUrl(String u)   { this.posterUrl = u; }

        public String  getVideoUrl()     { return videoUrl; }
        public void    setVideoUrl(String u)    { this.videoUrl = u; }

        public Double  getRating()       { return rating; }
        public void    setRating(Double r)      { this.rating = r; }

        public Integer getReleaseYear()  { return releaseYear; }
        public void    setReleaseYear(Integer y){ this.releaseYear = y; }
    }
}
