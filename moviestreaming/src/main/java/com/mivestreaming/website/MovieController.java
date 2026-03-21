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

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
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
    public Map<String, Object> details(@PathVariable long id) {
        return movieService.getMovieDetails(id);
    }
}
