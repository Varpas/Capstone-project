package com.bookworm.web;

import com.bookworm.domain.OrderStatus;
import com.bookworm.dto.Dto.BookSummary;
import com.bookworm.dto.Dto.GiftPointsDto;
import com.bookworm.dto.Dto.OrderDto;
import com.bookworm.dto.Dto.PageResponse;
import com.bookworm.dto.Dto.PaymentRequest;
import com.bookworm.dto.Dto.PaymentResult;
import com.bookworm.dto.Dto.PlaceOrderRequest;
import com.bookworm.dto.Dto.PriceBreakdown;
import com.bookworm.dto.Dto.PricingRequest;
import com.bookworm.dto.Dto.ReorderResult;
import com.bookworm.service.CurrentUser;
import com.bookworm.service.GiftPointService;
import com.bookworm.service.OrderService;
import com.bookworm.service.RecommendationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Checkout, orders, payment, recommendations and gift points. */
@RestController
@Validated
public class OrderController {

    private final OrderService orders;
    private final RecommendationService recommendations;
    private final GiftPointService giftPoints;

    public OrderController(OrderService orders, RecommendationService recommendations, GiftPointService giftPoints) {
        this.orders = orders;
        this.recommendations = recommendations;
        this.giftPoints = giftPoints;
    }

    @GetMapping("/recommendations")
    public List<BookSummary> recommendations(@RequestParam(defaultValue = "6") @Min(1) @Max(50) int limit) {
        return recommendations.recommendations(CurrentUser.requireId(), limit);
    }

    @PostMapping("/checkout/quote")
    public PriceBreakdown quote(@Valid @RequestBody PricingRequest req) {
        return orders.quote(CurrentUser.requireId(), req);
    }

    @GetMapping("/orders")
    public PageResponse<OrderDto> list(@RequestParam(required = false) OrderStatus status,
                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orders.list(CurrentUser.requireId(), status, page, size);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto place(@Valid @RequestBody PlaceOrderRequest req) {
        return orders.place(CurrentUser.requireId(), req);
    }

    @GetMapping("/orders/{orderId}")
    public OrderDto get(@PathVariable Long orderId) {
        return orders.get(CurrentUser.requireId(), orderId);
    }

    @PostMapping("/orders/{orderId}/payment")
    public PaymentResult pay(@PathVariable Long orderId, @Valid @RequestBody PaymentRequest req) {
        return orders.pay(CurrentUser.requireId(), orderId, req);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public OrderDto cancel(@PathVariable Long orderId) {
        return orders.cancel(CurrentUser.requireId(), orderId);
    }

    @PostMapping("/orders/{orderId}/reorder")
    public ReorderResult reorder(@PathVariable Long orderId) {
        return orders.reorder(CurrentUser.requireId(), orderId);
    }

    @GetMapping("/me/gift-points")
    public GiftPointsDto giftPoints(@RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return giftPoints.get(CurrentUser.requireId(), page, size);
    }
}
