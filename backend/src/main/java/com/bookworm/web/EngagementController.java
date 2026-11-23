package com.bookworm.web;

import com.bookworm.dto.Dto.AuthorDto;
import com.bookworm.dto.Dto.BookSummary;
import com.bookworm.service.CurrentUser;
import com.bookworm.service.WishlistService;
import com.bookworm.service.WriterService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Wishlist ("My Wishlist") and followed authors ("My Writers"). */
@RestController
public class EngagementController {

    private final WishlistService wishlist;
    private final WriterService writers;

    public EngagementController(WishlistService wishlist, WriterService writers) {
        this.wishlist = wishlist;
        this.writers = writers;
    }

    @GetMapping("/wishlist")
    public List<BookSummary> wishlist() {
        return wishlist.list(CurrentUser.requireId());
    }

    @PutMapping("/wishlist/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addToWishlist(@PathVariable Long bookId) {
        wishlist.add(CurrentUser.requireId(), bookId);
    }

    @DeleteMapping("/wishlist/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWishlist(@PathVariable Long bookId) {
        wishlist.remove(CurrentUser.requireId(), bookId);
    }

    @GetMapping("/me/writers")
    public List<AuthorDto> writers() {
        return writers.list(CurrentUser.requireId());
    }

    @PutMapping("/me/writers/{authorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable Long authorId) {
        writers.follow(CurrentUser.requireId(), authorId);
    }

    @DeleteMapping("/me/writers/{authorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable Long authorId) {
        writers.unfollow(CurrentUser.requireId(), authorId);
    }
}
