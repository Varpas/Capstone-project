package com.bookworm.service;

import com.bookworm.config.AppProperties;
import com.bookworm.domain.Address;
import com.bookworm.domain.Book;
import com.bookworm.domain.Cart;
import com.bookworm.domain.CartItem;
import com.bookworm.domain.Order;
import com.bookworm.domain.OrderItem;
import com.bookworm.domain.OrderStatus;
import com.bookworm.domain.Payment;
import com.bookworm.domain.PaymentStatus;
import com.bookworm.domain.PointTxnType;
import com.bookworm.domain.User;
import com.bookworm.dto.Dto.AddressRequest;
import com.bookworm.dto.Dto.OrderDto;
import com.bookworm.dto.Dto.OrderItemDto;
import com.bookworm.dto.Dto.PageResponse;
import com.bookworm.dto.Dto.PaymentRequest;
import com.bookworm.dto.Dto.PaymentResult;
import com.bookworm.dto.Dto.PlaceOrderRequest;
import com.bookworm.dto.Dto.PriceBreakdown;
import com.bookworm.dto.Dto.PricingRequest;
import com.bookworm.dto.Dto.ReorderResult;
import com.bookworm.dto.Dto.ShippingAddress;
import com.bookworm.dto.Dto.SkippedItem;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.BookRepository;
import com.bookworm.repository.CartRepository;
import com.bookworm.repository.OrderRepository;
import com.bookworm.repository.PaymentRepository;
import com.bookworm.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orders;
    private final CartRepository carts;
    private final BookRepository books;
    private final UserRepository users;
    private final PaymentRepository payments;
    private final PricingService pricing;
    private final GiftPointService points;
    private final AddressService addresses;
    private final CartService cartService;
    private final PaymentSimulator gateway;
    private final Duration cancelWindow;

    public OrderService(OrderRepository orders, CartRepository carts, BookRepository books, UserRepository users,
                        PaymentRepository payments, PricingService pricing, GiftPointService points,
                        AddressService addresses, CartService cartService, PaymentSimulator gateway,
                        AppProperties props) {
        this.orders = orders;
        this.carts = carts;
        this.books = books;
        this.users = users;
        this.payments = payments;
        this.pricing = pricing;
        this.points = points;
        this.addresses = addresses;
        this.cartService = cartService;
        this.gateway = gateway;
        this.cancelWindow = Duration.ofHours(props.orders().cancelWindowHours());
    }

    // ---------------------------------------------------------------- quote

    @Transactional(readOnly = true)
    public PriceBreakdown quote(Long userId, PricingRequest req) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Unknown user"));
        Cart cart = requireNonEmptyCart(userId);
        requireStock(cart);
        return priceCart(cart, req.couponCode(), req.pointsToRedeem(), user).breakdown();
    }

    // ---------------------------------------------------------------- place

    public OrderDto place(Long userId, PlaceOrderRequest req) {
        if ((req.addressId() == null) == (req.address() == null)) {
            throw ApiException.badRequest("Provide exactly one of addressId or address");
        }
        // Lock the user: serialises concurrent checkouts spending the same gift points.
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> ApiException.unauthorized("Unknown user"));
        Cart cart = requireNonEmptyCart(userId);
        requireStock(cart);

        Address saved = req.addressId() != null ? addresses.require(userId, req.addressId()) : null;
        var priced = priceCart(cart, req.couponCode(), req.pointsToRedeem(), user);
        PriceBreakdown price = priced.breakdown();

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        if (saved != null) {
            snapshot(order, saved);
        } else {
            snapshot(order, req.address());
            if (Boolean.TRUE.equals(req.saveAddress())) {
                addresses.create(userId, req.address());
            }
        }
        order.setSubtotal(price.subtotal());
        order.setTax(price.tax());
        order.setDeliveryCharge(price.deliveryCharge());
        order.setCoupon(priced.coupon());
        order.setCouponDiscount(price.couponDiscount());
        order.setPointsRedeemed(price.pointsRedeemed());
        order.setPointsDiscount(price.pointsDiscount());
        order.setTotal(price.total());
        for (CartItem ci : cart.getItems()) {
            Book b = ci.getBook();
            OrderItem oi = new OrderItem();
            oi.setBook(b);
            oi.setTitle(b.getTitle());
            oi.setAuthorName(b.getAuthor().getName());
            oi.setCoverImageUrl(b.getCoverImageUrl());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(Money.round(b.getPrice()));
            oi.setEstimatedDeliveryDate(b.estimatedDeliveryDate());
            order.addItem(oi);
        }
        orders.saveAndFlush(order);
        // Points are reserved now and restored if the order is cancelled or the payment fails.
        if (price.pointsRedeemed() > 0) {
            points.apply(user, order, -price.pointsRedeemed(), PointTxnType.REDEEM);
        }
        return toDto(order);
    }

    // -------------------------------------------------------------- payment

    public PaymentResult pay(Long userId, Long orderId, PaymentRequest req) {
        Order order = lockOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("Order is not awaiting payment (status " + order.getStatus() + ")");
        }
        gateway.validate(req);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(req.method());
        payment.setAmount(order.getTotal());

        var outcome = gateway.charge(req);
        payment.setCardLast4(outcome.cardLast4());
        payment.setTransactionRef(outcome.transactionRef());

        String failure = outcome.success() ? null : outcome.failureReason();
        List<Book> locked = null;
        if (outcome.success()) {
            locked = books.lockAllById(order.getItems().stream().map(i -> i.getBook().getId()).distinct().toList());
            failure = firstShortage(order, locked);
        }

        User user = users.findByIdForUpdate(userId).orElseThrow();
        if (failure == null) {
            Map<Long, Book> byId = locked.stream().collect(Collectors.toMap(Book::getId, Function.identity()));
            for (OrderItem i : order.getItems()) {
                Book b = byId.get(i.getBook().getId());
                if (!b.isEbook()) {
                    b.setStock(b.getStock() - i.getQuantity());
                }
                b.setCopiesSold(b.getCopiesSold() + i.getQuantity());
            }
            payment.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PAID);
            int earned = pricing.pointsToEarn(order.getTotal());
            order.setPointsEarned(earned);
            if (earned > 0) {
                points.apply(user, order, earned, PointTxnType.EARN);
            }
            cartService.clear(userId);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failure);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            if (order.getPointsRedeemed() > 0) {
                points.apply(user, order, order.getPointsRedeemed(), PointTxnType.REFUND);
            }
        }
        payments.save(payment);
        order.getPayments().add(payment);
        return toDto(payment);
    }

    // --------------------------------------------------------------- cancel

    public OrderDto cancel(Long userId, Long orderId) {
        Order order = lockOrder(userId, orderId);
        Instant now = Instant.now();
        switch (order.getStatus()) {
            case PENDING_PAYMENT -> {
                User user = users.findByIdForUpdate(userId).orElseThrow();
                if (order.getPointsRedeemed() > 0) {
                    points.apply(user, order, order.getPointsRedeemed(), PointTxnType.REFUND);
                }
            }
            case PAID -> {
                if (now.isAfter(order.getCreatedAt().plus(cancelWindow))) {
                    throw ApiException.conflict("Orders can only be cancelled within "
                            + cancelWindow.toHours() + " hours of placing them");
                }
                List<Book> locked = books.lockAllById(
                        order.getItems().stream().map(i -> i.getBook().getId()).distinct().toList());
                Map<Long, Book> byId = locked.stream().collect(Collectors.toMap(Book::getId, Function.identity()));
                for (OrderItem i : order.getItems()) {
                    Book b = byId.get(i.getBook().getId());
                    if (!b.isEbook()) {
                        b.setStock(b.getStock() + i.getQuantity());
                    }
                    b.setCopiesSold(Math.max(0, b.getCopiesSold() - i.getQuantity()));
                }
                order.getPayments().stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                        .forEach(p -> p.setStatus(PaymentStatus.REFUNDED));
                User user = users.findByIdForUpdate(userId).orElseThrow();
                if (order.getPointsRedeemed() > 0) {
                    points.apply(user, order, order.getPointsRedeemed(), PointTxnType.REFUND);
                }
                if (order.getPointsEarned() > 0) {
                    points.apply(user, order, -order.getPointsEarned(), PointTxnType.REFUND);
                }
            }
            default -> throw ApiException.conflict("Order cannot be cancelled (status " + order.getStatus() + ")");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        return toDto(order);
    }

    // -------------------------------------------------------------- reorder

    public ReorderResult reorder(Long userId, Long orderId) {
        Order order = orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        List<SkippedItem> skipped = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Book book = books.findById(item.getBook().getId()).orElseThrow();
            int inCart = cartService.quantityInCart(userId, book.getId());
            int room = book.availableQuantity() == Integer.MAX_VALUE
                    ? item.getQuantity() : book.availableQuantity() - inCart;
            int toAdd = Math.min(item.getQuantity(), room);
            if (toAdd <= 0) {
                skipped.add(new SkippedItem(book.getId(), book.isInStock() ? "Already at maximum stock in cart"
                        : "Out of stock"));
                continue;
            }
            cartService.addItem(userId, book.getId(), toAdd);
            if (toAdd < item.getQuantity()) {
                skipped.add(new SkippedItem(book.getId(), "Only " + toAdd + " of " + item.getQuantity() + " added: limited stock"));
            }
        }
        return new ReorderResult(cartService.getCart(userId), skipped);
    }

    // ---------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> list(Long userId, OrderStatus status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = status == null ? orders.findByUserId(userId, pr) : orders.findByUserIdAndStatus(userId, status, pr);
        return PageResponse.of(result.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long userId, Long orderId) {
        return toDto(orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("Order not found")));
    }

    // -------------------------------------------------------------- helpers

    private Order lockOrder(Long userId, Long orderId) {
        return orders.findForUpdate(orderId, userId).orElseThrow(() -> ApiException.notFound("Order not found"));
    }

    private Cart requireNonEmptyCart(Long userId) {
        return carts.findByUserId(userId).filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> ApiException.conflict("Your cart is empty"));
    }

    private void requireStock(Cart cart) {
        for (CartItem i : cart.getItems()) {
            if (i.getQuantity() > i.getBook().availableQuantity()) {
                throw ApiException.conflict("'" + i.getBook().getTitle() + "' is out of stock or has fewer than "
                        + i.getQuantity() + " copies left");
            }
        }
    }

    private static String firstShortage(Order order, List<Book> locked) {
        Map<Long, Book> byId = locked.stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        Map<Long, Integer> needed = order.getItems().stream().collect(
                Collectors.groupingBy(i -> i.getBook().getId(), Collectors.summingInt(OrderItem::getQuantity)));
        for (var e : needed.entrySet()) {
            Book b = byId.get(e.getKey());
            if (e.getValue() > b.availableQuantity()) {
                return "'" + b.getTitle() + "' is no longer in stock";
            }
        }
        return null;
    }

    private PricingService.Priced priceCart(Cart cart, String coupon, Integer pointsToRedeem, User user) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(i -> i.getBook().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        return pricing.price(subtotal, count, coupon, pointsToRedeem, user);
    }

    private static void snapshot(Order o, Address a) {
        o.setShipFirstName(a.getFirstName());
        o.setShipLastName(a.getLastName());
        o.setShipEmail(a.getEmail());
        o.setShipPhone(a.getPhoneCountryCode() + a.getPhone());
        o.setShipAddressLine(a.getAddressLine());
        o.setShipCity(a.getCity());
        o.setShipState(a.getState());
        o.setShipPinCode(a.getPinCode());
        o.setShipCountry(a.getCountry());
    }

    private static void snapshot(Order o, AddressRequest a) {
        o.setShipFirstName(a.firstName().trim());
        o.setShipLastName(a.lastName().trim());
        o.setShipEmail(a.email().trim());
        o.setShipPhone(a.phoneCountryCode() + a.phone());
        o.setShipAddressLine(a.addressLine().trim());
        o.setShipCity(a.city().trim());
        o.setShipState(a.state().trim());
        o.setShipPinCode(a.pinCode().trim());
        o.setShipCountry(a.country().trim());
    }

    private OrderDto toDto(Order o) {
        Instant until = o.getCreatedAt().plus(cancelWindow);
        boolean cancellable = o.getStatus() == OrderStatus.PENDING_PAYMENT
                || (o.getStatus() == OrderStatus.PAID && !Instant.now().isAfter(until));
        List<OrderItemDto> items = o.getItems().stream().map(i -> new OrderItemDto(i.getBook().getId(), i.getTitle(),
                i.getAuthorName(), i.getCoverImageUrl(), i.getQuantity(), i.getUnitPrice(),
                Money.round(i.lineTotal()), i.getEstimatedDeliveryDate())).toList();
        PriceBreakdown price = new PriceBreakdown(
                o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(), o.getSubtotal(), o.getTax(),
                o.getDeliveryCharge(), o.getCoupon() == null ? null : o.getCoupon().getCode(),
                o.getCouponDiscount(), o.getPointsRedeemed(), o.getPointsDiscount(), o.getTotal(),
                o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.CANCELLED && o.getPointsEarned() > 0
                        ? o.getPointsEarned() : pricing.pointsToEarn(o.getTotal()));
        PaymentResult payment = o.getPayments().stream().max(Comparator.comparing(Payment::getId))
                .map(this::toDto).orElse(null);
        return new OrderDto(o.getId(), o.getStatus(), items,
                new ShippingAddress(o.getShipFirstName(), o.getShipLastName(), o.getShipEmail(), o.getShipPhone(),
                        o.getShipAddressLine(), o.getShipCity(), o.getShipState(), o.getShipPinCode(),
                        o.getShipCountry()),
                price, payment, o.getCreatedAt(), o.getCancelledAt(), cancellable, until);
    }

    private PaymentResult toDto(Payment p) {
        return new PaymentResult(p.getOrder().getId(), p.getMethod(), p.getStatus(), p.getAmount(), p.getCardLast4(),
                p.getTransactionRef(), p.getFailureReason(), p.getCreatedAt());
    }
}
