package com.bookworm.service;

import com.bookworm.domain.Book;
import com.bookworm.domain.Category;
import com.bookworm.dto.Dto.BookSummary;
import com.bookworm.repository.BookRepository;
import com.bookworm.repository.OrderRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final BookRepository books;
    private final OrderRepository orders;
    private final com.bookworm.repository.OrderItemRepository orderItems;
    private final BookMapper mapper;

    public RecommendationService(BookRepository books, OrderRepository orders,
                                 com.bookworm.repository.OrderItemRepository orderItems, BookMapper mapper) {
        this.books = books;
        this.orders = orders;
        this.orderItems = orderItems;
        this.mapper = mapper;
    }

    public List<BookSummary> recommendations(Long userId, int limit) {
        return mapper.toSummaries(recommend(userId, limit), userId);
    }

    /**
     * Books in the categories or by the authors of past purchases, excluding books already bought,
     * ranked by copies sold. Topped up with bestsellers; users without orders only get bestsellers.
     */
    List<Book> recommend(Long userId, int limit) {
        Collection<Long> purchasedIds = orders.findPurchasedBookIds(userId);
        Map<Long, Book> result = new LinkedHashMap<>();
        if (!purchasedIds.isEmpty()) {
            Set<Long> authorIds = new LinkedHashSet<>();
            Set<Long> categoryIds = new LinkedHashSet<>();
            for (Book b : books.findAllById(purchasedIds)) {
                authorIds.add(b.getAuthor().getId());
                for (Category c : b.getCategories()) {
                    categoryIds.add(c.getId());
                }
            }
            books.findRecommended(authorIds, categoryIds.isEmpty() ? List.of(-1L) : categoryIds, purchasedIds,
                            PageRequest.of(0, limit))
                    .forEach(b -> result.put(b.getId(), b));
        }
        if (result.size() < limit) {
            // Fall back to (and top up with) bestsellers the user has not bought yet.
            Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
            List<Long> recent = orderItems.findTopSellerIds(since, PageRequest.of(0, limit + purchasedIds.size()));
            List<Book> candidates = new ArrayList<>(books.findAllById(recent));
            candidates.sort((a, b) -> Integer.compare(recent.indexOf(a.getId()), recent.indexOf(b.getId())));
            candidates.addAll(books.findAllByOrderByCopiesSoldDescIdAsc(
                    PageRequest.of(0, limit + purchasedIds.size())));
            for (Book b : candidates) {
                if (result.size() >= limit) {
                    break;
                }
                if (!purchasedIds.contains(b.getId())) {
                    result.putIfAbsent(b.getId(), b);
                }
            }
        }
        return new ArrayList<>(result.values());
    }
}
