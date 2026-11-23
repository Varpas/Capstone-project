package com.bookworm.service;

import com.bookworm.domain.PaymentMethod;
import com.bookworm.dto.Dto.PaymentRequest;
import com.bookworm.exception.ApiException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Stands in for a payment gateway. Validates the method-specific fields, then decides the outcome
 * deterministically: a card number ending in 0000 is declined, everything else succeeds.
 * Card numbers and CVVs are only inspected here and never stored or logged.
 */
@Component
public class PaymentSimulator {

    private static final Pattern UPI = Pattern.compile("^[A-Za-z0-9._-]{2,256}@[A-Za-z]{2,64}$");
    private static final DateTimeFormatter EXPIRY = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    public record Outcome(boolean success, String cardLast4, String transactionRef, String failureReason) {}

    /** @throws ApiException 400 when the input is incomplete or invalid */
    public void validate(PaymentRequest r) {
        if (r.method() == PaymentMethod.CREDIT_CARD || r.method() == PaymentMethod.DEBIT_CARD) {
            if (isBlank(r.cardNumber()) || isBlank(r.cvv()) || isBlank(r.expiry()) || isBlank(r.nameOnCard())) {
                throw ApiException.badRequest("cardNumber, nameOnCard, cvv and expiry are required for card payments");
            }
            if (!luhn(r.cardNumber())) {
                throw ApiException.badRequest("Card number is invalid");
            }
            if (YearMonth.parse(r.expiry(), EXPIRY).isBefore(YearMonth.now())) {
                throw ApiException.badRequest("Card has expired");
            }
        } else if (r.method() == PaymentMethod.UPI) {
            if (isBlank(r.upiId()) || !UPI.matcher(r.upiId()).matches()) {
                throw ApiException.badRequest("A valid upiId such as name@bank is required for UPI");
            }
        } else if (r.method() == PaymentMethod.WALLET) {
            if (isBlank(r.walletId())) {
                throw ApiException.badRequest("walletId is required for WALLET");
            }
        }
    }

    public Outcome charge(PaymentRequest r) {
        String ref = "TXN-" + LocalDate.now().format(DAY) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        boolean card = r.method() == PaymentMethod.CREDIT_CARD || r.method() == PaymentMethod.DEBIT_CARD;
        String last4 = card ? r.cardNumber().substring(r.cardNumber().length() - 4) : null;
        if (card && "0000".equals(last4)) {
            return new Outcome(false, last4, ref, "Card declined by issuer");
        }
        return new Outcome(true, last4, ref, null);
    }

    static boolean luhn(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int d = number.charAt(i) - '0';
            if (alternate) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
