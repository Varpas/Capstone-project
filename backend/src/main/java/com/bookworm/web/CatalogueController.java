package com.bookworm.web;

import com.bookworm.domain.BookFormat;
import com.bookworm.dto.Dto.AuthorDto;
import com.bookworm.dto.Dto.BookDetail;
import com.bookworm.dto.Dto.BookSummary;
import com.bookworm.dto.Dto.CategoryDto;
import com.bookworm.dto.Dto.HomeResponse;
import com.bookworm.dto.Dto.PageResponse;
import com.bookworm.dto.Dto.PublisherDto;
import com.bookworm.service.CatalogueService;
import com.bookworm.service.CatalogueService.BookQuery;
import com.bookworm.service.CurrentUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class CatalogueController {

    private final CatalogueService catalogue;

    public CatalogueController(CatalogueService catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping("/home")
    public HomeResponse home(@RequestParam(defaultValue = "6") @Min(1) @Max(20) int limit) {
        return catalogue.home(limit, CurrentUser.id().orElse(null));
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalogue.listCategories();
    }

    @GetMapping("/publishers")
    public List<PublisherDto> publishers() {
        return catalogue.listPublishers();
    }

    @GetMapping("/books")
    public PageResponse<BookSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) BookFormat format,
            @RequestParam(required = false) @Min(0) BigDecimal minPrice,
            @RequestParam(required = false) @Min(0) BigDecimal maxPrice,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return catalogue.search(new BookQuery(q, categoryId, publisherId, authorId, language, format, minPrice,
                maxPrice, sort), page, size, CurrentUser.id().orElse(null));
    }

    @GetMapping("/books/{bookId}")
    public BookDetail book(@PathVariable Long bookId) {
        return catalogue.getBook(bookId, CurrentUser.id().orElse(null));
    }

    @GetMapping("/books/{bookId}/related")
    public List<BookSummary> related(@PathVariable Long bookId,
                                     @RequestParam(defaultValue = "3") @Min(1) @Max(20) int limit) {
        return catalogue.related(bookId, limit, CurrentUser.id().orElse(null));
    }

    @GetMapping("/authors/{authorId}")
    public AuthorDto author(@PathVariable Long authorId) {
        return catalogue.getAuthor(authorId, CurrentUser.id().orElse(null));
    }
}
