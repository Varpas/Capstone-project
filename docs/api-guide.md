# Book Worm API guide

A readable companion to [`openapi.yaml`](openapi.yaml). The OpenAPI file is the machine-readable contract. This guide adds
the rules, worked examples and error behaviour that are hard to see in a schema. Every example below is a real
request and response from the running backend, with long values shortened.

- Base URL: `http://localhost:8080/api/v1`
- Format: JSON in and out (`Content-Type: application/json`)
- Money: INR, numbers with 2 decimals
- Auth: `Authorization: Bearer <accessToken>`
- Try it: [`scripts/smoke-test.sh`](../scripts/smoke-test.sh) calls every flow below and checks the results.

## Contents
1. [Conventions](#1-conventions)
2. [Endpoint summary](#2-endpoint-summary)
3. [Walkthrough with examples](#3-walkthrough-with-examples)
4. [Business rules](#4-business-rules)
5. [Validation rules](#5-validation-rules)
6. [Error handling](#6-error-handling)
7. [Order and payment state machine](#7-order-and-payment-state-machine)
8. [Security model](#8-security-model)

---

## 1. Conventions

### Authentication
Register or log in to get a JWT. It is valid for 3600 seconds (`expiresIn`). Send it on every protected call:

```
Authorization: Bearer eyJhbGciOiJIUzM4NCJ9...
```

Public endpoints (no token needed): `/auth/*`, `GET /home`, `GET /categories`, `GET /publishers`, `GET /books/**`,
`GET /authors/{id}` and `GET /actuator/health`. An invalid token on a public endpoint is ignored and the call still
works. On a protected endpoint it gives `401`.

### Pagination
Paged endpoints take `page` (from 0, default 0) and `size` (1 to 100, default 20) and return:

```json
{ "page": 0, "size": 1, "totalElements": 30, "totalPages": 30, "content": [ ... ] }
```

### Status codes used

| Code | Meaning in this API |
|---|---|
| `200` | Success. Includes a **declined payment**, which is a normal outcome (`status: FAILED`) and not an HTTP error |
| `201` | Created: register, address, order, review |
| `204` | Success with no body: delete address, wishlist and writers changes, clear cart |
| `400` | Invalid input: field validation, malformed JSON, bad query parameter, bad coupon, bad payment details |
| `401` | Missing, invalid or expired token, or wrong login |
| `404` | Resource does not exist **or belongs to another user** |
| `405` | Method not allowed on this path |
| `409` | Valid request, but the current state does not allow it (stock, empty cart, duplicate, wrong order status) |
| `500` | Unexpected server error. The body never contains a stack trace |

---

## 2. Endpoint summary

`Auth` column: **P** = public, **T** = token required.

| Method | Path | Auth | Success | Purpose |
|---|---|---|---|---|
| POST | `/auth/register` | P | 201 | Create an account and get a token |
| POST | `/auth/login` | P | 200 | Log in and get a token |
| GET | `/users/me` | T | 200 | Current user and gift-point balance |
| GET | `/home?limit=` | P | 200 | `recommended`, `bestsellers`, `newLaunches` (limit 1 to 20, default 6) |
| GET | `/categories` | P | 200 | All categories |
| GET | `/publishers` | P | 200 | All publishers |
| GET | `/books` | P | 200 | Search, filter, sort and page the catalogue |
| GET | `/books/{bookId}` | P | 200 | Book detail |
| GET | `/books/{bookId}/related?limit=` | P | 200 | Related books (limit 1 to 20, default 3) |
| GET | `/authors/{authorId}` | P | 200 | Author profile. `followed` is included when logged in |
| GET | `/books/{bookId}/reviews` | P | 200 | Reviews of a book (paged) |
| POST | `/books/{bookId}/reviews` | T | 201 | Review a book (one per user per book) |
| GET | `/cart` | T | 200 | The cart |
| DELETE | `/cart` | T | 204 | Empty the cart |
| POST | `/cart/items` | T | 200 | Add a book. Quantities add up |
| PUT | `/cart/items/{bookId}` | T | 200 | Set the quantity |
| DELETE | `/cart/items/{bookId}` | T | 200 | Remove a book. Returns the cart |
| GET | `/wishlist` | T | 200 | Wishlist books |
| PUT | `/wishlist/{bookId}` | T | 204 | Add (safe to repeat) |
| DELETE | `/wishlist/{bookId}` | T | 204 | Remove |
| GET | `/me/writers` | T | 200 | Followed authors |
| PUT | `/me/writers/{authorId}` | T | 204 | Follow (safe to repeat) |
| DELETE | `/me/writers/{authorId}` | T | 204 | Unfollow |
| GET | `/addresses` | T | 200 | Saved addresses |
| POST | `/addresses` | T | 201 | Save an address |
| GET | `/addresses/{id}` | T | 200 | One address |
| PUT | `/addresses/{id}` | T | 200 | Replace an address |
| DELETE | `/addresses/{id}` | T | 204 | Delete (orders keep their own copy) |
| GET | `/recommendations?limit=` | T | 200 | Books based on order history, excluding books already bought (limit 1 to 50, default 6) |
| POST | `/checkout/quote` | T | 200 | Price the cart with an optional coupon and points |
| GET | `/orders?status=&page=&size=` | T | 200 | Order history, newest first |
| POST | `/orders` | T | 201 | Place an order from the cart (status `PENDING_PAYMENT`) |
| GET | `/orders/{orderId}` | T | 200 | One order |
| POST | `/orders/{orderId}/payment` | T | 200 | Pay (simulated) |
| POST | `/orders/{orderId}/cancel` | T | 200 | Cancel and refund |
| POST | `/orders/{orderId}/reorder` | T | 200 | "Buy it again": refill the cart from a past order |
| GET | `/me/gift-points?page=&size=` | T | 200 | Balance and ledger |
| GET | `/actuator/health` | P | 200 | Health check |

### `GET /books` query parameters

| Parameter | Type | Rules |
|---|---|---|
| `q` | text | Case-insensitive match on title, author name or description |
| `categoryId`, `publisherId`, `authorId` | number | Filters |
| `language` | text | e.g. `English` |
| `format` | enum | `PAPERBACK`, `HARDCOVER` or `EBOOK`. Other values give 400 |
| `minPrice`, `maxPrice` | number | `>= 0`. `minPrice` greater than `maxPrice` gives 400 |
| `sort` | text | `relevance` (default), `bestselling`, `price_asc`, `price_desc`, `newest`, `rating`. Other values give 400 |
| `page`, `size` | number | `page >= 0`, `1 <= size <= 100` |

---

## 3. Walkthrough with examples

The commands assume `B=http://localhost:8080/api/v1`.

### 3.1 Register

```bash
curl -X POST $B/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"asha@example.com","password":"password123","firstName":"Asha","lastName":"Rao"}'
```
`201 Created`
```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": 8, "email": "asha@example.com", "firstName": "Asha", "lastName": "Rao", "giftPoints": 0 }
}
```
Registering the same e-mail again (in any letter case) returns `409`. The password hash is never returned.

### 3.2 Search the catalogue

```bash
curl "$B/books?q=Joy&sort=price_asc&size=1"
```
`200 OK`
```json
{
  "page": 0, "size": 1, "totalElements": 1, "totalPages": 1,
  "content": [{
    "id": 1, "title": "Joy of Minimalism",
    "author": { "id": 1, "name": "Daniel Reed" },
    "format": "PAPERBACK",
    "categories": [{ "id": 7, "name": "Self-help" }, { "id": 21, "name": "Non-Fiction" }],
    "price": 149.00, "averageRating": 0.00, "inStock": true, "estimatedDeliveryDate": "2026-11-26"
  }]
}
```
`GET /books/1` adds `isbn`, `language`, `publisher`, `publishedDate`, `copiesSold`, `reviewCount`, `stock` and
`backCoverImageUrl`. When a valid token is sent, `wishlisted` is filled in.

### 3.3 Cart

```bash
curl -X POST $B/cart/items -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"bookId":1,"quantity":2}'
```
`200 OK`
```json
{ "items": [{ "book": { "id": 1, "title": "Joy of Minimalism", "price": 149.00 }, "quantity": 2, "lineTotal": 298.00 }],
  "itemCount": 2, "subtotal": 298.00 }
```
`quantity` defaults to 1. Adding a book that is already in the cart increases the quantity. The request fails with
`409` if the new total is more than the stock. eBooks have no stock limit.

### 3.4 Quote the price

```bash
curl -X POST $B/checkout/quote -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"couponCode":"WELCOME10","pointsToRedeem":0}'
```
`200 OK`
```json
{ "itemCount": 2, "subtotal": 298.00, "tax": 35.76, "deliveryCharge": 40.00,
  "couponCode": "WELCOME10", "couponDiscount": 29.80,
  "pointsRedeemed": 0, "pointsDiscount": 0.00, "total": 343.96, "pointsToEarn": 3 }
```
See [pricing](#41-pricing) for the formula. Both fields are optional.

### 3.5 Place the order

Send **exactly one** of `addressId` (a saved address) or `address` (a new one).

```bash
curl -X POST $B/orders -H "Authorization: Bearer $T" -H 'Content-Type: application/json' -d '{
  "couponCode": "WELCOME10",
  "address": { "firstName":"Asha","lastName":"Rao","email":"asha@example.com","phoneCountryCode":"+91",
               "phone":"9876543210","addressLine":"1 MG Road","city":"Bengaluru","state":"Karnataka",
               "pinCode":"560001","country":"India" },
  "saveAddress": false }'
```
`201 Created`
```json
{
  "id": 11, "status": "PENDING_PAYMENT",
  "items": [{ "bookId": 1, "title": "Joy of Minimalism", "authorName": "Daniel Reed", "quantity": 2,
              "unitPrice": 149.00, "lineTotal": 298.00, "estimatedDeliveryDate": "2026-11-26" }],
  "shippingAddress": { "firstName": "Asha", "phone": "+919876543210", "city": "Bengaluru", "pinCode": "560001" },
  "price": { "subtotal": 298.00, "tax": 35.76, "deliveryCharge": 40.00, "couponDiscount": 29.80, "total": 343.96, "pointsToEarn": 3 },
  "createdAt": "2026-11-23T16:24:28.457263Z",
  "cancellable": true, "cancellableUntil": "2026-11-25T16:24:28.457263Z"
}
```
Any client-supplied `total` or `subtotal` is ignored. The order stores a copy of the address and unit prices.
Stock is **not** taken yet. Gift points you chose to redeem are reserved.

### 3.6 Pay

```bash
curl -X POST $B/orders/11/payment -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"method":"CREDIT_CARD","cardNumber":"4242424242424242","nameOnCard":"Asha Rao","cvv":"123","expiry":"12/2099"}'
```
`200 OK`
```json
{ "orderId": 11, "method": "CREDIT_CARD", "status": "SUCCESS", "amount": 343.96,
  "cardLast4": "4242", "transactionRef": "TXN-20261123-20D0010C", "createdAt": "2026-11-23T16:24:28.475746Z" }
```

Fields per method:

| `method` | Required fields |
|---|---|
| `CREDIT_CARD`, `DEBIT_CARD` | `cardNumber` (13 to 19 digits, Luhn-valid), `nameOnCard`, `cvv` (3 or 4 digits), `expiry` (`MM/YYYY`, not in the past) |
| `UPI` | `upiId` such as `name@bank` |
| `WALLET` | `walletId` |

A card ending in `0000` is declined. This is still `200`:
```json
{ "orderId": 12, "method": "CREDIT_CARD", "status": "FAILED", "cardLast4": "0000",
  "failureReason": "Card declined by issuer", "transactionRef": "TXN-20261123-7C1E22AB" }
```
The order becomes `PAYMENT_FAILED`, reserved points are given back, and the cart is kept so the user can place
a new order. The full card number and CVV are never stored, logged or returned.

### 3.7 Cancel and reorder

```bash
curl -X POST $B/orders/11/cancel  -H "Authorization: Bearer $T"
curl -X POST $B/orders/11/reorder -H "Authorization: Bearer $T"
```
Cancel returns the order with `"status": "CANCELLED"`, `"cancellable": false` and the payment as `REFUNDED`.
Reorder returns:
```json
{ "cart": { "itemCount": 2, "subtotal": 298.00, "items": [ ... ] },
  "skipped": [{ "bookId": 5, "reason": "Out of stock" }] }
```
Reorder uses **current** prices and stock. Items it cannot add are listed in `skipped` instead of failing the call.

### 3.8 Gift points

```bash
curl $B/me/gift-points -H "Authorization: Bearer $T"
```
```json
{ "balance": 0,
  "transactions": { "page": 0, "size": 20, "totalElements": 2, "totalPages": 1, "content": [
    { "id": 9, "orderId": 11, "points": -3, "type": "REFUND", "createdAt": "2026-11-23T16:24:28.506812Z" },
    { "id": 8, "orderId": 11, "points":  3, "type": "EARN",   "createdAt": "2026-11-23T16:24:28.476660Z" } ] } }
```
Types: `EARN`, `REDEEM`, `REFUND`. Here the order was cancelled, so the 3 earned points were taken back.

### 3.9 Reviews

```bash
curl -X POST $B/books/1/reviews -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"rating":4,"comment":"Loved it"}'
```
`201 Created`
```json
{ "id": 1, "bookId": 1, "userName": "Asha Rao", "rating": 4, "comment": "Loved it", "createdAt": "2026-11-23T16:24:28.537805Z" }
```
A second review of the same book by the same user returns `409`. The book's `averageRating` and `reviewCount`
update right away.

---

## 4. Business rules

### 4.1 Pricing

All prices are computed on the server in [`PricingService`](../backend/src/main/java/com/bookworm/service/PricingService.java).
Rounding is 2 decimals, half up.

```
subtotal       = sum(unit price x quantity)
tax            = 12% of subtotal
delivery       = 0 if subtotal >= 500 (or the cart is empty), otherwise 40
gross          = subtotal + tax + delivery
couponDiscount = percent coupons: subtotal x percent.  fixed coupons: the fixed amount
                 capped so it never exceeds gross
pointsDiscount = pointsToRedeem x 1.00
total          = gross - couponDiscount - pointsDiscount     (never negative)
pointsToEarn   = floor(total / 100)
```

Worked example from the tests: 2 x 149.00 + 1 x 229.00 = 527.00; tax 63.24; delivery 0; `WELCOME10`
removes 52.70, so the total is **537.54** and the user earns **5** points.

### 4.2 Coupons (seeded)

| Code | Rule |
|---|---|
| `WELCOME10` | 10% off the subtotal, no minimum |
| `FLAT100` | 100 off, subtotal must be at least 500 |
| `EXPIRED20` | 20% off, expired in 2020. Always rejected, kept to test rejection |

A coupon is rejected with `400` and a clear message if it does not exist, is inactive, is not valid yet, has
expired, or the subtotal is below its minimum. Codes are matched ignoring case and surrounding spaces.

### 4.3 Gift points
- 1 point is worth 1 INR when redeemed. 1 point is earned per 100 INR of the **final total**.
- You cannot redeem more than your balance, or more than the amount left to pay (both `400`).
- Redeemed points are taken when the order is **placed**. They come back if payment fails or the order is cancelled.
- Earned points are added when payment **succeeds**. Cancelling a paid order takes them back.
- `users.gift_points` is a cached balance. The `gift_point_txns` ledger is the source of truth.

### 4.4 Stock
- Checked when adding to the cart, when quoting, and when placing the order.
- Taken only when payment **succeeds**. It is checked again at that moment under a database lock, so two buyers
  cannot both take the last copy. If it ran out, the payment is recorded as `FAILED`.
- Put back when a paid order is cancelled.

### 4.5 Cancellation
- `PENDING_PAYMENT`: can always be cancelled.
- `PAID`: only within **48 hours** of placing it (`bookworm.orders.cancel-window-hours`). After that, `409` with a
  message that names the window.
- `PAYMENT_FAILED` and `CANCELLED`: cannot be cancelled (`409`).
- Cancelling a paid order restores stock, marks the payment `REFUNDED` and reverses points.

---

## 5. Validation rules

Bean validation on the request records in [`Dto.java`](../backend/src/main/java/com/bookworm/dto/Dto.java). A broken
rule returns `400` with one `fieldErrors` entry per field.

| Request | Field | Rule |
|---|---|---|
| Register | `email` | required, valid e-mail, max 255 |
| | `password` | required, 8 to 72 characters |
| | `firstName`, `lastName` | required, max 100 |
| | `phone` | optional, max 20 |
| Login | `email`, `password` | required, `email` must be valid |
| Cart add | `bookId` | required |
| | `quantity` | optional (default 1), min 1 |
| Cart update | `quantity` | required, min 1 |
| Address | `firstName`, `lastName` | required, max 100 |
| | `email` | required, valid, max 255 |
| | `phoneCountryCode` | required, `+` and 1 to 4 digits, e.g. `+91` |
| | `phone` | required, 6 to 15 digits |
| | `addressLine` | required, max 255 |
| | `city`, `state`, `country` | required, max 100 |
| | `pinCode` | required, max 10 |
| Quote / order | `couponCode` | optional, max 30 |
| | `pointsToRedeem` | optional, min 0 |
| Order | `addressId` / `address` | exactly one of them |
| | `address` | validated as above. Errors are reported as `address.<field>` |
| Payment | `method` | required, one of `CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `WALLET` |
| | `cardNumber` | 13 to 19 digits, Luhn-valid |
| | `cvv` | 3 or 4 digits |
| | `expiry` | `MM/YYYY`, month 01 to 12, not in the past |
| | `nameOnCard` | max 100, required for cards |
| | `upiId` | `name@bank` format, required for UPI |
| | `walletId` | required for WALLET |
| Review | `rating` | required, 1 to 5 |
| | `comment` | optional, max 100 |

Query parameters (`page`, `size`, `limit`, `minPrice`, `maxPrice`, `sort`, `format`, `status`, ids) are checked
too and give the same `400` shape.

---

## 6. Error handling

### 6.1 One error shape

Every error, including the ones raised by Spring Security, uses this body
([`ErrorResponse`](../backend/src/main/java/com/bookworm/dto/ErrorResponse.java)):

```json
{ "timestamp": "2026-11-23T16:23:07.947897Z", "status": 400, "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": [ { "field": "email", "message": "must be a well-formed email address" } ] }
```
`fieldErrors` is only present for field validation errors. Stack traces and exception class names are never exposed.
Unexpected errors are logged on the server and returned as `500` with the message `Unexpected error`.

### 6.2 Real examples

| Situation | Status | `message` |
|---|---|---|
| Register with bad e-mail, short password and no names | 400 | `Validation failed` (4 field errors) |
| Broken JSON body | 400 | `Malformed request: unreadable request body` |
| `GET /books?format=PDF` | 400 | `Malformed request: invalid value for 'format'` |
| `GET /books/abc` | 400 | `Malformed request: invalid value for 'bookId'` |
| `GET /books?minPrice=-1&size=500` | 400 | `Validation failed` (`search.minPrice`, `search.size`) |
| `GET /books?sort=bogus` | 400 | `Unknown sort: bogus` |
| `POST /orders` with `{}` | 400 | `Provide exactly one of addressId or address` |
| Quote with `EXPIRED20` | 400 | `Coupon 'EXPIRED20' has expired` |
| Quote with an unknown code | 400 | `Coupon 'NOPE' does not exist` |
| Redeem more points than owned | 400 | `You only have 0 gift points` |
| Card number fails Luhn | 400 | `Card number is invalid` |
| Card expiry in the past | 400 | `Card has expired` |
| No token, bad token or expired token | 401 | `Missing, invalid or expired token` |
| Wrong password or unknown e-mail | 401 | `Invalid e-mail or password` (same text for both, to stop account probing) |
| Another user's order, address or cart item | 404 | `Order not found` |
| Unknown book | 404 | `Book not found` |
| Unknown URL | 404 | `Resource not found` |
| `DELETE /categories` | 405 | `Request method 'DELETE' is not supported` |
| Register a taken e-mail | 409 | `An account with this e-mail already exists` |
| Add 9999 copies of a book with 40 in stock | 409 | `Only 40 copies of 'Joy of Minimalism' are in stock` |
| Quote or order with an empty cart | 409 | `Your cart is empty` |
| Pay an order that is already paid | 409 | `Order is not awaiting payment (status PAID)` |
| Cancel a cancelled order | 409 | `Order cannot be cancelled (status CANCELLED)` |
| Cancel a paid order after 48 hours | 409 | `Orders can only be cancelled within 48 hours of placing them` |
| Review the same book twice | 409 | `You have already reviewed this book` |

### 6.3 Why 404 for other people's data
Requests for orders, addresses and cart items that belong to someone else return `404`, not `403`. This way the API
does not confirm that an id exists.

---

## 7. Order and payment state machine

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT: POST /orders
    PENDING_PAYMENT --> PAID: payment SUCCESS (stock taken, points earned, cart cleared)
    PENDING_PAYMENT --> PAYMENT_FAILED: payment FAILED (points returned)
    PENDING_PAYMENT --> CANCELLED: cancel (points returned)
    PAID --> CANCELLED: cancel within 48 h (stock back, REFUNDED, points reversed)
    PAYMENT_FAILED --> [*]
    CANCELLED --> [*]
```

`PAYMENT_FAILED` is final. To try again, the user places a new order from the cart, which is still full.

| Payment `status` | Meaning |
|---|---|
| `SUCCESS` | Charged |
| `FAILED` | Declined, or the stock ran out before the charge |
| `REFUNDED` | Charged, then the order was cancelled |

---

## 8. Security model

| Concern | How it is handled |
|---|---|
| Passwords | BCrypt hash, never returned |
| Sessions | Stateless JWT (HS384), 1 hour, signed with `JWT_SECRET` |
| Authorisation | Every cart, order, address, wishlist, review and points query is filtered by the token's user id |
| Price tampering | Prices come from the database. Totals in a request are ignored |
| Payment data | Only the last 4 card digits are stored. Number and CVV are never stored, logged or returned |
| Concurrency | Row locks on the user, order and book rows stop double payment, overspent points and overselling |
| CORS | Only the origins in `bookworm.cors.allowed-origins` |
| Errors | One safe body, no internals |

Known limits (intended for this capstone): payment is simulated, there are no refresh tokens or roles, and there is
no rate limiting.
