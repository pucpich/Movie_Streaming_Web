package com.mivestreaming.website;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller API công khai – mọi user đều truy cập được (không cần đăng nhập).
 *
 * Base path: /api/library
 *
 * Đây là danh sách phim do Admin thêm vào hệ thống (khác với TMDB API).
 */
@RestController
@RequestMapping("/api/library")
@CrossOrigin
public class PublicMovieController {

    private final MovieRepository movieRepository;

    public PublicMovieController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * GET /api/library/movies
     * Lấy toàn bộ danh sách phim để hiển thị trang chủ.
     *
     * Query params (tuỳ chọn):
     *   ?genre=Hành+động   – lọc theo thể loại
     *   ?q=avatar          – tìm kiếm theo tên
     */
    @GetMapping("/movies")
    public ResponseEntity<List<Movie>> getMovies(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String q) {

        List<Movie> result;

        if (q != null && !q.isBlank()) {
            // Tìm kiếm theo từ khoá trong tên phim
            result = movieRepository.findByTitleContainingIgnoreCase(q.trim());
        } else if (genre != null && !genre.isBlank()) {
            // Lọc theo thể loại
            result = movieRepository.findByGenreIgnoreCase(genre.trim());
        } else {
            // Trả toàn bộ danh sách
            result = movieRepository.findAll();
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/library/movies/{id}
     * Xem chi tiết một bộ phim cụ thể.
     */
    @GetMapping("/movies/{id}")
    public ResponseEntity<?> getMovieDetail(@PathVariable String id) {
        Optional<Movie> opt = movieRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy phim."));
        }
        return ResponseEntity.ok(opt.get());
    }
}
