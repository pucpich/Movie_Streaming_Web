package com.mivestreaming.website;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "users")
public class User {

    public enum Role {
        ROLE_USER,
        ROLE_PREMIUM,
        ROLE_ADMIN
    }

    @Id
    private String id;

    @Field("full_name")
    private String fullName;

    @Field("email")
    private String email;

    @Field("password")
    private String password;

    /** Vai trò của user, mặc định là ROLE_USER */
    @Field("role")
    private Role role = Role.ROLE_USER;

    @Field("favorite_movie_ids")
    private java.util.List<String> favoriteMovieIds = new java.util.ArrayList<>();

    @Field("watched_history")
    private java.util.List<WatchedMovie> watchedHistory = new java.util.ArrayList<>();

    @Field("reviews")
    private java.util.List<Review> reviews = new java.util.ArrayList<>();


    public static class WatchedMovie {
        @Field("movie_id")
        private String movieId;

        @Field("watched_at")
        private java.time.LocalDateTime watchedAt;

        public WatchedMovie() {}

        public WatchedMovie(String movieId, java.time.LocalDateTime watchedAt) {
            this.movieId = movieId;
            this.watchedAt = watchedAt;
        }

        public String getMovieId() { return movieId; }
        public void setMovieId(String movieId) { this.movieId = movieId; }

        public java.time.LocalDateTime getWatchedAt() { return watchedAt; }
        public void setWatchedAt(java.time.LocalDateTime watchedAt) { this.watchedAt = watchedAt; }
    }
    public static class Review {
        @Field("movie_id")
        private String movieId;

        @Field("rating")
        private Double rating;

        @Field("comment")
        private String comment;

        @Field("created_at")
        private java.time.LocalDateTime createdAt;

        @Field("updated_at")
        private java.time.LocalDateTime updatedAt;

        public String getMovieId() { return movieId; }
        public void setMovieId(String movieId) { this.movieId = movieId; }

        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public java.util.List<String> getFavoriteMovieIds() { return favoriteMovieIds; }
    public void setFavoriteMovieIds(java.util.List<String> favoriteMovieIds) { this.favoriteMovieIds = favoriteMovieIds; }

    public java.util.List<WatchedMovie> getWatchedHistory() { return watchedHistory; }
    public void setWatchedHistory(java.util.List<WatchedMovie> watchedHistory) { this.watchedHistory = watchedHistory; }

    public java.util.List<Review> getReviews() { return reviews; }
    public void setReviews(java.util.List<Review> reviews) { this.reviews = reviews; }

    // Tiện ích kiểm tra nhanh
    public boolean isAdmin() {
        return Role.ROLE_ADMIN.equals(this.role);
    }

    public boolean isPremium() {
        return Role.ROLE_PREMIUM.equals(this.role) || isAdmin();
    }
}
