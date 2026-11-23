package com.bookworm.service;

import com.bookworm.config.AppProperties;
import com.bookworm.domain.Coupon;
import com.bookworm.domain.DiscountType;
import com.bookworm.domain.User;
import com.bookworm.dto.Dto.PriceBreakdown;
import com.bookworm.exception.ApiException;
import com.bookworm.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** Single source of truth for checkout maths. The client never sends prices or totals. */
@Service
public class PricingService {

    private final AppProperties.Pricing pricing;
    private final CouponRepository coupons;

    public PricingService(AppProperties props, CouponRepository coupons) {
        this.pricing = props.pricing();
        this.coupons = coupons;
    }

    public record Priced(PriceBreakdown breakdown, Coupon coupon) {}

    /**
     * total = subtotal + tax + delivery - coupon - points. It is never negative.
     *
     * @throws ApiException 400 for an unknown, expired, inactive or ineligible coupon, or for points
     *                      that exceed the balance or the payable amount
     */
    public Priced price(BigDecimal subtotalRaw, int itemCount, String couponCode, Integer pointsToRedeem,
                        User user) {
        BigDecimal subtotal = Money.round(subtotalRaw);
        BigDecimal tax = Money.round(subtotal.multiply(pricing.taxRate()));
        BigDecimal delivery = subtotal.signum() == 0 || subtotal.compareTo(pricing.freeDeliveryThreshold()) >= 0
                ? Money.zero() : Money.round(pricing.deliveryCharge());
        BigDecimal gross = subtotal.add(tax).add(delivery);

        Coupon coupon = null;
        BigDecimal couponDiscount = Money.zero();
        if (couponCode != null && !couponCode.isBlank()) {
            coupon = validCoupon(couponCode.trim(), subtotal);
            BigDecimal raw = coupon.getDiscountType() == DiscountType.PERCENT
                    ? subtotal.multiply(coupon.getDiscountValue()).movePointLeft(2)
                    : coupon.getDiscountValue();
            couponDiscount = Money.round(raw).min(gross);
        }
        BigDecimal afterCoupon = gross.subtract(couponDiscount);

        int points = pointsToRedeem == null ? 0 : pointsToRedeem;
        BigDecimal pointsDiscount = Money.round(pricing.pointValue().multiply(BigDecimal.valueOf(points)));
        if (points > user.getGiftPoints()) {
            throw ApiException.badRequest("You only have " + user.getGiftPoints() + " gift points");
        }
        if (pointsDiscount.compareTo(afterCoupon) > 0) {
            throw ApiException.badRequest("Gift points exceed the payable amount");
        }
        BigDecimal total = afterCoupon.subtract(pointsDiscount);
        int pointsToEarn = total.divideToIntegralValue(BigDecimal.valueOf(pricing.earnPerInr())).intValue();

        return new Priced(new PriceBreakdown(itemCount, subtotal, tax, delivery,
                coupon == null ? null : coupon.getCode(), couponDiscount, points, pointsDiscount, total,
                pointsToEarn), coupon);
    }

    private Coupon validCoupon(String code, BigDecimal subtotal) {
        Coupon c = coupons.findByCodeIgnoreCase(code)
                .orElseThrow(() -> ApiException.badRequest("Coupon '" + code + "' does not exist"));
        Instant now = Instant.now();
        if (!c.isActive()) {
            throw ApiException.badRequest("Coupon '" + c.getCode() + "' is not active");
        }
        if (c.getValidFrom() != null && now.isBefore(c.getValidFrom())) {
            throw ApiException.badRequest("Coupon '" + c.getCode() + "' is not valid yet");
        }
        if (c.getValidTo() != null && now.isAfter(c.getValidTo())) {
            throw ApiException.badRequest("Coupon '" + c.getCode() + "' has expired");
        }
        if (subtotal.compareTo(c.getMinOrderAmount()) < 0) {
            throw ApiException.badRequest("Coupon '" + c.getCode() + "' needs a minimum order of "
                    + Money.round(c.getMinOrderAmount()));
        }
        return c;
    }

    public int pointsToEarn(BigDecimal total) {
        return total.divideToIntegralValue(BigDecimal.valueOf(pricing.earnPerInr())).intValue();
    }
}
