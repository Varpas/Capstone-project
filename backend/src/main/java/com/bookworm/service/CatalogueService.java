package com.bookworm.service;

import com.bookworm.domain.Author;
import com.bookworm.domain.Book;
import com.bookworm.domain.BookFormat;
import com.bookworm.dto.Dto.AuthorDto;
import com.bookworm.dto.Dto.BookDetail;
import com.bookworm.dto.Dto.BookSummary;
import com.bookworm.dto.Dto.CategoryDto;
import com.bookworm.dto.Dto.HomeResponse;
import com.bookworm.dto.Dto.PageResponse;
import com.bookworm.dto.Dto.PublisherDto;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.AuthorRepository;
import com.bookworm.repository.BookRepository;
import com.bookworm.repository.CategoryRepository;
import com.bookworm.repository.FollowedAuthorRepository;
import com.bookworm.repository.OrderItemRepository;
import com.bookworm.repository.PublisherRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogueService {

    private final BookRepository books;
    private final AuthorRepository authors;
    private final CategoryRepository categories;
    private final PublisherRepository publishers;
    private final FollowedAuthorRepository followed;
    private final OrderItemRepository orderItems;
    private final BookMapper mapper;
    private final RecommendationService recommendations;

    public CatalogueService(BookRepository books, AuthorRepository authors, CategoryRepository categories,
                            PublisherRepository publishers, FollowedAuthorRepository followed,
                            OrderItemRepository orderItems, BookMapper mapper,
                            RecommendationService recommendations) {
        this.books = books;
        this.authors = authors;
        this.categories = categories;
        this.publishers = publishers;
        this.followed = followed;
        this.orderItems = orderItems;
        this.mapper = mapper;
        this.recommendations = recommendations;
    }

    public List<CategoryDto> listCategories() {
        return categories.findAllByOrderByNameAsc().stream().map(c -> new CategoryDto(c.getId(), c.getName())).toList();
    }

    public List<PublisherDto> listPublishers() {
        return publishers.findAllByOrderByNameAsc().stream().map(p -> new PublisherDto(p.getId(), p.getName()))
                .toList();
    }

    public HomeResponse home(int limit, Long userId) {
        List<Book> recommended = userId == null ? bestsellers(limit) : recommendations.recommend(userId, limit);
        return new HomeResponse(
                mapper.toSummaries(recommended, userId),
                mapper.toSummaries(bestsellers(limit), userId),
                mapper.toSummaries(books.findAllByOrderByPublishedDateDescIdDesc(PageRequest.of(0, limit)), userId));
    }

    /**
     * Top sellers of the last 30 days. When fewer than {@code limit} books sold in that period the list is
     * topped up with all-time best sellers, so the shelf is never empty on a fresh catalogue.
     */
    List<Book> bestsellers(int limit) {
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Long> ids = orderItems.findTopSellerIds(since, PageRequest.of(0, limit));
        Map<Long, Book> result = new LinkedHashMap<>();
        Map<Long, Book> found = new LinkedHashMap<>();
        books.findAllById(ids).forEach(b -> found.put(b.getId(), b));
        ids.forEach(id -> result.put(id, found.get(id)));
        if (result.size() < limit) {
            for (Book b : books.findAllByOrderByCopiesSoldDescIdAsc(PageRequest.of(0, limit))) {
                if (result.size() >= limit) {
                    break;
                }
                result.putIfAbsent(b.getId(), b);
            }
        }
        return new ArrayList<>(result.values());
    }

    public PageResponse<BookSummary> search(BookQuery q, int page, int size, Long userId) {
        if (q.minPrice() != null && q.maxPrice() != null && q.minPrice().compareTo(q.maxPrice()) > 0) {
            throw ApiException.badRequest("minPrice must not be greater than maxPrice");
        }
        Pageable pageable = PageRequest.of(page, size, sortFor(q.sort()));
        var result = books.findAll(specification(q), pageable);
        List<BookSummary> content = mapper.toSummaries(result.getContent(), userId);
        return new PageResponse<>(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), content);
    }

    public BookDetail getBook(Long id, Long userId) {
        return mapper.toDetail(find(id), userId);
    }

    public List<BookSummary> related(Long id, int limit, Long userId) {
        Book book = find(id);
        List<Long> categoryIds = book.getCategories().stream().map(c -> c.getId()).toList();
        List<Book> related = books.findRelated(id, book.getAuthor().getId(),
                categoryIds.isEmpty() ? List.of(-1L) : categoryIds, PageRequest.of(0, limit));
        return mapper.toSummaries(related, userId);
    }

    public AuthorDto getAuthor(Long id, Long userId) {
        Author a = authors.findById(id).orElseThrow(() -> ApiException.notFound("Author not found"));
        Boolean isFollowed = userId == null ? null : followed.existsByUserIdAndAuthorId(userId, id);
        return new AuthorDto(a.getId(), a.getName(), a.getBio(), a.getPhotoUrl(), isFollowed);
    }

    private Book find(Long id) {
        return books.findById(id).orElseThrow(() -> ApiException.notFound("Book not found"));
    }

    private static Sort sortFor(String sort) {
        Sort byId = Sort.by(Sort.Direction.ASC, "id");
        return switch (sort == null ? "relevance" : sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price").and(byId);
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price").and(byId);
            case "newest" -> Sort.by(Sort.Direction.DESC, "publishedDate").and(byId);
            case "rating" -> Sort.by(Sort.Direction.DESC, "averageRating", "reviewCount").and(byId);
            case "bestselling", "relevance" -> Sort.by(Sort.Direction.DESC, "copiesSold").and(byId);
            default -> throw ApiException.badRequest("Unknown sort: " + sort);
        };
    }

    private static Specification<Book> specification(BookQuery q) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q.q() != null && !q.q().isBlank()) {
                String pattern = "%" + escapeLike(q.q().trim().toLowerCase()) + "%";
                Join<Book, Author> author = root.join("author", JoinType.LEFT);
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(author.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\')));
            }
            if (q.categoryId() != null) {
                ps.add(cb.equal(root.join("categories", JoinType.INNER).get("id"), q.categoryId()));
            }
            if (q.publisherId() != null) {
                ps.add(cb.equal(root.get("publisher").get("id"), q.publisherId()));
            }
            if (q.authorId() != null) {
                ps.add(cb.equal(root.get("author").get("id"), q.authorId()));
            }
            if (q.language() != null && !q.language().isBlank()) {
                ps.add(cb.equal(cb.lower(root.get("language")), q.language().trim().toLowerCase()));
            }
            if (q.format() != null) {
                ps.add(cb.equal(root.get("format"), q.format()));
            }
            if (q.minPrice() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("price"), q.minPrice()));
            }
            if (q.maxPrice() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("price"), q.maxPrice()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public record BookQuery(String q, Long categoryId, Long publisherId, Long authorId, String language,
                            BookFormat format, BigDecimal minPrice, BigDecimal maxPrice, String sort) {}
}
