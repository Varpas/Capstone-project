package com.bookworm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookworm.domain.PaymentMethod;
import com.bookworm.dto.Dto.PaymentRequest;
import com.bookworm.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Pure unit tests for the payment simulator's validation and decline rules. */
class PaymentSimulatorTest {

    private final PaymentSimulator sim = new PaymentSimulator();

    private static PaymentRequest card(String number, String cvv, String expiry, String name) {
        return new PaymentRequest(PaymentMethod.CREDIT_CARD, number, name, cvv, expiry, null, null);
    }

    private void assertRejected(PaymentRequest r, String messagePart) {
        assertThatThrownBy(() -> sim.validate(r))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining(messagePart);
    }

    @Test
    void luhnAcceptsKnownGoodAndRejectsBadNumbers() {
        assertThat(PaymentSimulator.luhn("4242424242424242")).isTrue();
        assertThat(PaymentSimulator.luhn("4000000000020000")).isTrue();
        assertThat(PaymentSimulator.luhn("4242424242424241")).isFalse();
        assertThat(PaymentSimulator.luhn("1234567812345678")).isFalse();
    }

    @Test
    void acceptsAValidCard() {
        sim.validate(card("4242424242424242", "123", "12/2099", "Test User"));
    }

    @Test
    void rejectsIncompleteCardDetails() {
        assertRejected(card(null, "123", "12/2099", "Test"), "required");
        assertRejected(card("4242424242424242", null, "12/2099", "Test"), "required");
        assertRejected(card("4242424242424242", "123", null, "Test"), "required");
        assertRejected(card("4242424242424242", "123", "12/2099", " "), "required");
    }

    @Test
    void rejectsInvalidAndExpiredCards() {
        assertRejected(card("4242424242424241", "123", "12/2099", "Test"), "invalid");
        assertRejected(card("4242424242424242", "123", "01/2001", "Test"), "expired");
    }

    @Test
    void debitCardsAreValidatedLikeCreditCards() {
        var r = new PaymentRequest(PaymentMethod.DEBIT_CARD, "4242424242424241", "Test", "123", "12/2099", null, null);
        assertRejected(r, "invalid");
    }

    @Test
    void upiNeedsAWellFormedId() {
        sim.validate(new PaymentRequest(PaymentMethod.UPI, null, null, null, null, "name@bank", null));
        sim.validate(new PaymentRequest(PaymentMethod.UPI, null, null, null, null, "first.last-1@okaxis", null));
        for (String bad : new String[] {null, "", "not-a-vpa", "@bank", "a@", "a@b4nk"}) {
            assertRejected(new PaymentRequest(PaymentMethod.UPI, null, null, null, null, bad, null), "upiId");
        }
    }

    @Test
    void walletNeedsAnId() {
        sim.validate(new PaymentRequest(PaymentMethod.WALLET, null, null, null, null, null, "w-123"));
        assertRejected(new PaymentRequest(PaymentMethod.WALLET, null, null, null, null, null, " "), "walletId");
    }

    @Test
    void cardEndingInZerosIsDeclinedWithoutLeakingTheNumber() {
        var out = sim.charge(card("4000000000020000", "123", "12/2099", "Test"));
        assertThat(out.success()).isFalse();
        assertThat(out.cardLast4()).isEqualTo("0000");
        assertThat(out.failureReason()).isEqualTo("Card declined by issuer");
        assertThat(out.toString()).doesNotContain("4000000000020000");
    }

    @Test
    void otherMethodsSucceedWithUniqueReferences() {
        var card1 = sim.charge(card("4242424242424242", "123", "12/2099", "Test"));
        var card2 = sim.charge(card("4242424242424242", "123", "12/2099", "Test"));
        var upi = sim.charge(new PaymentRequest(PaymentMethod.UPI, null, null, null, null, "a@bank", null));
        assertThat(card1.success()).isTrue();
        assertThat(card1.cardLast4()).isEqualTo("4242");
        assertThat(card1.transactionRef()).matches("TXN-\\d{8}-[0-9A-F]{8}").isNotEqualTo(card2.transactionRef());
        assertThat(upi.success()).isTrue();
        assertThat(upi.cardLast4()).isNull();
    }
}
