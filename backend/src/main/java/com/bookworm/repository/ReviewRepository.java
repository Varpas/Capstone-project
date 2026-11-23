package com.bookworm.repository;

import com.bookworm.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query(value = "select r from Review r join fetch r.user where r.book.id = :bookId",
            countQuery = "select count(r) from Review r where r.book.id = :bookId")
    Page<Review> findByBook(@Param("bookId") Long bookId, Pageable pageable);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.book.id = :bookId")
    double averageRating(@Param("bookId") Long bookId);

    long countByBookId(Long bookId);
}
