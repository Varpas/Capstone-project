package com.bookworm;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookworm.domain.Order;
import com.bookworm.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/** Walks the 12-step customer journey over real HTTP against an in-memory database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String GOOD_CARD = "4242424242424242";
    private static final String DECLINED_CARD = "4000000000020000"; // Luhn-valid, ends in 0000

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper json;
    @Autowired OrderRepository orderRepository;
    @Autowired TransactionTemplate tx;

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

    private JsonNode ok(ResponseEntity<JsonNode> r) {
        assertThat(r.getStatusCode().is2xxSuccessful()).as(String.valueOf(r.getBody())).isTrue();
        return r.getBody();
    }

    private String register() {
        String email = "u" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        var r = call(HttpMethod.POST, "/auth/register", null,
                Map.of("email", email, "password", "password123", "firstName", "Test", "lastName", "User"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().get("accessToken").asText();
    }

    private Map<String, Object> address() {
        return Map.ofEntries(Map.entry("firstName", "Test"), Map.entry("lastName", "User"),
                Map.entry("email", "t@example.com"), Map.entry("phoneCountryCode", "+91"),
                Map.entry("phone", "9876543210"), Map.entry("addressLine", "1 MG Road"),
                Map.entry("city", "Bengaluru"), Map.entry("state", "Karnataka"), Map.entry("pinCode", "560001"),
                Map.entry("country", "India"));
    }

    private long bookId(String title) {
        // RestTemplate encodes the URL itself, so the title is passed through as a template variable.
        var r = rest.getForEntity(url("/books?q={q}"), JsonNode.class, title);
        var page = ok(r);
        return page.get("content").get(0).get("id").asLong();
    }

    private void addToCart(String token, long bookId, int qty) {
        ok(call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", bookId, "quantity", qty)));
    }

    private JsonNode placeOrder(String token, Map<String, Object> extra) {
        var body = new java.util.HashMap<String, Object>(extra);
        body.put("address", address());
        var r = call(HttpMethod.POST, "/orders", token, body);
        assertThat(r.getStatusCode()).as(String.valueOf(r.getBody())).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }

    private ResponseEntity<JsonNode> payByCard(String token, long orderId, String card) {
        return call(HttpMethod.POST, "/orders/" + orderId + "/payment", token,
                Map.of("method", "CREDIT_CARD", "cardNumber", card, "nameOnCard", "Test User", "cvv", "123",
                        "expiry", "12/2099"));
    }

    private int stock(long bookId) {
        return ok(call(HttpMethod.GET, "/books/" + bookId, null, null)).get("stock").asInt();
    }

    // -------------------------------------------------------------- tests

    @Test
    void guestsCanBrowseButNotUseCart() {
        assertThat(ok(call(HttpMethod.GET, "/categories", null, null)).size()).isGreaterThanOrEqualTo(19);
        assertThat(ok(call(HttpMethod.GET, "/publishers", null, null)).size()).isGreaterThan(0);
        JsonNode home = ok(call(HttpMethod.GET, "/home?limit=4", null, null));
        assertThat(home.get("bestsellers")).hasSize(4);
        assertThat(home.get("newLaunches")).hasSize(4);
        assertThat(home.get("recommended")).hasSize(4);
        assertThat(call(HttpMethod.GET, "/cart", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call(HttpMethod.GET, "/cart", "garbage", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void searchFilterSortAndDetail() {
        JsonNode cheapest = ok(call(HttpMethod.GET, "/books?sort=price_asc&size=3", null, null));
        double first = cheapest.get("content").get(0).get("price").asDouble();
        double second = cheapest.get("content").get(1).get("price").asDouble();
        assertThat(first).isLessThanOrEqualTo(second);
        assertThat(cheapest.get("totalElements").asInt()).isEqualTo(30);

        JsonNode ebooks = ok(call(HttpMethod.GET, "/books?format=EBOOK&maxPrice=100", null, null));
        ebooks.get("content").forEach(b -> {
            assertThat(b.get("format").asText()).isEqualTo("EBOOK");
            assertThat(b.get("price").asDouble()).isLessThanOrEqualTo(100);
        });

        long id = bookId("Joy of Minimalism");
        JsonNode detail = ok(call(HttpMethod.GET, "/books/" + id, null, null));
        assertThat(detail.get("publisher").get("name").asText()).isEqualTo("ABC Publishers");
        assertThat(detail.get("estimatedDeliveryDate").asText()).isNotBlank();
        assertThat(ok(call(HttpMethod.GET, "/books/" + id + "/related", null, null)).size()).isEqualTo(3);
        assertThat(call(HttpMethod.GET, "/books/999999", null, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "/books?minPrice=500&maxPrice=100", null, null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void fullJourneyWithCouponPointsCancelAndReorder() {
        String token = register();
        long book = bookId("Joy of Minimalism");           // 149.00, physical
        long book2 = bookId("The Last Witness");           // 229.00, physical
        int stockBefore = stock(book);

        addToCart(token, book, 2);
        addToCart(token, book2, 1);
        JsonNode cart = ok(call(HttpMethod.GET, "/cart", token, null));
        assertThat(cart.get("itemCount").asInt()).isEqualTo(3);
        assertThat(cart.get("subtotal").asDouble()).isEqualTo(527.00);

        // Quote: 527 + 12% tax (63.24) + free delivery - 10% coupon (52.70) = 537.54
        JsonNode quote = ok(call(HttpMethod.POST, "/checkout/quote", token, Map.of("couponCode", "WELCOME10")));
        assertThat(quote.get("tax").asDouble()).isEqualTo(63.24);
        assertThat(quote.get("deliveryCharge").asDouble()).isEqualTo(0.0);
        assertThat(quote.get("couponDiscount").asDouble()).isEqualTo(52.70);
        assertThat(quote.get("total").asDouble()).isEqualTo(537.54);
        assertThat(quote.get("pointsToEarn").asInt()).isEqualTo(5);

        // Bad coupons are rejected with a reason
        var expired = call(HttpMethod.POST, "/checkout/quote", token, Map.of("couponCode", "EXPIRED20"));
        assertThat(expired.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(expired.getBody().get("message").asText()).contains("expired");

        JsonNode order = placeOrder(token, Map.of("couponCode", "WELCOME10"));
        long orderId = order.get("id").asLong();
        assertThat(order.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.get("price").get("total").asDouble()).isEqualTo(537.54);
        assertThat(stock(book)).as("stock is only taken on payment").isEqualTo(stockBefore);

        // A declined card leaves the order retryable... as PAYMENT_FAILED, with no stock taken
        JsonNode declined = ok(payByCard(token, orderId, DECLINED_CARD));
        assertThat(declined.get("status").asText()).isEqualTo("FAILED");
        assertThat(declined.get("cardLast4").asText()).isEqualTo("0000");
        assertThat(ok(call(HttpMethod.GET, "/orders/" + orderId, token, null)).get("status").asText())
                .isEqualTo("PAYMENT_FAILED");
        assertThat(stock(book)).isEqualTo(stockBefore);
        assertThat(ok(call(HttpMethod.GET, "/cart", token, null)).get("itemCount").asInt()).isEqualTo(3);

        // Place again and pay successfully
        long orderId2 = placeOrder(token, Map.of("couponCode", "WELCOME10")).get("id").asLong();
        JsonNode paid = ok(payByCard(token, orderId2, GOOD_CARD));
        assertThat(paid.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(paid.get("amount").asDouble()).isEqualTo(537.54);
        assertThat(paid.get("cardLast4").asText()).isEqualTo("4242");
        assertThat(paid.toString()).doesNotContain(GOOD_CARD).doesNotContain("\"cvv\"");

        assertThat(stock(book)).isEqualTo(stockBefore - 2);
        assertThat(ok(call(HttpMethod.GET, "/cart", token, null)).get("itemCount").asInt()).isZero();
        assertThat(ok(call(HttpMethod.GET, "/users/me", token, null)).get("giftPoints").asInt()).isEqualTo(5);
        JsonNode points = ok(call(HttpMethod.GET, "/me/gift-points", token, null));
        assertThat(points.get("balance").asInt()).isEqualTo(5);
        assertThat(points.get("transactions").get("content").get(0).get("type").asText()).isEqualTo("EARN");

        // Paying twice is not possible
        assertThat(payByCard(token, orderId2, GOOD_CARD).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Order history, recommendations exclude what was bought
        JsonNode history = ok(call(HttpMethod.GET, "/orders", token, null));
        assertThat(history.get("totalElements").asInt()).isEqualTo(2);
        assertThat(history.get("content").get(0).get("id").asLong()).isEqualTo(orderId2);
        JsonNode recs = ok(call(HttpMethod.GET, "/recommendations?limit=10", token, null));
        recs.forEach(b -> assertThat(b.get("id").asLong()).isNotIn(book, book2));
        assertThat(recs.size()).isGreaterThan(0);

        // Cancel inside the 48 h window: stock back, points reversed, payment refunded
        JsonNode beforeCancel = ok(call(HttpMethod.GET, "/orders/" + orderId2, token, null));
        assertThat(beforeCancel.get("cancellable").asBoolean()).isTrue();
        JsonNode cancelled = ok(call(HttpMethod.POST, "/orders/" + orderId2 + "/cancel", token, null));
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("payment").get("status").asText()).isEqualTo("REFUNDED");
        assertThat(stock(book)).isEqualTo(stockBefore);
        assertThat(ok(call(HttpMethod.GET, "/users/me", token, null)).get("giftPoints").asInt()).isZero();
        assertThat(call(HttpMethod.POST, "/orders/" + orderId2 + "/cancel", token, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // Buy it again
        JsonNode reorder = ok(call(HttpMethod.POST, "/orders/" + orderId2 + "/reorder", token, null));
        assertThat(reorder.get("cart").get("itemCount").asInt()).isEqualTo(3);
        assertThat(reorder.get("skipped")).isEmpty();
    }

    @Test
    void cancellationIsRejectedAfter48Hours() {
        String token = register();
        long book = bookId("Salt, Fat, Heat");
        addToCart(token, book, 1);
        long orderId = placeOrder(token, Map.of()).get("id").asLong();
        assertThat(ok(payByCard(token, orderId, GOOD_CARD)).get("status").asText()).isEqualTo("SUCCESS");
        int stockAfterPayment = stock(book);

        tx.executeWithoutResult(s -> {
            Order o = orderRepository.findById(orderId).orElseThrow();
            o.setCreatedAt(Instant.now().minus(49, ChronoUnit.HOURS));
        });

        JsonNode order = ok(call(HttpMethod.GET, "/orders/" + orderId, token, null));
        assertThat(order.get("cancellable").asBoolean()).isFalse();
        var r = call(HttpMethod.POST, "/orders/" + orderId + "/cancel", token, null);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().get("message").asText()).contains("48 hours");
        assertThat(stock(book)).isEqualTo(stockAfterPayment);
    }

    @Test
    void giftPointsAreReservedRedeemedAndRestored() {
        String token = register();
        long book = bookId("Salt, Fat, Heat"); // 499 -> subtotal >= 500? no: 499 + 40 delivery + 59.88 tax
        addToCart(token, book, 3);              // 1497 -> free delivery, tax 179.64, total 1676.64
        long orderId = placeOrder(token, Map.of()).get("id").asLong();
        ok(payByCard(token, orderId, GOOD_CARD));
        int earned = ok(call(HttpMethod.GET, "/users/me", token, null)).get("giftPoints").asInt();
        assertThat(earned).isEqualTo(16); // floor(1676.64 / 100)

        // Cannot redeem more than the balance
        addToCart(token, book, 1);
        var tooMany = call(HttpMethod.POST, "/checkout/quote", token, Map.of("pointsToRedeem", earned + 1));
        assertThat(tooMany.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode quote = ok(call(HttpMethod.POST, "/checkout/quote", token, Map.of("pointsToRedeem", 10)));
        assertThat(quote.get("pointsDiscount").asDouble()).isEqualTo(10.0);
        // 499 + 59.88 tax + 40 delivery(499 < 500) - 10
        assertThat(quote.get("total").asDouble()).isEqualTo(588.88);

        long order2 = placeOrder(token, Map.of("pointsToRedeem", 10)).get("id").asLong();
        assertThat(ok(call(HttpMethod.GET, "/users/me", token, null)).get("giftPoints").asInt()).isEqualTo(earned - 10);

        // Cancelling the unpaid order gives the points back
        ok(call(HttpMethod.POST, "/orders/" + order2 + "/cancel", token, null));
        assertThat(ok(call(HttpMethod.GET, "/users/me", token, null)).get("giftPoints").asInt()).isEqualTo(earned);

        // Failed payment also restores them
        long order3 = placeOrder(token, Map.of("pointsToRedeem", 5)).get("id").asLong();
        assertThat(ok(payByCard(token, order3, DECLINED_CARD)).get("status").asText()).isEqualTo("FAILED");
        assertThat(ok(call(HttpMethod.GET, "/users/me", token, null)).get("giftPoints").asInt()).isEqualTo(earned);
    }

    @Test
    void stockIsEnforced() {
        String token = register();
        long ebook = bookId("The Quiet Habit");
        addToCart(token, ebook, 50); // eBooks have no stock limit

        long scarce = bookId("Monsoon Hearts"); // stock 12
        var tooMany = call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", scarce, "quantity", 13));
        assertThat(tooMany.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", 999999)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "/cart/items", token, Map.of("bookId", scarce, "quantity", 0))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ordersAreScopedToTheirOwner() {
        String alice = register();
        String bob = register();
        addToCart(alice, bookId("Offline"), 1);
        long orderId = placeOrder(alice, Map.of()).get("id").asLong();
        assertThat(call(HttpMethod.GET, "/orders/" + orderId, bob, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(payByCard(bob, orderId, GOOD_CARD).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "/orders/" + orderId + "/cancel", bob, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void paymentInputIsValidated() {
        String token = register();
        addToCart(token, bookId("Offline"), 1);
        long orderId = placeOrder(token, Map.of()).get("id").asLong();
        assertThat(payByCard(token, orderId, "4242424242424241").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "/orders/" + orderId + "/payment", token,
                Map.of("method", "UPI", "upiId", "not-a-vpa")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "/orders/" + orderId + "/payment", token,
                Map.of("method", "CREDIT_CARD", "cardNumber", GOOD_CARD, "nameOnCard", "A", "cvv", "123",
                        "expiry", "01/2001")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var upi = call(HttpMethod.POST, "/orders/" + orderId + "/payment", token,
                Map.of("method", "UPI", "upiId", "name@bank"));
        assertThat(ok(upi).get("status").asText()).isEqualTo("SUCCESS");
    }

    @Test
    void wishlistWritersAddressesAndReviews() {
        String token = register();
        long book = bookId("Offline");

        assertThat(call(HttpMethod.PUT, "/wishlist/" + book, token, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(call(HttpMethod.PUT, "/wishlist/" + book, token, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(ok(call(HttpMethod.GET, "/wishlist", token, null))).hasSize(1);
        assertThat(ok(call(HttpMethod.GET, "/books/" + book, token, null)).get("wishlisted").asBoolean()).isTrue();
        call(HttpMethod.DELETE, "/wishlist/" + book, token, null);
        assertThat(ok(call(HttpMethod.GET, "/wishlist", token, null))).isEmpty();

        long authorId = ok(call(HttpMethod.GET, "/books/" + book, null, null)).get("author").get("id").asLong();
        assertThat(call(HttpMethod.PUT, "/me/writers/" + authorId, token, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(ok(call(HttpMethod.GET, "/me/writers", token, null))).hasSize(1);
        assertThat(ok(call(HttpMethod.GET, "/authors/" + authorId, token, null)).get("followed").asBoolean()).isTrue();
        call(HttpMethod.DELETE, "/me/writers/" + authorId, token, null);
        assertThat(ok(call(HttpMethod.GET, "/me/writers", token, null))).isEmpty();

        JsonNode a1 = ok(call(HttpMethod.POST, "/addresses", token, address()));
        assertThat(a1.get("isDefault").asBoolean()).isTrue();
        var second = new java.util.HashMap<>(address());
        second.put("isDefault", true);
        JsonNode a2 = ok(call(HttpMethod.POST, "/addresses", token, second));
        JsonNode list = ok(call(HttpMethod.GET, "/addresses", token, null));
        assertThat(list).hasSize(2);
        assertThat(list.get(0).get("isDefault").asBoolean()).isFalse();
        assertThat(list.get(1).get("isDefault").asBoolean()).isTrue();
        var bad = new java.util.HashMap<>(address());
        bad.put("phone", "abc");
        var r = call(HttpMethod.POST, "/addresses", token, bad);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("fieldErrors").get(0).get("field").asText()).isEqualTo("phone");

        // Use a saved address at checkout, then delete it: the order keeps its snapshot
        addToCart(token, book, 1);
        var placed = call(HttpMethod.POST, "/orders", token, Map.of("addressId", a2.get("id").asLong()));
        long orderId = ok(placed).get("id").asLong();
        assertThat(call(HttpMethod.DELETE, "/addresses/" + a2.get("id").asLong(), token, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(ok(call(HttpMethod.GET, "/orders/" + orderId, token, null)).get("shippingAddress")
                .get("city").asText()).isEqualTo("Bengaluru");
        // address and addressId together (or neither) is a bad request
        assertThat(call(HttpMethod.POST, "/orders", token, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(call(HttpMethod.POST, "/books/" + book + "/reviews", token, Map.of("rating", 6)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "/books/" + book + "/reviews", token,
                Map.of("rating", 5, "comment", "x".repeat(101))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode review = ok(call(HttpMethod.POST, "/books/" + book + "/reviews", token,
                Map.of("rating", 4, "comment", "Loved it")));
        assertThat(review.get("userName").asText()).isEqualTo("Test User");
        assertThat(call(HttpMethod.POST, "/books/" + book + "/reviews", token, Map.of("rating", 5)).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        JsonNode detail = ok(call(HttpMethod.GET, "/books/" + book, null, null));
        assertThat(detail.get("averageRating").asDouble()).isEqualTo(4.0);
        assertThat(detail.get("reviewCount").asInt()).isEqualTo(1);
        assertThat(ok(call(HttpMethod.GET, "/books/" + book + "/reviews", null, null)).get("content")).hasSize(1);
    }

    @Test
    void authValidation() {
        String email = "dup" + UUID.randomUUID().toString().substring(0, 6) + "@example.com";
        var body = Map.of("email", email, "password", "password123", "firstName", "A", "lastName", "B");
        assertThat(call(HttpMethod.POST, "/auth/register", null, body).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(call(HttpMethod.POST, "/auth/register", null, body).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.POST, "/auth/register", null,
                Map.of("email", "nope", "password", "short", "firstName", "A", "lastName", "B")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "/auth/login", null, Map.of("email", email, "password", "wrong"))
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var login = call(HttpMethod.POST, "/auth/login", null, Map.of("email", email, "password", "password123"));
        assertThat(ok(login).get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(login.getBody().get("user").has("passwordHash")).isFalse();
    }
}
