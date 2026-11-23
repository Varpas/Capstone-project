package com.bookworm.service;

import com.bookworm.domain.WishlistItem;
import com.bookworm.dto.Dto.BookSummary;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.BookRepository;
import com.bookworm.repository.UserRepository;
import com.bookworm.repository.WishlistRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlist;
    private final BookRepository books;
    private final UserRepository users;
    private final BookMapper mapper;

    public WishlistService(WishlistRepository wishlist, BookRepository books, UserRepository users,
                           BookMapper mapper) {
        this.wishlist = wishlist;
        this.books = books;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<BookSummary> list(Long userId) {
        return mapper.toSummaries(wishlist.findByUser(userId).stream().map(WishlistItem::getBook).toList(), userId);
    }

    public void add(Long userId, Long bookId) {
        var book = books.findById(bookId).orElseThrow(() -> ApiException.notFound("Book not found"));
        if (!wishlist.existsByUserIdAndBookId(userId, bookId)) {
            WishlistItem item = new WishlistItem();
            item.setUser(users.getReferenceById(userId));
            item.setBook(book);
            wishlist.save(item);
        }
    }

    public void remove(Long userId, Long bookId) {
        wishlist.deleteByUserAndBook(userId, bookId);
    }
}
