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

    private static final String TYPE_ALL = "all";

    private String normalizeMediaType(String type) {
        if (type == null) return "movie";
        String t = type.trim().toLowerCase();
        if (t.equals(TYPE_ALL)) return "movie";
        if (t.equals("tv")) return "tv";
        return "movie";
    }

    private Integer resolveGenreId(String genreKey) {
        if (genreKey == null) return null;
        String k = genreKey.trim().toLowerCase();
        switch (k) {
            case "vien-tuong":
            case "viendtuong":
            case "vientuong":
                return 878; // Science Fiction
            case "kinh-di":
            case "kinhdi":
                return 27; // Horror
            case "sitcom":
                return 35; // Comedy
            case "chieu-rap":
            case "chieurp":
                return 18; // Drama
            case "co-trang":
            case "cotrang":
                return 36; // History
            default:
                return null;
        }
    }

    private boolean tmdbEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchTmdbResults(String endpointPath, int page, String extraQuery) {
        if (!tmdbEnabled()) return null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(apiBase).append(endpointPath)
                    .append("?api_key=").append(apiKey);

            if (page > 0) {
                sb.append("&page=").append(page);
            }
            if (extraQuery != null && !extraQuery.isBlank()) {
                if (!extraQuery.startsWith("&")) sb.append("&").append(extraQuery);
                else sb.append(extraQuery);
            }

            Map<String, Object> response = restTemplate.getForObject(sb.toString(), Map.class);
            if (response == null || !response.containsKey("results")) return null;

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null || results.isEmpty()) return null;

            for (Map<String, Object> m : results) {
                addTmdbImageUrls(m);
            }
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

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
    public List<Map<String, Object>> getTrendingMovies(String type, int page) {
        String mediaType = normalizeMediaType(type);
        List<Map<String, Object>> results = fetchTmdbResults("/trending/" + mediaType + "/week", page, null);
        return results != null ? results : getGhibliMovies();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getNowPlayingMovies(String type, int page) {
        String mediaType = normalizeMediaType(type);
        String endpoint = mediaType.equals("tv") ? "/tv/on_the_air" : "/movie/now_playing";
        List<Map<String, Object>> results = fetchTmdbResults(endpoint, page, null);
        return results != null ? results : getGhibliMovies();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> browseMovies(String q, String genreKey, String topicKey, String type, int page) {
        String mediaType = normalizeMediaType(type);

        String query = q == null ? null : q.trim();
        if (query != null && query.equalsIgnoreCase(TYPE_ALL)) query = null;

        String genre = genreKey == null ? null : genreKey.trim();
        if (genre != null && genre.equalsIgnoreCase(TYPE_ALL)) genre = null;

        String topic = topicKey == null ? null : topicKey.trim();
        if (topic != null && topic.equalsIgnoreCase(TYPE_ALL)) topic = null;

        if (query != null && !query.isBlank()) {
            String encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            List<Map<String, Object>> results = fetchTmdbResults("/search/" + mediaType, page, "query=" + encoded);
            return results != null ? results : getGhibliMovies();
        }

        Integer genreId = resolveGenreId(genre);
        Integer topicGenreId = resolveGenreId(topic);

        Integer useGenreId = genreId != null ? genreId : topicGenreId;
        if (useGenreId != null) {
            List<Map<String, Object>> results = fetchTmdbResults("/discover/" + mediaType, page, "with_genres=" + useGenreId);
            return results != null ? results : getGhibliMovies();
        }

        // Topic không map được genreId -> coi như keyword search
        if (topic != null && !topic.isBlank()) {
            String encodedTopic = java.net.URLEncoder.encode(topic, java.nio.charset.StandardCharsets.UTF_8);
            List<Map<String, Object>> results = fetchTmdbResults("/search/" + mediaType, page, "query=" + encodedTopic);
            return results != null ? results : getGhibliMovies();
        }

        List<Map<String, Object>> results = fetchTmdbResults("/" + mediaType + "/popular", page, null);
        return results != null ? results : getGhibliMovies();
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
    public Map<String, Object> getMovieDetails(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        try {
            long tmdbId = Long.parseLong(id);
            if (apiKey != null && !apiKey.isBlank()) {
                // Try movie first, then tv fallback
                try {
                    String url = apiBase + "/movie/" + tmdbId + "?api_key=" + apiKey + "&language=vi-VN&append_to_response=credits";
                    Map<String, Object> movie = restTemplate.getForObject(url, Map.class);
                    if (movie != null) {
                        // Load videos separately
                        try {
                            String videosUrl = apiBase + "/movie/" + tmdbId + "/videos?api_key=" + apiKey;
                            Map<String, Object> videosResponse = restTemplate.getForObject(videosUrl, Map.class);
                            if (videosResponse != null) {
                                movie.put("videos", videosResponse);
                            }
                        } catch (Exception ignored) {}
                        addTmdbImageUrls(movie);
                        return movie;
                    }
                } catch (Exception ignored) {}

                try {
                    String url = apiBase + "/tv/" + tmdbId + "?api_key=" + apiKey + "&language=vi-VN&append_to_response=credits";
                    Map<String, Object> tv = restTemplate.getForObject(url, Map.class);
                    if (tv != null) {
                        // Load videos separately
                        try {
                            String videosUrl = apiBase + "/tv/" + tmdbId + "/videos?api_key=" + apiKey;
                            Map<String, Object> videosResponse = restTemplate.getForObject(videosUrl, Map.class);
                            if (videosResponse != null) {
                                tv.put("videos", videosResponse);
                            }
                        } catch (Exception ignored) {}
                        addTmdbImageUrls(tv);
                        return tv;
                    }
                } catch (Exception ignored) {}
            }
        } catch (NumberFormatException ignored) {
            // id khong phai so (demo/ghibli) -> fallback tim trong data local
        } catch (Exception e) {
            e.printStackTrace();
        }

        return findMovieById(id);
    }

    private Map<String, Object> findMovieById(String id) {
        for (Map<String, Object> movie : getGhibliMovies()) {
            if (id.equals(String.valueOf(movie.get("id")))) {
                return movie;
            }
        }
        for (Map<String, Object> movie : getDemoMovies()) {
            if (id.equals(String.valueOf(movie.get("id")))) {
                return movie;
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
