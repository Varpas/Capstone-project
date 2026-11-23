package com.bookworm.web;

import com.bookworm.dto.Dto.CartDto;
import com.bookworm.dto.Dto.CartItemRequest;
import com.bookworm.dto.Dto.CartQuantityRequest;
import com.bookworm.service.CartService;
import com.bookworm.service.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CartController {

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping("/cart")
    public CartDto get() {
        return cart.getCart(CurrentUser.requireId());
    }

    @DeleteMapping("/cart")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear() {
        cart.clear(CurrentUser.requireId());
    }

    @PostMapping("/cart/items")
    public CartDto add(@Valid @RequestBody CartItemRequest req) {
        return cart.addItem(CurrentUser.requireId(), req.bookId(), req.quantity() == null ? 1 : req.quantity());
    }

    @PutMapping("/cart/items/{bookId}")
    public CartDto setQuantity(@PathVariable Long bookId, @Valid @RequestBody CartQuantityRequest req) {
        return cart.setQuantity(CurrentUser.requireId(), bookId, req.quantity());
    }

    @DeleteMapping("/cart/items/{bookId}")
    public CartDto remove(@PathVariable Long bookId) {
        return cart.removeItem(CurrentUser.requireId(), bookId);
    }
}
