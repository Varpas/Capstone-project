package com.bookworm.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money rounding is defined once: 2 decimals, HALF_UP. */
public final class Money {

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return round(BigDecimal.ZERO);
    }
}
