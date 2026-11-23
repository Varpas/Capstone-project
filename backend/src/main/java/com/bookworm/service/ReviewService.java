package com.bookworm.service;

import com.bookworm.domain.Book;
import com.bookworm.domain.Review;
import com.bookworm.dto.Dto.PageResponse;
import com.bookworm.dto.Dto.ReviewDto;
import com.bookworm.dto.Dto.ReviewRequest;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.BookRepository;
import com.bookworm.repository.ReviewRepository;
import com.bookworm.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviews;
    private final BookRepository books;
    private final UserRepository users;

    public ReviewService(ReviewRepository reviews, BookRepository books, UserRepository users) {
        this.reviews = reviews;
        this.books = books;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> list(Long bookId, int page, int size) {
        if (!books.existsById(bookId)) {
            throw ApiException.notFound("Book not found");
        }
        var result = reviews.findByBook(bookId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return PageResponse.of(result.map(ReviewService::toDto));
    }

    public ReviewDto create(Long userId, Long bookId, ReviewRequest req) {
        // Locking the book serialises concurrent reviews, so the derived rating stays exact.
        Book book = books.lockAllById(List.of(bookId)).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Book not found"));
        if (reviews.existsByUserIdAndBookId(userId, bookId)) {
            throw ApiException.conflict("You have already reviewed this book");
        }
        Review review = new Review();
        review.setUser(users.getReferenceById(userId));
        review.setBook(book);
        review.setRating(req.rating());
        review.setComment(req.comment() == null || req.comment().isBlank() ? null : req.comment().trim());
        reviews.saveAndFlush(review);

        book.setReviewCount((int) reviews.countByBookId(bookId));
        book.setAverageRating(BigDecimal.valueOf(reviews.averageRating(bookId)).setScale(2, RoundingMode.HALF_UP));
        return toDto(review);
    }

    private static ReviewDto toDto(Review r) {
        return new ReviewDto(r.getId(), r.getBook().getId(), r.getUser().getFullName(), r.getRating(),
                r.getComment(), r.getCreatedAt());
    }
}
