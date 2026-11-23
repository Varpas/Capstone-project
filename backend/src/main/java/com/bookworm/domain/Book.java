package com.bookworm.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String isbn;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Author author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookFormat format;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(name = "delivery_days", nullable = false)
    private int deliveryDays;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "back_cover_image_url")
    private String backCoverImageUrl;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "copies_sold", nullable = false)
    private int copiesSold;

    @Column(name = "average_rating", nullable = false)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @ManyToMany
    @org.hibernate.annotations.BatchSize(size = 50)
    @JoinTable(name = "book_categories",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new LinkedHashSet<>();

    /** eBooks have unlimited availability. */
    public boolean isEbook() {
        return format == BookFormat.EBOOK;
    }

    public boolean isInStock() {
        return isEbook() || stock > 0;
    }

    public int availableQuantity() {
        return isEbook() ? Integer.MAX_VALUE : stock;
    }

    public LocalDate estimatedDeliveryDate() {
        return LocalDate.now().plusDays(deliveryDays);
    }
}
