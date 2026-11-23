# Book Worm backend

Spring Boot 3 / Java 17+ / PostgreSQL (Flyway). Implements `docs/openapi.yaml`.
Base path: `/api/v1`.

## Run

With PostgreSQL (defaults: `jdbc:postgresql://localhost:5432/bookworm`, user and password `bookworm`):

```
mvn spring-boot:run
```

Without PostgreSQL, using an in-memory H2 database (development only):

```
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

With Docker: `docker compose up --build` from the repository root.

Environment variables: `PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 bytes).

Flyway creates the schema (`V1`) and loads seed data (`V2`): 21 categories, 12 authors, 5 publishers, 30 books and the
coupons `WELCOME10` (10%), `FLAT100` (₹100 off ₹500+) and `EXPIRED20` (expired, for testing rejection).

## Behaviour worth knowing

- **Pricing** is computed only on the server: `total = subtotal + 12% tax + delivery (₹40, free from ₹500) - coupon - points`.
  All rates are in `application.yml` under `bookworm.pricing`.
- **Gift points**: 1 point = ₹1 when redeemed, 1 point earned per ₹100 of the total. Points are reserved when the
  order is placed, and restored if the payment fails or the order is cancelled.
- **Payment is simulated**. A card number ending in `0000` is declined (for example `4000000000020000`, which passes
  the Luhn check). A failed payment returns HTTP 200 with `status: FAILED` and the order becomes `PAYMENT_FAILED`.
  Card numbers and CVVs are never stored. Only the last four digits are kept.
- **Stock** is checked when adding to the cart and when placing the order, and taken when payment succeeds.
- **Cancellation**: a `PAID` order can be cancelled within 48 hours (`bookworm.orders.cancel-window-hours`).
  This restores stock, refunds the payment and reverses points. A `PENDING_PAYMENT` order can always be cancelled.
- **Buy it again** (`POST /orders/{id}/reorder`) adds the items at current prices and lists any it had to skip.

## Layout

```
config/      security, JWT, typed properties
domain/      JPA entities
repository/  Spring Data repositories
service/     business logic (PricingService, OrderService, PaymentSimulator, ...)
web/         REST controllers
dto/         request and response records
exception/   ApiException and the global error handler
```
