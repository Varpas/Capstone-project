# Testing

The backend has three layers of automated checks. All of them run without Docker or PostgreSQL.

| Layer | What it proves | Where | Count |
|---|---|---|---|
| Unit tests | Business rules are correct in isolation | `backend/src/test/java/com/bookworm/service`, `.../config` | 30 |
| API integration tests | Features work end to end over real HTTP against a real (in-memory) database | `ApiIntegrationTest` | 10 |
| Validation and error-handling tests | Bad input, bad auth and bad state are refused with the right status and a safe, consistent error body | `ValidationAndErrorHandlingTest` | 24 |
| Smoke script | The same journey against a **running** server, including the Docker stack | `scripts/smoke-test.sh` | 66 checks |

**Latest result: 64 automated tests, 0 failures, 0 errors, 0 skipped. Smoke script: 66 of 66 checks passed.**

## How to run

```bash
# 1. Automated tests (JUnit 5, Spring Boot Test, H2 in PostgreSQL mode, Flyway migrations and seed data)
cd backend && mvn test

# 2. Smoke test against a running server (needs curl and jq)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=h2        # terminal 1
./scripts/smoke-test.sh                                                  # terminal 2, from the repo root
VERBOSE=1 ./scripts/smoke-test.sh                                        # also print every request and response
BASE_URL=http://localhost:8080/api/v1 ./scripts/smoke-test.sh            # e.g. against docker compose
```

The smoke script exits with a non-zero code if any check fails, so it can be used in a pipeline.

## What each test class covers

### Unit tests

| Class | Tests | Covered |
|---|---|---|
| `PricingServiceTest` | 14 | Tax with half-up rounding; delivery fee below 500 and free at exactly 500; empty cart costs 0; percent and fixed coupons; coupon minimum order; trimmed codes; coupon capped at the order value; unknown, expired, inactive and not-yet-valid coupons; blank coupon; points redeemed 1:1; points over balance; points over payable amount; points earned per 100 of the total |
| `PaymentSimulatorTest` | 9 | Luhn check; missing card fields; invalid and expired cards; debit cards; UPI id format (valid and 5 invalid forms); wallet id; decline on `0000`; unique transaction references; the card number is not exposed in the result |
| `JwtServiceTest` | 5 | Token round trip; tampered payload; token signed with another key; expired token; garbage and blank tokens |
| `MoneyTest` | 2 | Half-up rounding; zero has 2 decimals |

### `ApiIntegrationTest` (feature behaviour)

| Test | Covered |
|---|---|
| `guestsCanBrowseButNotUseCart` | Public catalogue; home sections; cart gives 401 without a token and with a bad one |
| `searchFilterSortAndDetail` | Sort, format and price filters; book detail; related books; 404 and 400 cases |
| `fullJourneyWithCouponPointsCancelAndReorder` | Cart, quote, coupon, place, **declined then successful payment**, stock taken, points earned, history, recommendations, cancel with refund, reorder |
| `cancellationIsRejectedAfter48Hours` | The order is aged by 49 hours in the database; cancel gives 409 and stock is unchanged |
| `giftPointsAreReservedRedeemedAndRestored` | Points reserved on order, restored on cancel and on payment failure |
| `stockIsEnforced` | Stock limit on cart (409); eBooks unlimited; unknown book 404; quantity 0 is 400 |
| `ordersAreScopedToTheirOwner` | A second user gets 404 on read, pay and cancel |
| `paymentInputIsValidated` | Luhn, UPI and expiry checks; UPI success |
| `wishlistWritersAddressesAndReviews` | Wishlist, followed authors, addresses (default handling, phone validation), order keeps its address copy, reviews (range, length, duplicates, average rating) |
| `authValidation` | Duplicate e-mail, invalid body, wrong password, token type, no password hash in the response |

### `ValidationAndErrorHandlingTest` (error behaviour)

Every assertion also checks the **error envelope**: `status`, `error`, a non-empty `message`, a `timestamp`, and that no
exception class name or stack trace appears in the body.

| Test | Covered |
|---|---|
| `registerReportsEveryInvalidField` | All four invalid fields are reported together in `fieldErrors` |
| `registerEnforcesFieldLengths` | Max length of password and names |
| `loginRequiresEmailAndPassword` | Missing fields |
| `malformedAndEmptyJsonBodiesAreBadRequests` | Broken JSON, empty body, array instead of object, wrong types |
| `addressFieldsAreValidated` | Phone, country code, e-mail, blank city and missing pin code in one request |
| `nestedAddressInsideAnOrderIsValidated` | Error path is `address.phone` |
| `cartQuantityRules` | 0, negative and missing `bookId` |
| `checkoutRequestRules` | Negative points, over-long coupon, unknown coupon, points the user does not have |
| `orderNeedsExactlyOneOfAddressIdAndAddress` | Neither, both, and an unknown `addressId` (404) |
| `reviewRules` | Rating 0 and 6, missing rating, long comment, unknown book |
| `queryParametersAreValidated` | Negative price, page size 0 and 101, negative page, min over max, bad enum, bad id type, bad sort, home limit |
| `orderListFilterRejectsUnknownStatus` | `status=SHIPPED` |
| `paymentFieldsAreValidated` | Missing method, unknown method, bad card fields, Luhn, expiry, UPI, wallet; the order stays `PENDING_PAYMENT` |
| `protectedEndpointsRejectMissingMalformedAndForgedTokens` | 13 protected endpoints x 3 kinds of bad token = 39 checks, all 401 |
| `publicEndpointsWorkWithoutAToken` | 9 public endpoints |
| `aBadTokenOnAPublicEndpointDoesNotBreakIt` | Stale token on a public page |
| `loginDoesNotRevealWhetherTheEmailExists` | Same message for unknown user and wrong password |
| `unknownResourcesAre404` | 12 different missing-resource cases |
| `wrongHttpMethodIs405` | Three examples |
| `stateConflictsAre409` | Empty cart, double payment, double cancel, paying a cancelled order |
| `duplicateRegistrationAndDuplicateReviewAre409` | Includes upper-case e-mail |
| `usersCannotSeeOrChangeEachOthersData` | Cart, orders, addresses (read, update, delete), order payment, cancel and reorder across two users |
| `sensitiveDataNeverLeavesTheServer` | No password or hash; no card number or CVV in the payment or order response |
| `serverIgnoresClientSuppliedPrices` | A request with `total: 0.01` still gets the real price |

## Requirement to test traceability

| Requirement | Test |
|---|---|
| Browse and search without logging in | `guestsCanBrowseButNotUseCart`, `searchFilterSortAndDetail` |
| Login and register | `authValidation`, `registerReportsEveryInvalidField` |
| Cart and stock limits | `stockIsEnforced`, `cartQuantityRules` |
| Coupons | `PricingServiceTest`, `fullJourney...`, `checkoutRequestRules` |
| Tax, delivery, totals | `PricingServiceTest`, `fullJourney...` |
| Gift points | `PricingServiceTest`, `giftPointsAreReservedRedeemedAndRestored` |
| Checkout address | `wishlistWritersAddressesAndReviews`, `nestedAddressInsideAnOrderIsValidated` |
| Simulated payment, success and failure | `PaymentSimulatorTest`, `fullJourney...`, `paymentFieldsAreValidated` |
| Order history and "Buy it again" | `fullJourney...` |
| Cancel within 48 hours | `fullJourney...`, `cancellationIsRejectedAfter48Hours` |
| Wishlist and My Writers | `wishlistWritersAddressesAndReviews` |
| Reviews | `wishlistWritersAddressesAndReviews`, `reviewRules` |
| Per-user data isolation | `ordersAreScopedToTheirOwner`, `usersCannotSeeOrChangeEachOthersData` |
| No sensitive data exposed | `sensitiveDataNeverLeavesTheServer`, `PaymentSimulatorTest` |

## Smoke script sections

`scripts/smoke-test.sh` makes real `curl` calls and prints PASS or FAIL for each check:

1. Health and public catalogue (5)
2. Authentication (5)
3. Cart (4)
4. Checkout quote, coupon and gift points (7)
5. Place order and pay, including a declined card (10)
6. Cancel, refund and buy it again (5)
7. Validation errors (16)
8. Authentication and authorisation errors (8)
9. Not found and method errors (5)
10. Stock rules (1)

## Manual Curl Test Cases (Live Demo)

Targeted, copy-pasteable `curl` commands extracted from [`docs/demo-script.md`](demo-script.md) for live API demonstration, error-handling validation, and contract verification against a running backend. These are the manual, interactive counterparts to the automated assertions in [`scripts/smoke-test.sh`](../scripts/smoke-test.sh).

### Environment Setup & Authentication Token

Set the base API URL and register a temporary user to obtain a valid JWT token (`$T`):

```bash
# Base URL (default local Spring Boot port)
B=http://localhost:8080/api/v1

# Register a new user and capture the JWT access token
T=$(curl -s -X POST $B/auth/register \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"demo$RANDOM@example.com\",\"password\":\"password123\",\"firstName\":\"Demo\",\"lastName\":\"User\"}" | jq -r .accessToken)

# Inspect the captured token
echo "Token: $T"
```

### 1. Request Body Validation & Field Errors
* **Endpoint**: `POST /api/v1/auth/register`
* **Test Purpose**: Verifies that the API enforces schema validation on incoming request bodies, returns `400 Bad Request` with structured `fieldErrors` detailing each failing constraint (invalid email format, short password, missing required fields), and never leaks stack traces.
* **Command**:
  ```bash
  curl -s -X POST $B/auth/register \
    -H 'Content-Type: application/json' \
    -d '{"email":"nope","password":"short"}' | jq
  ```
* **Expected Output**:
  ```json
  {
    "timestamp": "2026-11-23T18:00:35.006750Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "fieldErrors": [
      { "field": "firstName", "message": "must not be blank" },
      { "field": "email", "message": "must be a well-formed email address" },
      { "field": "password", "message": "size must be between 8 and 72" },
      { "field": "lastName", "message": "must not be blank" }
    ]
  }
  ```

### 2. Missing Authentication & Protected Endpoint Guard
* **Endpoint**: `GET /api/v1/cart`
* **Test Purpose**: Verifies that protected user endpoints require a valid `Bearer` authorization token and immediately respond with `401 Unauthorized` when requested without credentials.
* **Command**:
  ```bash
  curl -s -i $B/cart | head -1
  ```
* **Expected Output**:
  ```http
  HTTP/1.1 401
  ```

### 3. Query Parameter Range & Cross-Field Validation
* **Endpoint**: `GET /api/v1/books`
* **Test Purpose**: Verifies that search and filter query parameters are validated against relational domain constraints (minimum price cannot exceed maximum price) and return a clear `400 Bad Request`.
* **Command**:
  ```bash
  curl -s "$B/books?minPrice=500&maxPrice=100" | jq
  ```
* **Expected Output**:
  ```json
  {
    "timestamp": "2026-11-23T18:00:35.030527Z",
    "status": 400,
    "error": "Bad Request",
    "message": "minPrice must not be greater than maxPrice"
  }
  ```

### 4. Expired Promotion & Coupon Rule Enforcement
* **Endpoint**: `POST /api/v1/checkout/quote`
* **Test Purpose**: Verifies that the server-side pricing engine validates coupon status before applying discounts and returns `400 Bad Request` if a coupon has expired.
* **Prerequisites**: Cart must contain at least one item before requesting a checkout quote.
* **Command**:
  ```bash
  # Step A: Add a book to the user's cart
  curl -s -X POST $B/cart/items \
    -H "Authorization: Bearer $T" \
    -H 'Content-Type: application/json' \
    -d '{"bookId":1,"quantity":1}' > /dev/null

  # Step B: Request quote with expired coupon
  curl -s -X POST $B/checkout/quote \
    -H "Authorization: Bearer $T" \
    -H 'Content-Type: application/json' \
    -d '{"couponCode":"EXPIRED20"}' | jq
  ```
* **Expected Output**:
  ```json
  {
    "timestamp": "2026-11-23T18:00:35.062126Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Coupon 'EXPIRED20' has expired"
  }
  ```

### 5. Inventory Stock & Concurrency Limits
* **Endpoint**: `POST /api/v1/cart/items`
* **Test Purpose**: Verifies that cart operations check available warehouse stock and return `409 Conflict` when a requested quantity exceeds inventory on hand.
* **Command**:
  ```bash
  curl -s -X POST $B/cart/items \
    -H "Authorization: Bearer $T" \
    -H 'Content-Type: application/json' \
    -d '{"bookId":1,"quantity":9999}' | jq
  ```
* **Expected Output**:
  ```json
  {
    "timestamp": "2026-11-23T18:00:35.077724Z",
    "status": 409,
    "error": "Conflict",
    "message": "Only 40 copies of 'Joy of Minimalism' are in stock"
  }
  ```

## Defects found while testing

- Both defects in this round were in the tests, not the application. A review is created with `201`, which matches
  `openapi.yaml`, and the smoke script's first run compared money values as text (`527.00` against `527`). Both were corrected in the tests.
- No application defects were found by the new tests. They confirm that the existing validation and error handling
  behave as `openapi.yaml` describes.

## Known gaps

- Payment is simulated, so there is no gateway contract test.
- No load or concurrency test. The code uses row locks for concurrent payment, and this is described in
  [`api-guide.md`](api-guide.md#8-security-model), but it is not exercised by a multi-threaded test.
- No browser (end-to-end UI) tests. The frontend is checked by hand against the wireframes.
- No coverage report is configured.
