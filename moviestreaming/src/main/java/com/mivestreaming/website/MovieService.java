package com.mivestreaming.website;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MovieService {

    private static final String GHIBLI_API = "https://ghibliapi.vercel.app/films";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${tmdb.api.key:}")
    private String apiKey;

    @Value("${tmdb.api.base:https://api.themoviedb.org/3}")
    private String apiBase;

    @Value("${tmdb.image.base:https://image.tmdb.org/t/p/w500}")
    private String imageBase;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTopRatedMovies(int page) {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String url = apiBase + "/movie/top_rated?api_key=" + apiKey + "&page=" + page;
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response != null && response.containsKey("results")) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                    if (results != null && !results.isEmpty()) {
                        for (Map<String, Object> m : results) {
                            addTmdbImageUrls(m);
                        }
                        return results;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getGhibliMovies();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPopularMovies(int page) {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String url = apiBase + "/movie/popular?api_key=" + apiKey + "&page=" + page;
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response != null && response.containsKey("results")) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                    if (results != null && !results.isEmpty()) {
                        for (Map<String, Object> m : results) {
                            addTmdbImageUrls(m);
                        }
                        return results;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getGhibliMovies();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchMovies(String query, int page) {
        if (query == null || query.isBlank()) {
            return getPopularMovies(page);
        }
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String url = apiBase + "/search/movie?api_key=" + apiKey + "&query="
                        + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                        + "&page=" + page;
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response != null && response.containsKey("results")) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                    if (results != null && !results.isEmpty()) {
                        for (Map<String, Object> m : results) {
                            addTmdbImageUrls(m);
                        }
                        return results;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return filterGhibliByQuery(query);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getGhibliMovies() {
        try {
            List<Map<String, Object>> raw = restTemplate.getForObject(GHIBLI_API, List.class);
            if (raw != null && !raw.isEmpty()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map<String, Object> m : raw) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", m.get("id"));
                    item.put("title", m.get("title"));
                    item.put("overview", m.get("description"));
                    item.put("release_date", m.get("release_date"));
                    String rt = String.valueOf(m.get("rt_score"));
                    try {
                        item.put("vote_average", Double.parseDouble(rt) / 10.0);
                    } catch (Exception e) {
                        item.put("vote_average", 8.0);
                    }
                    String img = (String) m.get("image");
                    item.put("poster_url", img);
                    item.put("backdrop_url", img);
                    result.add(item);
                }
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getDemoMovies();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filterGhibliByQuery(String query) {
        List<Map<String, Object>> all = getGhibliMovies();
        if (query == null || query.isBlank()) return all;
        String q = query.toLowerCase().trim();
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> m : all) {
            String title = String.valueOf(m.get("title")).toLowerCase();
            String overview = String.valueOf(m.get("overview")).toLowerCase();
            if (title.contains(q) || overview.contains(q)) {
                filtered.add(m);
            }
        }
        return filtered.isEmpty() ? all : filtered;
    }

    private List<Map<String, Object>> getDemoMovies() {
        List<Map<String, Object>> demo = new ArrayList<>();
        String[] titles = { "Còn Ra Thế Thống Gì Nữa", "Dune: Part Two", "Oppenheimer", "The Batman", "Avatar: The Way of Water" };
        String[] years = { "2026", "2024", "2023", "2022", "2022" };
        double[] ratings = { 8.5, 8.2, 8.3, 7.9, 7.6 };
        for (int i = 0; i < titles.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "demo-" + i);
            m.put("title", titles[i]);
            m.put("release_date", years[i]);
            m.put("vote_average", ratings[i]);
            m.put("overview", "Phim demo từ Liqi Phim.");
            m.put("poster_url", null);
            m.put("backdrop_url", null);
            demo.add(m);
        }
        return demo;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMovieDetails(long id) {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String url = apiBase + "/movie/" + id + "?api_key=" + apiKey + "&language=vi-VN&append_to_response=credits,videos";
                Map<String, Object> movie = restTemplate.getForObject(url, Map.class);
                if (movie != null) {
                    addTmdbImageUrls(movie);
                    return movie;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void addTmdbImageUrls(Map<String, Object> m) {
        Object poster = m.get("poster_path");
        if (poster != null && !poster.toString().isEmpty()) {
            m.put("poster_url", imageBase + poster);
        } else {
            m.put("poster_url", null);
        }
        Object backdrop = m.get("backdrop_path");
        if (backdrop != null && !backdrop.toString().isEmpty()) {
            m.put("backdrop_url", "https://image.tmdb.org/t/p/original" + backdrop);
        } else {
            m.put("backdrop_url", null);
        }
    }
}
