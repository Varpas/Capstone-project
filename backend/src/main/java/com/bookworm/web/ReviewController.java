package com.bookworm.web;

import com.bookworm.dto.Dto.PageResponse;
import com.bookworm.dto.Dto.ReviewDto;
import com.bookworm.dto.Dto.ReviewRequest;
import com.bookworm.service.CurrentUser;
import com.bookworm.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class ReviewController {

    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/books/{bookId}/reviews")
    public PageResponse<ReviewDto> list(@PathVariable Long bookId,
                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviews.list(bookId, page, size);
    }

    @PostMapping("/books/{bookId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto create(@PathVariable Long bookId, @Valid @RequestBody ReviewRequest req) {
        return reviews.create(CurrentUser.requireId(), bookId, req);
    }
}
