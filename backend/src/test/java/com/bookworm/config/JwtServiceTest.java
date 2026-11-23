package com.bookworm.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Token round trip, tampering, expiry and wrong-key handling. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    private static JwtService service(String secret, long expirySeconds) {
        return new JwtService(new AppProperties(new AppProperties.Jwt(secret, expirySeconds),
                new AppProperties.Pricing(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, 100),
                new AppProperties.Orders(48), new AppProperties.Cors(List.of())));
    }

    @Test
    void issuedTokenRoundTripsToTheUserId() {
        JwtService jwt = service(SECRET, 3600);
        assertThat(jwt.parseUserId(jwt.issue(42L, "a@b.com"))).contains(42L);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = service(SECRET, 3600);
        String token = jwt.issue(42L, "a@b.com");
        String[] parts = token.split("\\.");
        // Swap in another user's payload, keeping the original signature.
        String otherPayload = service(SECRET, 3600).issue(7L, "x@y.com").split("\\.")[1];
        String tampered = parts[0] + "." + otherPayload + "." + parts[2];
        assertThat(jwt.parseUserId(tampered)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreign = service("another-secret-another-secret-another-secret", 3600).issue(1L, "a@b.com");
        assertThat(service(SECRET, 3600).parseUserId(foreign)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder().subject("1")
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key).compact();
        assertThat(service(SECRET, 3600).parseUserId(expired)).isEmpty();
    }

    @Test
    void garbageAndBlankTokensAreRejected() {
        JwtService jwt = service(SECRET, 3600);
        assertThat(jwt.parseUserId("garbage")).isEmpty();
        assertThat(jwt.parseUserId("")).isEmpty();
        assertThat(jwt.parseUserId("a.b.c")).isEmpty();
    }
}
