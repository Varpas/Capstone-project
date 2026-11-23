package com.bookworm.service;

import com.bookworm.domain.Book;
import com.bookworm.domain.Cart;
import com.bookworm.domain.CartItem;
import com.bookworm.domain.User;
import com.bookworm.dto.Dto.BookSummary;
import com.bookworm.dto.Dto.CartDto;
import com.bookworm.dto.Dto.CartItemDto;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.BookRepository;
import com.bookworm.repository.CartRepository;
import com.bookworm.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final BookRepository books;
    private final UserRepository users;
    private final BookMapper mapper;

    public CartService(CartRepository carts, BookRepository books, UserRepository users, BookMapper mapper) {
        this.carts = carts;
        this.books = books;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CartDto getCart(Long userId) {
        return carts.findByUserId(userId).map(c -> toDto(c, userId)).orElseGet(() -> toDto(null, userId));
    }

    public CartDto addItem(Long userId, Long bookId, int quantity) {
        Book book = books.findById(bookId).orElseThrow(() -> ApiException.notFound("Book not found"));
        Cart cart = getOrCreate(userId);
        CartItem item = findItem(cart, bookId);
        int newQuantity = (item == null ? 0 : item.getQuantity()) + quantity;
        requireStock(book, newQuantity);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setBook(book);
            cart.getItems().add(item);
        }
        item.setQuantity(newQuantity);
        carts.saveAndFlush(cart);
        return toDto(cart, userId);
    }

    public CartDto setQuantity(Long userId, Long bookId, int quantity) {
        Cart cart = carts.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Book is not in the cart"));
        CartItem item = findItem(cart, bookId);
        if (item == null) {
            throw ApiException.notFound("Book is not in the cart");
        }
        requireStock(item.getBook(), quantity);
        item.setQuantity(quantity);
        carts.saveAndFlush(cart);
        return toDto(cart, userId);
    }

    public CartDto removeItem(Long userId, Long bookId) {
        Cart cart = carts.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Book is not in the cart"));
        if (!cart.getItems().removeIf(i -> i.getBook().getId().equals(bookId))) {
            throw ApiException.notFound("Book is not in the cart");
        }
        carts.saveAndFlush(cart);
        return toDto(cart, userId);
    }

    public void clear(Long userId) {
        carts.findByUserId(userId).ifPresent(c -> {
            c.getItems().clear();
            carts.saveAndFlush(c);
        });
    }

    /** Quantity of the book already in the cart (0 when absent). */
    @Transactional(readOnly = true)
    public int quantityInCart(Long userId, Long bookId) {
        return carts.findByUserId(userId).map(c -> findItem(c, bookId)).map(CartItem::getQuantity).orElse(0);
    }

    public CartDto toDto(Cart cart, Long userId) {
        List<CartItem> items = cart == null ? List.of() : cart.getItems();
        Map<Long, BookSummary> summaries = mapper.toSummaries(items.stream().map(CartItem::getBook).toList(), userId)
                .stream().collect(Collectors.toMap(BookSummary::id, Function.identity()));
        List<CartItemDto> lines = items.stream().map(i -> new CartItemDto(
                summaries.get(i.getBook().getId()), i.getQuantity(),
                Money.round(i.getBook().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))).toList();
        BigDecimal subtotal = lines.stream().map(CartItemDto::lineTotal).reduce(Money.zero(), BigDecimal::add);
        return new CartDto(lines, lines.stream().mapToInt(CartItemDto::quantity).sum(), subtotal);
    }

    private Cart getOrCreate(Long userId) {
        return carts.findByUserId(userId).orElseGet(() -> {
            User user = users.getReferenceById(userId);
            return carts.save(new Cart(user));
        });
    }

    private static CartItem findItem(Cart cart, Long bookId) {
        return cart.getItems().stream().filter(i -> i.getBook().getId().equals(bookId)).findFirst().orElse(null);
    }

    private static void requireStock(Book book, int quantity) {
        if (quantity > book.availableQuantity()) {
            throw ApiException.conflict(book.getStock() == 0
                    ? "'" + book.getTitle() + "' is out of stock"
                    : "Only " + book.getStock() + " copies of '" + book.getTitle() + "' are in stock");
        }
    }
}
