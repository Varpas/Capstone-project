package com.bookworm.dto;

import com.bookworm.domain.BookFormat;
import com.bookworm.domain.OrderStatus;
import com.bookworm.domain.PaymentMethod;
import com.bookworm.domain.PaymentStatus;
import com.bookworm.domain.PointTxnType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response payloads. They mirror the schemas in docs/openapi.yaml. */
public final class Dto {

    private Dto() {}

    // ------------------------------------------------------------- common
    public record PageResponse<T>(int page, int size, long totalElements, int totalPages, List<T> content) {
        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> p) {
            return new PageResponse<>(p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                    p.getContent());
        }
    }

    // --------------------------------------------------------------- auth
    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 20) String phone) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserDto(Long id, String email, String firstName, String lastName, String phone, int giftPoints) {}

    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserDto user) {}

    // ---------------------------------------------------------- catalogue
    public record CategoryDto(Long id, String name) {}

    public record PublisherDto(Long id, String name) {}

    public record AuthorRef(Long id, String name) {}

    public record AuthorDto(Long id, String name, String bio, String photoUrl, Boolean followed) {}

    public record BookSummary(
            Long id,
            String title,
            AuthorRef author,
            String description,
            BookFormat format,
            List<CategoryDto> categories,
            BigDecimal price,
            String coverImageUrl,
            BigDecimal averageRating,
            boolean inStock,
            LocalDate estimatedDeliveryDate,
            Boolean wishlisted) {}

    public record BookDetail(
            Long id,
            String title,
            AuthorRef author,
            String description,
            BookFormat format,
            List<CategoryDto> categories,
            BigDecimal price,
            String coverImageUrl,
            BigDecimal averageRating,
            boolean inStock,
            LocalDate estimatedDeliveryDate,
            Boolean wishlisted,
            String isbn,
            String language,
            PublisherDto publisher,
            String backCoverImageUrl,
            LocalDate publishedDate,
            int copiesSold,
            int reviewCount,
            int stock) {}

    public record HomeResponse(List<BookSummary> recommended, List<BookSummary> bestsellers,
                               List<BookSummary> newLaunches) {}

    // ------------------------------------------------------------ reviews
    public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 100) String comment) {}

    public record ReviewDto(Long id, Long bookId, String userName, int rating, String comment, Instant createdAt) {}

    // --------------------------------------------------------------- cart
    public record CartItemRequest(@NotNull Long bookId, @Min(1) Integer quantity) {}

    public record CartQuantityRequest(@NotNull @Min(1) Integer quantity) {}

    public record CartItemDto(BookSummary book, int quantity, BigDecimal lineTotal) {}

    public record CartDto(List<CartItemDto> items, int itemCount, BigDecimal subtotal) {}

    // ----------------------------------------------------------- address
    public record AddressRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = "^\\+[0-9]{1,4}$", message = "must look like +91") String phoneCountryCode,
            @NotBlank @Pattern(regexp = "^[0-9]{6,15}$", message = "must be 6 to 15 digits") String phone,
            @NotBlank @Size(max = 255) String addressLine,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 10) String pinCode,
            @NotBlank @Size(max = 100) String country,
            @JsonProperty("isDefault") Boolean isDefault) {}

    public record AddressDto(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phoneCountryCode,
            String phone,
            String addressLine,
            String city,
            String state,
            String pinCode,
            String country,
            @JsonProperty("isDefault") boolean isDefault) {}

    // ----------------------------------------------------------- checkout
    public record PricingRequest(@Size(max = 30) String couponCode, @Min(0) Integer pointsToRedeem) {}

    public record PriceBreakdown(
            int itemCount,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal deliveryCharge,
            String couponCode,
            BigDecimal couponDiscount,
            int pointsRedeemed,
            BigDecimal pointsDiscount,
            BigDecimal total,
            int pointsToEarn) {}

    public record PlaceOrderRequest(
            @Size(max = 30) String couponCode,
            @Min(0) Integer pointsToRedeem,
            Long addressId,
            @Valid AddressRequest address,
            Boolean saveAddress) {}

    // ------------------------------------------------------------- orders
    public record OrderItemDto(Long bookId, String title, String authorName, String coverImageUrl, int quantity,
                               BigDecimal unitPrice, BigDecimal lineTotal, LocalDate estimatedDeliveryDate) {}

    public record ShippingAddress(String firstName, String lastName, String email, String phone, String addressLine,
                                  String city, String state, String pinCode, String country) {}

    public record OrderDto(
            Long id,
            OrderStatus status,
            List<OrderItemDto> items,
            ShippingAddress shippingAddress,
            PriceBreakdown price,
            PaymentResult payment,
            Instant createdAt,
            Instant cancelledAt,
            boolean cancellable,
            Instant cancellableUntil) {}

    public record SkippedItem(Long bookId, String reason) {}

    public record ReorderResult(CartDto cart, List<SkippedItem> skipped) {}

    // ------------------------------------------------------------ payment
    public record PaymentRequest(
            @NotNull PaymentMethod method,
            @Pattern(regexp = "^[0-9]{13,19}$", message = "must be 13 to 19 digits") String cardNumber,
            @Size(max = 100) String nameOnCard,
            @Pattern(regexp = "^[0-9]{3,4}$", message = "must be 3 or 4 digits") String cvv,
            @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{4}$", message = "must be MM/YYYY") String expiry,
            String upiId,
            String walletId) {}

    public record PaymentResult(Long orderId, PaymentMethod method, PaymentStatus status, BigDecimal amount,
                                String cardLast4, String transactionRef, String failureReason, Instant createdAt) {}

    // -------------------------------------------------------- gift points
    public record GiftPointTxnDto(Long id, Long orderId, int points, PointTxnType type, Instant createdAt) {}

    public record GiftPointsDto(int balance, PageResponse<GiftPointTxnDto> transactions) {}
}
