package com.bookworm.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void roundsHalfUpToTwoDecimals() {
        assertThat(Money.round(new BigDecimal("1.005")).toPlainString()).isEqualTo("1.01");
        assertThat(Money.round(new BigDecimal("1.004")).toPlainString()).isEqualTo("1.00");
        assertThat(Money.round(new BigDecimal("2")).toPlainString()).isEqualTo("2.00");
    }

    @Test
    void zeroHasTwoDecimals() {
        assertThat(Money.zero().toPlainString()).isEqualTo("0.00");
    }
}
