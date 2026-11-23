# Demo video script

A suggested order for a 8 to 10 minute screen recording. It is built to show the things a reviewer needs to see:
how Bob was used, the API design, that the app works, and that validation and error handling are real.

Before recording, run `docker compose up --build` (or the H2 option in the README), then check
http://localhost:8080/api/v1/actuator/health shows `UP`.

| Time | Show | Say |
|---|---|---|
| 0:00 | Repository tree in the editor | What was built: Spring Boot REST API, PostgreSQL, React UI. Built with IBM Bob in Agent mode, spec first |
| 0:30 | `PLAN.md`, then `docs/backend-requirements.md` and `docs/frontend-requirements.md` | How Bob turned the wireframes (`recources/Slide-5.png` to `Slide-10.png`) into requirements, entities and assumptions |
| 1:30 | `docs/openapi.yaml` and `docs/api-guide.md` | The API was designed before the code. Show the endpoint table, the pricing formula and the error table in the guide. Mention the Redocly lint (`npx @redocly/cli lint docs/openapi.yaml`) |
| 2:30 | Backend code: `Dto.java` validation annotations, `PricingService`, `GlobalExceptionHandler` | Layering. Validation lives on the request records. All errors pass through one handler. The server owns every price |
| 3:30 | Terminal: `VERBOSE=1 ./scripts/smoke-test.sh` against the running app | Walk the journey live. Point at the 66 PASS lines and the final `Passed: 66 Failed: 0` |
| 5:30 | Terminal: a few hand-made `curl` calls (below) | Show a validation error with `fieldErrors`, a 401, a 409, a declined card |
| 7:00 | Terminal: `cd backend && mvn test` | 64 tests, 0 failures. Explain the three layers: unit, integration, validation and error handling. Open `docs/testing.md` |
| 8:00 | Browser at http://localhost:3000 | Log in, add to cart, apply `WELCOME10`, pay with `4000000000020000` (declined), then `4242424242424242` (success), see the order, cancel it |
| 9:00 | `README.md` "How it was built with Bob" | What Bob did in each phase, what was reviewed by hand, and the known limits |

## Hand-made curl calls for the live part

```bash
B=http://localhost:8080/api/v1

# 400 with per-field errors
curl -s -X POST $B/auth/register -H 'Content-Type: application/json' -d '{"email":"nope","password":"short"}' | jq

# 401 without a token
curl -s -i $B/cart | head -1

# 400 for a bad query value, and a 400 for an expired coupon (needs a token and a cart item)
curl -s "$B/books?minPrice=500&maxPrice=100" | jq
curl -s -X POST $B/checkout/quote -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"couponCode":"EXPIRED20"}' | jq

# 409 when asking for more copies than exist
curl -s -X POST $B/cart/items -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"bookId":1,"quantity":9999}' | jq
```

To get a token for `$T`:

```bash
T=$(curl -s -X POST $B/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"demo'$RANDOM'@example.com","password":"password123","firstName":"Demo","lastName":"User"}' | jq -r .accessToken)
```

## Points a reviewer should hear

- Which parts Bob generated and what you checked by hand.
- The API contract came first, and the code and the tests follow it.
- Totals are computed only on the server, and the tests prove a client cannot change them.
- Every error uses one JSON shape, and the tests check that no stack trace leaks.
- A declined payment is a normal `200` result with `status: FAILED`, and it releases reserved points.
- What is not covered: a real payment gateway, load tests and browser tests (see `docs/testing.md`).
