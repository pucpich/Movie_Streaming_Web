package com.mivestreaming.website;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho Movie entity (MongoDB).
 * Spring Data MongoDB sẽ thiết lập các method truy vấn.
 */
@Repository
public interface MovieRepository extends MongoRepository<Movie, String> {

    /** Tìm phim theo thể loại (không phân biệt hoa/thường) */
    List<Movie> findByGenreIgnoreCase(String genre);

    /** Tìm phim theo tiêu đề (tìm kiếm gần đúng, không phân biệt hoa/thường) */
    List<Movie> findByTitleContainingIgnoreCase(String keyword);
}
