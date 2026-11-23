package com.bookworm.service;

import com.bookworm.domain.Author;
import com.bookworm.domain.Book;
import com.bookworm.domain.Category;
import com.bookworm.dto.Dto.AuthorRef;
import com.bookworm.dto.Dto.BookDetail;
import com.bookworm.dto.Dto.BookSummary;
import com.bookworm.dto.Dto.CategoryDto;
import com.bookworm.dto.Dto.PublisherDto;
import com.bookworm.repository.WishlistRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Must be called inside a transaction (lazy associations are read). */
@Component
public class BookMapper {

    private final WishlistRepository wishlistRepository;

    public BookMapper(WishlistRepository wishlistRepository) {
        this.wishlistRepository = wishlistRepository;
    }

    /** {@code userId} may be null (guest): the {@code wishlisted} flag is then omitted. */
    public List<BookSummary> toSummaries(Collection<Book> books, Long userId) {
        Set<Long> wishlisted = null;
        if (userId != null && !books.isEmpty()) {
            wishlisted = wishlistRepository.findBookIds(userId, books.stream().map(Book::getId).toList());
        }
        Set<Long> ids = wishlisted;
        return books.stream().map(b -> summary(b, ids == null ? null : ids.contains(b.getId()))).toList();
    }

    public BookSummary toSummary(Book b, Long userId) {
        return toSummaries(List.of(b), userId).get(0);
    }

    public BookDetail toDetail(Book b, Long userId) {
        BookSummary s = toSummary(b, userId);
        return new BookDetail(s.id(), s.title(), s.author(), s.description(), s.format(), s.categories(), s.price(),
                s.coverImageUrl(), s.averageRating(), s.inStock(), s.estimatedDeliveryDate(), s.wishlisted(),
                b.getIsbn(), b.getLanguage(),
                b.getPublisher() == null ? null : new PublisherDto(b.getPublisher().getId(), b.getPublisher().getName()),
                b.getBackCoverImageUrl(), b.getPublishedDate(), b.getCopiesSold(), b.getReviewCount(), b.getStock());
    }

    private BookSummary summary(Book b, Boolean wishlisted) {
        Author a = b.getAuthor();
        List<CategoryDto> categories = b.getCategories().stream()
                .sorted(Comparator.comparing(Category::getId))
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();
        return new BookSummary(b.getId(), b.getTitle(), new AuthorRef(a.getId(), a.getName()), b.getDescription(),
                b.getFormat(), categories, Money.round(b.getPrice()), b.getCoverImageUrl(), b.getAverageRating(),
                b.isInStock(), b.isInStock() ? b.estimatedDeliveryDate() : null, wishlisted);
    }
}
