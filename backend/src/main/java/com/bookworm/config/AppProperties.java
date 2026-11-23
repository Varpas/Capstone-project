package com.bookworm.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bookworm")
public record AppProperties(Jwt jwt, Pricing pricing, Orders orders, Cors cors) {

    public record Jwt(String secret, long expirySeconds) {}

    /**
     * @param taxRate               fraction of the subtotal, e.g. 0.12
     * @param freeDeliveryThreshold subtotal at or above which delivery is free
     * @param deliveryCharge        flat fee below the threshold
     * @param pointValue            INR value of one gift point when redeemed
     * @param earnPerInr            one point is earned per this many INR of the total
     */
    public record Pricing(BigDecimal taxRate, BigDecimal freeDeliveryThreshold, BigDecimal deliveryCharge,
                          BigDecimal pointValue, int earnPerInr) {}

    public record Orders(int cancelWindowHours) {}

    public record Cors(List<String> allowedOrigins) {}
}
