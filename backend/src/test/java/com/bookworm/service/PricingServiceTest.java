package com.bookworm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookworm.config.AppProperties;
import com.bookworm.domain.Coupon;
import com.bookworm.domain.DiscountType;
import com.bookworm.domain.User;
import com.bookworm.dto.Dto.PriceBreakdown;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Pure unit tests for the checkout maths and coupon rules. Only the coupon repository is mocked. */
class PricingServiceTest {

    private CouponRepository coupons;
    private PricingService pricing;
    private User user;

    @BeforeEach
    void setUp() {
        coupons = mock(CouponRepository.class);
        var props = new AppProperties(
                new AppProperties.Jwt("x".repeat(40), 3600),
                new AppProperties.Pricing(new BigDecimal("0.12"), new BigDecimal("500.00"), new BigDecimal("40.00"),
                        new BigDecimal("1.00"), 100),
                new AppProperties.Orders(48),
                new AppProperties.Cors(List.of()));
        pricing = new PricingService(props, coupons);
        user = new User();
        user.setGiftPoints(20);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private PriceBreakdown price(String subtotal, String coupon, Integer points) {
        return pricing.price(bd(subtotal), 1, coupon, points, user).breakdown();
    }

    private void coupon(String code, DiscountType type, String value, String min, Instant from, Instant to,
                        boolean active) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDiscountType(type);
        c.setDiscountValue(bd(value));
        c.setMinOrderAmount(bd(min));
        c.setValidFrom(from);
        c.setValidTo(to);
        c.setActive(active);
        when(coupons.findByCodeIgnoreCase(code)).thenReturn(Optional.of(c));
    }

    private static void assertBadRequest(Runnable r, String messagePart) {
        assertThatThrownBy(r::run)
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining(messagePart);
    }

    // ------------------------------------------------------- delivery and tax

    @Test
    void chargesDeliveryBelowThresholdAndAddsTax() {
        PriceBreakdown p = price("499.00", null, null);
        assertThat(p.tax()).isEqualByComparingTo("59.88");
        assertThat(p.deliveryCharge()).isEqualByComparingTo("40.00");
        assertThat(p.total()).isEqualByComparingTo("598.88");
    }

    @Test
    void deliveryIsFreeAtTheThresholdExactly() {
        PriceBreakdown p = price("500.00", null, null);
        assertThat(p.deliveryCharge()).isEqualByComparingTo("0.00");
        assertThat(p.total()).isEqualByComparingTo("560.00");
    }

    @Test
    void emptySubtotalCostsNothing() {
        PriceBreakdown p = price("0", null, null);
        assertThat(p.deliveryCharge()).isEqualByComparingTo("0");
        assertThat(p.total()).isEqualByComparingTo("0");
    }

    @Test
    void taxIsRoundedHalfUpToTwoDecimals() {
        // 10.05 * 0.12 = 1.206 -> 1.21
        assertThat(price("10.05", null, null).tax()).isEqualByComparingTo("1.21");
        // 0.04 * 0.12 = 0.0048 -> 0.00
        assertThat(price("0.04", null, null).tax()).isEqualByComparingTo("0.00");
    }

    // ----------------------------------------------------------------- coupons

    @Test
    void percentCouponIsAppliedToTheSubtotal() {
        coupon("WELCOME10", DiscountType.PERCENT, "10.00", "0.00", null, null, true);
        PriceBreakdown p = price("527.00", "WELCOME10", null);
        assertThat(p.couponCode()).isEqualTo("WELCOME10");
        assertThat(p.couponDiscount()).isEqualByComparingTo("52.70");
        assertThat(p.total()).isEqualByComparingTo("537.54");
    }

    @Test
    void fixedCouponNeedsTheMinimumOrder() {
        coupon("FLAT100", DiscountType.FIXED, "100.00", "500.00", null, null, true);
        assertThat(price("500.00", "FLAT100", null).couponDiscount()).isEqualByComparingTo("100.00");
        assertBadRequest(() -> price("499.00", "FLAT100", null), "minimum order");
    }

    @Test
    void couponCodeIsTrimmed() {
        coupon("WELCOME10", DiscountType.PERCENT, "10.00", "0.00", null, null, true);
        assertThat(price("100.00", "  WELCOME10 ", null).couponCode()).isEqualTo("WELCOME10");
    }

    @Test
    void couponDiscountNeverExceedsTheOrderValue() {
        coupon("HUGE", DiscountType.FIXED, "10000.00", "0.00", null, null, true);
        PriceBreakdown p = price("100.00", "HUGE", null);
        assertThat(p.total()).isEqualByComparingTo("0.00");
        assertThat(p.couponDiscount()).isEqualByComparingTo("152.00"); // 100 + 12 tax + 40 delivery
    }

    @Test
    void rejectsUnknownExpiredInactiveAndFutureCoupons() {
        Instant now = Instant.now();
        when(coupons.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());
        coupon("OLD", DiscountType.PERCENT, "20.00", "0.00", now.minus(30, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.DAYS), true);
        coupon("OFF", DiscountType.PERCENT, "20.00", "0.00", null, null, false);
        coupon("SOON", DiscountType.PERCENT, "20.00", "0.00", now.plus(1, ChronoUnit.DAYS), null, true);

        assertBadRequest(() -> price("100.00", "NOPE", null), "does not exist");
        assertBadRequest(() -> price("100.00", "OLD", null), "has expired");
        assertBadRequest(() -> price("100.00", "OFF", null), "not active");
        assertBadRequest(() -> price("100.00", "SOON", null), "not valid yet");
    }

    @Test
    void blankCouponIsTreatedAsNoCoupon() {
        PriceBreakdown p = price("100.00", "   ", null);
        assertThat(p.couponCode()).isNull();
        assertThat(p.couponDiscount()).isEqualByComparingTo("0.00");
    }

    // ------------------------------------------------------------- gift points

    @Test
    void pointsReduceTheTotalOnePointPerRupee() {
        PriceBreakdown p = price("499.00", null, 10);
        assertThat(p.pointsRedeemed()).isEqualTo(10);
        assertThat(p.pointsDiscount()).isEqualByComparingTo("10.00");
        assertThat(p.total()).isEqualByComparingTo("588.88");
    }

    @Test
    void cannotRedeemMoreThanTheBalance() {
        assertBadRequest(() -> price("499.00", null, 21), "only have 20 gift points");
    }

    @Test
    void cannotRedeemMoreThanThePayableAmount() {
        user.setGiftPoints(1000);
        assertBadRequest(() -> price("10.00", null, 500), "exceed the payable amount");
    }

    @Test
    void earnsOnePointPerHundredRupeesOfTheTotal() {
        assertThat(price("527.00", null, null).pointsToEarn()).isEqualTo(5); // 590.24
        assertThat(price("499.00", null, null).pointsToEarn()).isEqualTo(5); // 598.88
        assertThat(price("100.00", null, null).pointsToEarn()).isEqualTo(1); // 152.00
        assertThat(price("10.00", null, null).pointsToEarn()).isEqualTo(0);  // 51.20
    }
}
