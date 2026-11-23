package com.bookworm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Negative-path tests. Every error response must use the same {@code ErrorResponse} shape
 * (timestamp, status, error, message, optional fieldErrors) and the right HTTP status.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ValidationAndErrorHandlingTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper json;

    // ------------------------------------------------------------ helpers

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }

    private ResponseEntity<JsonNode> call(HttpMethod method, String path, String token, Object body) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) {
            h.setBearerAuth(token);
        }
        return rest.exchange(url(path), method, new HttpEntity<>(body, h), JsonNode.class);
    }

    private ResponseEntity<JsonNode> rawJson(HttpMethod method, String path, String token, String rawBody) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            h.setBearerAuth(token);
        }
        return rest.exchange(url(path), method, new HttpEntity<>(rawBody, h), JsonNode.class);
    }

    private String register() {
        String email = "v" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        var r = call(HttpMethod.POST, "/auth/register", null,
                Map.of("email", email, "password", "password123", "firstName", "Val", "lastName", "Idate"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().get("accessToken").asText();
    }

    private Map<String, Object> address() {
        return new HashMap<>(Map.ofEntries(Map.entry("firstName", "Test"), Map.entry("lastName", "User"),
                Map.entry("email", "t@example.com"), Map.entry("phoneCountryCode", "+91"),
                Map.entry("phone", "9876543210"), Map.entry("addressLine", "1 MG Road"),
                Map.entry("city", "Bengaluru"), Map.entry("state", "Karnataka"), Map.entry("pinCode", "560001"),
                Map.entry("country", "India")));
    }

    /** Asserts the status and the full error envelope. Returns the body for further checks. */
    private JsonNode assertError(ResponseEntity<JsonNode> r, HttpStatus expected) {
        assertThat(r.getStatusCode()).as(String.valueOf(r.getBody())).isEqualTo(expected);
        JsonNode body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status").asInt()).isEqualTo(expected.value());
        assertThat(body.get("error").asText()).isEqualTo(expected.getReasonPhrase());
        assertThat(body.get("message").asText()).isNotBlank();
        assertThat(body.get("timestamp").asText()).isNotBlank();
        assertThat(body.toString()).doesNotContain("Exception").doesNotContain("at com.bookworm");
        return body;
    }

    private static boolean hasFieldError(JsonNode body, String field) {
        return body.has("fieldErrors") && StreamSupport.stream(body.get("fieldErrors").spliterator(), false)
                .anyMatch(f -> field.equals(f.get("field").asText()) && !f.get("message").asText().isBlank());
    }

    private long firstBookId() {
        return call(HttpMethod.GET, "/books?size=1", null, null).getBody().get("content").get(0).get("id").asLong();
    }

    // --------------------------------------------------------- 400 : body

    @Test
    void registerReportsEveryInvalidField() {
        var body = assertError(call(HttpMethod.POST, "/auth/register", null,
                Map.of("email", "nope", "password", "short")), HttpStatus.BAD_REQUEST);
        assertThat(body.get("message").asText()).isEqualTo("Validation failed");
        assertThat(hasFieldError(body, "email")).isTrue();
        assertThat(hasFieldError(body, "password")).isTrue();
        assertThat(hasFieldError(body, "firstName")).isTrue();
        assertThat(hasFieldError(body, "lastName")).isTrue();
    }

    @Test
    void registerEnforcesFieldLengths() {
        var tooLongPassword = Map.of("email", "a@example.com", "password", "x".repeat(73), "firstName", "A",
                "lastName", "B");
        assertThat(hasFieldError(assertError(call(HttpMethod.POST, "/auth/register", null, tooLongPassword),
                HttpStatus.BAD_REQUEST), "password")).isTrue();
        var tooLongName = Map.of("email", "a@example.com", "password", "password123", "firstName", "A".repeat(101),
                "lastName", "B");
        assertThat(hasFieldError(assertError(call(HttpMethod.POST, "/auth/register", null, tooLongName),
                HttpStatus.BAD_REQUEST), "firstName")).isTrue();
    }

    @Test
    void loginRequiresEmailAndPassword() {
        var body = assertError(call(HttpMethod.POST, "/auth/login", null, Map.of()), HttpStatus.BAD_REQUEST);
        assertThat(hasFieldError(body, "email")).isTrue();
        assertThat(hasFieldError(body, "password")).isTrue();
    }

    @Test
    void malformedAndEmptyJsonBodiesAreBadRequests() {
        assertThat(assertError(rawJson(HttpMethod.POST, "/auth/login", null, "{not json"), HttpStatus.BAD_REQUEST)
                .get("message").asText()).startsWith("Malformed request");
        assertError(rawJson(HttpMethod.POST, "/auth/login", null, ""), HttpStatus.BAD_REQUEST);
        assertError(rawJson(HttpMethod.POST, "/auth/login", null, "[1,2,3]"), HttpStatus.BAD_REQUEST);
        assertError(rawJson(HttpMethod.POST, "/auth/login", null, "{\"email\": 12, \"password\": {}}"),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void addressFieldsAreValidated() {
        String token = register();
        Map<String, Object> a = address();
        a.put("phone", "abc");
        a.put("phoneCountryCode", "91");
        a.put("email", "not-an-email");
        a.put("city", "");
        a.remove("pinCode");
        var body = assertError(call(HttpMethod.POST, "/addresses", token, a), HttpStatus.BAD_REQUEST);
        for (String f : new String[] {"phone", "phoneCountryCode", "email", "city", "pinCode"}) {
            assertThat(hasFieldError(body, f)).as(f).isTrue();
        }
    }

    @Test
    void nestedAddressInsideAnOrderIsValidated() {
        String token = register();
        call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", firstBookId(), "quantity", 1));
        Map<String, Object> a = address();
        a.put("phone", "abc");
        var body = assertError(call(HttpMethod.POST, "/orders", token, Map.of("address", a)), HttpStatus.BAD_REQUEST);
        assertThat(hasFieldError(body, "address.phone")).isTrue();
    }

    @Test
    void cartQuantityRules() {
        String token = register();
        long book = firstBookId();
        assertError(call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", book, "quantity", 0)),
                HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", book, "quantity", -3)),
                HttpStatus.BAD_REQUEST);
        assertThat(hasFieldError(assertError(call(HttpMethod.POST, "/cart/items", token, Map.of("quantity", 1)),
                HttpStatus.BAD_REQUEST), "bookId")).isTrue();
        assertError(call(HttpMethod.PUT, "/cart/items/" + book, token, Map.of("quantity", 0)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void checkoutRequestRules() {
        String token = register();
        call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", firstBookId(), "quantity", 1));
        assertError(call(HttpMethod.POST, "/checkout/quote", token, Map.of("pointsToRedeem", -1)),
                HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.POST, "/checkout/quote", token, Map.of("couponCode", "X".repeat(31))),
                HttpStatus.BAD_REQUEST);
        assertThat(assertError(call(HttpMethod.POST, "/checkout/quote", token, Map.of("couponCode", "NOPE")),
                HttpStatus.BAD_REQUEST).get("message").asText()).contains("does not exist");
        assertThat(assertError(call(HttpMethod.POST, "/checkout/quote", token, Map.of("pointsToRedeem", 1)),
                HttpStatus.BAD_REQUEST).get("message").asText()).contains("gift points");
    }

    @Test
    void orderNeedsExactlyOneOfAddressIdAndAddress() {
        String token = register();
        call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", firstBookId(), "quantity", 1));
        assertError(call(HttpMethod.POST, "/orders", token, Map.of()), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.POST, "/orders", token, Map.of("addressId", 1, "address", address())),
                HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.POST, "/orders", token, Map.of("addressId", 999999)), HttpStatus.NOT_FOUND);
    }

    @Test
    void reviewRules() {
        String token = register();
        long book = firstBookId();
        assertThat(hasFieldError(assertError(call(HttpMethod.POST, "/books/" + book + "/reviews", token,
                Map.of("rating", 0)), HttpStatus.BAD_REQUEST), "rating")).isTrue();
        assertError(call(HttpMethod.POST, "/books/" + book + "/reviews", token, Map.of("rating", 6)),
                HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.POST, "/books/" + book + "/reviews", token, Map.of("comment", "no rating")),
                HttpStatus.BAD_REQUEST);
        assertThat(hasFieldError(assertError(call(HttpMethod.POST, "/books/" + book + "/reviews", token,
                Map.of("rating", 3, "comment", "x".repeat(101))), HttpStatus.BAD_REQUEST), "comment")).isTrue();
        assertError(call(HttpMethod.POST, "/books/999999/reviews", token, Map.of("rating", 3)), HttpStatus.NOT_FOUND);
    }

    // ----------------------------------------------------- 400 : parameters

    @Test
    void queryParametersAreValidated() {
        assertError(call(HttpMethod.GET, "/books?minPrice=-1", null, null), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.GET, "/books?size=0", null, null), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.GET, "/books?size=101", null, null), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.GET, "/books?page=-1", null, null), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.GET, "/books?minPrice=500&maxPrice=100", null, null), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.GET, "/books?format=PDF", null, null), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.GET, "/books?categoryId=abc", null, null), HttpStatus.BAD_REQUEST);
        assertThat(assertError(call(HttpMethod.GET, "/books?sort=bogus", null, null), HttpStatus.BAD_REQUEST)
                .get("message").asText()).contains("Unknown sort");
        assertError(call(HttpMethod.GET, "/home?limit=0", null, null), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.GET, "/home?limit=21", null, null), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.GET, "/books/abc", null, null), HttpStatus.BAD_REQUEST);
    }

    @Test
    void orderListFilterRejectsUnknownStatus() {
        assertError(call(HttpMethod.GET, "/orders?status=SHIPPED", register(), null), HttpStatus.BAD_REQUEST);
    }

    // --------------------------------------------------------- payment input

    @Test
    void paymentFieldsAreValidated() {
        String token = register();
        call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", firstBookId(), "quantity", 1));
        Map<String, Object> order = new HashMap<>();
        order.put("address", address());
        long orderId = call(HttpMethod.POST, "/orders", token, order).getBody().get("id").asLong();
        String path = "/orders/" + orderId + "/payment";

        assertThat(hasFieldError(assertError(call(HttpMethod.POST, path, token, Map.of()), HttpStatus.BAD_REQUEST),
                "method")).isTrue();
        assertError(rawJson(HttpMethod.POST, path, token, "{\"method\":\"BITCOIN\"}"), HttpStatus.BAD_REQUEST);

        var bad = assertError(call(HttpMethod.POST, path, token, Map.of("method", "CREDIT_CARD", "cardNumber", "12ab",
                "cvv", "1", "expiry", "2030-12", "nameOnCard", "A")), HttpStatus.BAD_REQUEST);
        for (String f : new String[] {"cardNumber", "cvv", "expiry"}) {
            assertThat(hasFieldError(bad, f)).as(f).isTrue();
        }
        assertThat(assertError(call(HttpMethod.POST, path, token, Map.of("method", "CREDIT_CARD")),
                HttpStatus.BAD_REQUEST).get("message").asText()).contains("required");
        assertThat(assertError(call(HttpMethod.POST, path, token, Map.of("method", "CREDIT_CARD", "cardNumber",
                "4242424242424241", "nameOnCard", "A", "cvv", "123", "expiry", "12/2099")),
                HttpStatus.BAD_REQUEST).get("message").asText()).contains("invalid");
        assertThat(assertError(call(HttpMethod.POST, path, token, Map.of("method", "CREDIT_CARD", "cardNumber",
                "4242424242424242", "nameOnCard", "A", "cvv", "123", "expiry", "01/2001")),
                HttpStatus.BAD_REQUEST).get("message").asText()).contains("expired");
        assertError(call(HttpMethod.POST, path, token, Map.of("method", "UPI", "upiId", "bad")), HttpStatus.BAD_REQUEST);
        assertError(call(HttpMethod.POST, path, token, Map.of("method", "WALLET")), HttpStatus.BAD_REQUEST);

        // None of the rejected attempts changed the order.
        assertThat(call(HttpMethod.GET, "/orders/" + orderId, token, null).getBody().get("status").asText())
                .isEqualTo("PENDING_PAYMENT");
    }

    // ------------------------------------------------- 401 : authentication

    @Test
    void protectedEndpointsRejectMissingMalformedAndForgedTokens() {
        String[][] endpoints = {
                {"GET", "/cart"}, {"GET", "/orders"}, {"GET", "/users/me"}, {"GET", "/wishlist"},
                {"GET", "/me/writers"}, {"GET", "/addresses"}, {"GET", "/recommendations"},
                {"GET", "/me/gift-points"}, {"POST", "/checkout/quote"}, {"POST", "/orders"},
                {"POST", "/cart/items"}, {"DELETE", "/cart"}, {"PUT", "/wishlist/1"},
        };
        String forged = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl";
        for (String[] e : endpoints) {
            HttpMethod m = HttpMethod.valueOf(e[0]);
            for (String token : new String[] {null, "garbage", forged}) {
                var body = assertError(call(m, e[1], token, m == HttpMethod.POST ? Map.of() : null),
                        HttpStatus.UNAUTHORIZED);
                assertThat(body.get("message").asText()).as(e[0] + " " + e[1]).contains("token");
            }
        }
    }

    @Test
    void publicEndpointsWorkWithoutAToken() {
        for (String p : new String[] {"/home", "/categories", "/publishers", "/books", "/books/1", "/books/1/related",
                "/books/1/reviews", "/authors/1", "/actuator/health"}) {
            assertThat(call(HttpMethod.GET, p, null, null).getStatusCode().is2xxSuccessful()).as(p).isTrue();
        }
    }

    @Test
    void aBadTokenOnAPublicEndpointDoesNotBreakIt() {
        assertThat(call(HttpMethod.GET, "/books", "garbage", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void loginDoesNotRevealWhetherTheEmailExists() {
        String token = register(); // makes sure at least one user exists
        assertThat(token).isNotBlank();
        var unknown = assertError(call(HttpMethod.POST, "/auth/login", null,
                Map.of("email", "ghost@example.com", "password", "password123")), HttpStatus.UNAUTHORIZED);
        var wrong = assertError(call(HttpMethod.POST, "/auth/login", null,
                Map.of("email", "ghost@example.com", "password", "x")), HttpStatus.UNAUTHORIZED);
        assertThat(unknown.get("message").asText()).isEqualTo(wrong.get("message").asText());
    }

    // ------------------------------------------- 404 / 405 / 409 : the rest

    @Test
    void unknownResourcesAre404() {
        String token = register();
        assertError(call(HttpMethod.GET, "/books/999999", null, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.GET, "/books/999999/related", null, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.GET, "/authors/999999", null, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.GET, "/orders/999999", token, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.POST, "/orders/999999/cancel", token, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.POST, "/orders/999999/reorder", token, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.GET, "/addresses/999999", token, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.PUT, "/cart/items/999999", token, Map.of("quantity", 1)), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.DELETE, "/cart/items/999999", token, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.PUT, "/wishlist/999999", token, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.PUT, "/me/writers/999999", token, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.GET, "/no/such/endpoint", token, null), HttpStatus.NOT_FOUND);
    }

    @Test
    void wrongHttpMethodIs405() {
        String token = register();
        assertError(call(HttpMethod.DELETE, "/categories", token, null), HttpStatus.METHOD_NOT_ALLOWED);
        assertError(call(HttpMethod.PUT, "/orders", token, Map.of()), HttpStatus.METHOD_NOT_ALLOWED);
        assertError(call(HttpMethod.GET, "/auth/login", null, null), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void stateConflictsAre409() {
        String token = register();
        long book = firstBookId();
        // Empty cart cannot be quoted or ordered.
        assertThat(assertError(call(HttpMethod.POST, "/checkout/quote", token, Map.of()), HttpStatus.CONFLICT)
                .get("message").asText()).contains("empty");
        Map<String, Object> order = Map.of("address", address());
        assertError(call(HttpMethod.POST, "/orders", token, order), HttpStatus.CONFLICT);

        call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", book, "quantity", 1));
        long orderId = call(HttpMethod.POST, "/orders", token, order).getBody().get("id").asLong();
        String pay = "/orders/" + orderId + "/payment";
        Map<String, Object> upi = Map.of("method", "UPI", "upiId", "name@bank");
        assertThat(call(HttpMethod.POST, pay, token, upi).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertError(call(HttpMethod.POST, pay, token, upi), HttpStatus.CONFLICT); // already paid
        assertThat(call(HttpMethod.POST, "/orders/" + orderId + "/cancel", token, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertError(call(HttpMethod.POST, "/orders/" + orderId + "/cancel", token, null), HttpStatus.CONFLICT);
        assertError(call(HttpMethod.POST, pay, token, upi), HttpStatus.CONFLICT); // cancelled
    }

    @Test
    void duplicateRegistrationAndDuplicateReviewAre409() {
        String email = "dup" + UUID.randomUUID().toString().substring(0, 6) + "@example.com";
        var body = Map.of("email", email, "password", "password123", "firstName", "A", "lastName", "B");
        assertThat(call(HttpMethod.POST, "/auth/register", null, body).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertError(call(HttpMethod.POST, "/auth/register", null, body), HttpStatus.CONFLICT);
        // E-mail matching is case-insensitive.
        var upper = Map.of("email", email.toUpperCase(), "password", "password123", "firstName", "A", "lastName", "B");
        assertError(call(HttpMethod.POST, "/auth/register", null, upper), HttpStatus.CONFLICT);

        String token = register();
        long book = firstBookId();
        assertThat(call(HttpMethod.POST, "/books/" + book + "/reviews", token, Map.of("rating", 5)).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertError(call(HttpMethod.POST, "/books/" + book + "/reviews", token, Map.of("rating", 4)),
                HttpStatus.CONFLICT);
    }

    // -------------------------------------------------- isolation and safety

    @Test
    void usersCannotSeeOrChangeEachOthersData() {
        String alice = register();
        String bob = register();
        long book = firstBookId();
        call(HttpMethod.POST, "/cart/items", alice, Map.of("bookId", book, "quantity", 1));
        long addressId = call(HttpMethod.POST, "/addresses", alice, address()).getBody().get("id").asLong();
        long orderId = call(HttpMethod.POST, "/orders", alice, Map.of("addressId", addressId)).getBody().get("id")
                .asLong();

        assertThat(call(HttpMethod.GET, "/cart", bob, null).getBody().get("itemCount").asInt()).isZero();
        assertThat(call(HttpMethod.GET, "/orders", bob, null).getBody().get("totalElements").asInt()).isZero();
        assertThat(call(HttpMethod.GET, "/addresses", bob, null).getBody()).isEmpty();
        assertError(call(HttpMethod.GET, "/addresses/" + addressId, bob, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.PUT, "/addresses/" + addressId, bob, address()), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.DELETE, "/addresses/" + addressId, bob, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.POST, "/orders", bob, Map.of("addressId", addressId)), HttpStatus.CONFLICT);
        assertError(call(HttpMethod.GET, "/orders/" + orderId, bob, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.POST, "/orders/" + orderId + "/payment", bob,
                Map.of("method", "UPI", "upiId", "a@bank")), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.POST, "/orders/" + orderId + "/cancel", bob, null), HttpStatus.NOT_FOUND);
        assertError(call(HttpMethod.POST, "/orders/" + orderId + "/reorder", bob, null), HttpStatus.NOT_FOUND);
        // Alice's order is untouched.
        assertThat(call(HttpMethod.GET, "/orders/" + orderId, alice, null).getBody().get("status").asText())
                .isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void sensitiveDataNeverLeavesTheServer() {
        String email = "safe" + UUID.randomUUID().toString().substring(0, 6) + "@example.com";
        var reg = call(HttpMethod.POST, "/auth/register", null,
                Map.of("email", email, "password", "password123", "firstName", "S", "lastName", "Afe"));
        assertThat(reg.getBody().toString()).doesNotContain("password").doesNotContain("$2a$");
        String token = reg.getBody().get("accessToken").asText();
        assertThat(call(HttpMethod.GET, "/users/me", token, null).getBody().toString())
                .doesNotContain("password").doesNotContain("$2a$");

        call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", firstBookId(), "quantity", 1));
        long orderId = call(HttpMethod.POST, "/orders", token, Map.of("address", address())).getBody().get("id")
                .asLong();
        var paid = call(HttpMethod.POST, "/orders/" + orderId + "/payment", token,
                Map.of("method", "CREDIT_CARD", "cardNumber", "4242424242424242", "nameOnCard", "S Afe",
                        "cvv", "737", "expiry", "12/2099"));
        assertThat(paid.getBody().toString()).doesNotContain("4242424242424242").doesNotContain("737");
        assertThat(call(HttpMethod.GET, "/orders/" + orderId, token, null).getBody().toString())
                .doesNotContain("4242424242424242").doesNotContain("\"cvv\"");
    }

    @Test
    void serverIgnoresClientSuppliedPrices() {
        String token = register();
        long book = firstBookId();
        call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", book, "quantity", 1));
        double honest = call(HttpMethod.POST, "/checkout/quote", token, Map.of()).getBody().get("total").asDouble();
        Map<String, Object> order = new HashMap<>();
        order.put("address", address());
        order.put("total", 0.01);
        order.put("subtotal", 0.01);
        var placed = call(HttpMethod.POST, "/orders", token, order);
        assertThat(placed.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(placed.getBody().get("price").get("total").asDouble()).isEqualTo(honest);
    }
}
