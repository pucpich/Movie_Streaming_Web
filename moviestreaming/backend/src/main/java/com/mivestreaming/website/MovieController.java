package com.mivestreaming.website;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;
    private final UserRepository userRepository;

    public MovieController(MovieService movieService, UserRepository userRepository) {
        this.movieService = movieService;
        this.userRepository = userRepository;
    }

    @GetMapping("/popular")
    public List<Map<String, Object>> popular(@RequestParam(defaultValue = "1") int page) {
        return movieService.getPopularMovies(page);
    }

    @GetMapping("/top-rated")
    public List<Map<String, Object>> topRated(@RequestParam(defaultValue = "1") int page) {
        return movieService.getTopRatedMovies(page);
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q,
            @RequestParam(defaultValue = "1") int page) {
        return movieService.searchMovies(q, page);
    }

    @GetMapping("/{id}")
    public Map<String, Object> details(@PathVariable String id) {
        return movieService.getMovieDetails(id);
    }

    @GetMapping("/{id}/reviews")
    public Map<String, Object> getMovieReviews(@PathVariable String id) {
        java.util.List<java.util.Map<String, Object>> reviews = new java.util.ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (user.getReviews() == null) continue;
            for (User.Review review : user.getReviews()) {
                if (id.equals(review.getMovieId())) {
                    java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
                    r.put("movieId", review.getMovieId());
                    r.put("rating", review.getRating());
                    r.put("comment", review.getComment());
                    r.put("createdAt", review.getCreatedAt());
                    r.put("updatedAt", review.getUpdatedAt());
                    r.put("userId", user.getId());
                    r.put("userName", user.getFullName());
                    reviews.add(r);
                }
            }
        }

        double avg = 0;
        int count = 0;
        for (java.util.Map<String, Object> review : reviews) {
            Object r = review.get("rating");
            if (r instanceof Number) {
                avg += ((Number) r).doubleValue();
                count++;
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("averageRating", count > 0 ? avg / count : null);
        result.put("ratingCount", count);
        result.put("reviews", reviews);
        return result;
    }

    @GetMapping("/browse")
    public List<Map<String, Object>> browse(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "1") int page) {
        return movieService.browseMovies(q, genre, topic, type, page);
    }

    @GetMapping("/now-playing")
    public List<Map<String, Object>> nowPlaying(
            @RequestParam(defaultValue = "movie") String type,
            @RequestParam(defaultValue = "1") int page) {
        return movieService.getNowPlayingMovies(type, page);
    }

    @GetMapping("/trending")
    public List<Map<String, Object>> trending(
            @RequestParam(defaultValue = "movie") String type,
            @RequestParam(defaultValue = "1") int page) {
        return movieService.getTrendingMovies(type, page);
    }
}
