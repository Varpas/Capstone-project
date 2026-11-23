#!/usr/bin/env bash
# End-to-end smoke test for a RUNNING Book Worm backend. It walks the customer journey over real HTTP,
# then probes validation and error handling. Every check prints PASS or FAIL, and the exit code is non-zero
# if anything failed.
#
#   BASE_URL=http://localhost:8080/api/v1 ./scripts/smoke-test.sh
#   VERBOSE=1 ./scripts/smoke-test.sh      # also print each request and response body
#
# Requires: curl, jq. Start the backend first, for example:
#   cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=h2
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
VERBOSE="${VERBOSE:-0}"
PASS=0
FAIL=0
STATUS=""
BODY=""

command -v jq >/dev/null || { echo "jq is required"; exit 2; }

# req METHOD PATH [JSON_BODY] [TOKEN] -> sets STATUS and BODY
req() {
  local method="$1" path="$2" data="${3:-}" token="${4:-}"
  local args=(-s -o /tmp/bw_body -w '%{http_code}' -X "$method" "$BASE_URL$path" -H 'Content-Type: application/json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$data" ]] && args+=(-d "$data")
  STATUS=$(curl "${args[@]}")
  BODY=$(cat /tmp/bw_body)
  if [[ "$VERBOSE" == "1" ]]; then
    echo "    > $method $path ${data:+$data}"
    echo "    < $STATUS $BODY" | cut -c1-400
  fi
}

check() { # check "description" "actual" "expected"
  if [[ "$2" == "$3" ]]; then
    PASS=$((PASS + 1)); printf '  PASS  %s\n' "$1"
  else
    FAIL=$((FAIL + 1)); printf '  FAIL  %s (expected %s, got %s)\n' "$1" "$3" "$2"
    printf '        response: %s\n' "$(echo "$BODY" | cut -c1-300)"
  fi
}

jqv() { echo "$BODY" | jq -r "$1"; }
section() { printf '\n== %s\n' "$1"; }

# Probe the server before doing anything else.
if ! curl -sf "$BASE_URL/actuator/health" >/dev/null; then
  echo "Backend is not reachable at $BASE_URL. Start it first (see header of this script)."; exit 2
fi

EMAIL="smoke$(date +%s)$RANDOM@example.com"
ADDRESS='{"firstName":"Smoke","lastName":"Test","email":"smoke@example.com","phoneCountryCode":"+91","phone":"9876543210","addressLine":"1 MG Road","city":"Bengaluru","state":"Karnataka","pinCode":"560001","country":"India"}'

section "1. Health and public catalogue"
req GET /actuator/health;            check "health is UP" "$(jqv .status)" "UP"
req GET /categories;                 check "categories 200" "$STATUS" "200"
req GET "/books?sort=price_asc&size=3"; check "book search 200" "$STATUS" "200"
check "search returns the 30 seeded books" "$(jqv .totalElements)" "30"
req GET "/books?q=Joy%20of%20Minimalism"; BOOK1=$(jqv '.content[0].id')
req GET "/books?q=The%20Last%20Witness";  BOOK2=$(jqv '.content[0].id')
req GET "/books/$BOOK1";             check "book detail has publisher" "$(jqv .publisher.name)" "ABC Publishers"

section "2. Authentication"
req POST /auth/register "{\"email\":\"$EMAIL\",\"password\":\"password123\",\"firstName\":\"Smoke\",\"lastName\":\"Test\"}"
check "register 201" "$STATUS" "201"
TOKEN=$(jqv .accessToken)
req POST /auth/register "{\"email\":\"$EMAIL\",\"password\":\"password123\",\"firstName\":\"Smoke\",\"lastName\":\"Test\"}"
check "duplicate email 409" "$STATUS" "409"
req POST /auth/login "{\"email\":\"$EMAIL\",\"password\":\"password123\"}"
check "login 200" "$STATUS" "200"
check "token type is Bearer" "$(jqv .tokenType)" "Bearer"
req GET /users/me "" "$TOKEN";       check "users/me 200" "$STATUS" "200"

section "3. Cart"
req POST /cart/items "{\"bookId\":$BOOK1,\"quantity\":2}" "$TOKEN"
check "add item 200" "$STATUS" "200"
req POST /cart/items "{\"bookId\":$BOOK2,\"quantity\":1}" "$TOKEN"
check "cart item count is 3" "$(jqv .itemCount)" "3"
check "cart subtotal is 527.00" "$(jqv '.subtotal == 527')" "true"
req PUT "/cart/items/$BOOK1" '{"quantity":1}' "$TOKEN"
check "update quantity" "$(jqv .itemCount)" "2"
req PUT "/cart/items/$BOOK1" '{"quantity":2}' "$TOKEN"

section "4. Checkout quote, coupon and gift points"
req POST /checkout/quote '{"couponCode":"WELCOME10"}' "$TOKEN"
check "quote 200" "$STATUS" "200"
check "tax is 12% (63.24)" "$(jqv '.tax == 63.24')" "true"
check "coupon discount is 10% (52.70)" "$(jqv '.couponDiscount == 52.7')" "true"
check "total is 537.54" "$(jqv '.total == 537.54')" "true"
req POST /checkout/quote '{"couponCode":"EXPIRED20"}' "$TOKEN"
check "expired coupon 400" "$STATUS" "400"
req POST /checkout/quote '{"couponCode":"NOPE"}' "$TOKEN"
check "unknown coupon 400" "$STATUS" "400"
req POST /checkout/quote '{"pointsToRedeem":5}' "$TOKEN"
check "redeeming points you do not have 400" "$STATUS" "400"

section "5. Place order and pay"
req POST /orders "{\"couponCode\":\"WELCOME10\",\"address\":$ADDRESS}" "$TOKEN"
check "place order 201" "$STATUS" "201"
ORDER=$(jqv .id)
check "order is PENDING_PAYMENT" "$(jqv .status)" "PENDING_PAYMENT"
req POST "/orders/$ORDER/payment" '{"method":"CREDIT_CARD","cardNumber":"4000000000020000","nameOnCard":"Smoke Test","cvv":"123","expiry":"12/2099"}' "$TOKEN"
check "declined card returns FAILED" "$(jqv .status)" "FAILED"
req GET "/orders/$ORDER" "" "$TOKEN"
check "order moves to PAYMENT_FAILED" "$(jqv .status)" "PAYMENT_FAILED"
req POST /orders "{\"couponCode\":\"WELCOME10\",\"address\":$ADDRESS}" "$TOKEN"
ORDER=$(jqv .id)
req POST "/orders/$ORDER/payment" '{"method":"CREDIT_CARD","cardNumber":"4242424242424242","nameOnCard":"Smoke Test","cvv":"123","expiry":"12/2099"}' "$TOKEN"
check "valid card is SUCCESS" "$(jqv .status)" "SUCCESS"
check "only the last 4 digits come back" "$(jqv .cardLast4)" "4242"
check "response never echoes the card number" "$(echo "$BODY" | grep -c 4242424242424242)" "0"
req POST "/orders/$ORDER/payment" '{"method":"UPI","upiId":"a@bank"}' "$TOKEN"
check "paying a paid order 409" "$STATUS" "409"
req GET /cart "" "$TOKEN";           check "cart emptied after payment" "$(jqv .itemCount)" "0"
req GET /users/me "" "$TOKEN";       check "5 gift points earned" "$(jqv .giftPoints)" "5"

section "6. Cancel, refund and buy it again"
req POST "/orders/$ORDER/cancel" "" "$TOKEN"
check "cancel inside 48 h" "$(jqv .status)" "CANCELLED"
check "payment refunded" "$(jqv .payment.status)" "REFUNDED"
req POST "/orders/$ORDER/cancel" "" "$TOKEN"
check "cancelling twice 409" "$STATUS" "409"
req POST "/orders/$ORDER/reorder" "" "$TOKEN"
check "reorder refills cart (3 items)" "$(jqv .cart.itemCount)" "3"
req GET /orders "" "$TOKEN";         check "order history has 2 orders" "$(jqv .totalElements)" "2"

section "7. Validation errors (400, with field details)"
req POST /auth/register '{"email":"nope","password":"short"}'
check "register: invalid body 400" "$STATUS" "400"
check "register: message" "$(jqv .message)" "Validation failed"
check "register: reports the email field" "$(jqv '[.fieldErrors[].field] | contains(["email"])')" "true"
req POST /auth/register '{bad json'
check "malformed JSON 400" "$STATUS" "400"
req POST /cart/items "{\"bookId\":$BOOK1,\"quantity\":0}" "$TOKEN"
check "cart quantity 0 is 400" "$STATUS" "400"
req POST /addresses '{"firstName":"A"}' "$TOKEN"
check "address missing fields 400" "$STATUS" "400"
req POST /orders '{}' "$TOKEN"
check "order needs addressId or address" "$STATUS" "400"
req POST "/books/$BOOK1/reviews" '{"rating":6}' "$TOKEN"
check "review rating 6 is 400" "$STATUS" "400"
req GET "/books?minPrice=500&maxPrice=100"
check "minPrice > maxPrice 400" "$STATUS" "400"
req GET "/books?size=500"
check "page size over 100 is 400" "$STATUS" "400"
req GET "/books?format=PDF"
check "unknown enum value 400" "$STATUS" "400"
req GET /books/abc
check "non-numeric id 400" "$STATUS" "400"
req POST "/orders/$ORDER/payment" '{"method":"CREDIT_CARD","cardNumber":"4242424242424241","nameOnCard":"X","cvv":"123","expiry":"12/2099"}' "$TOKEN"
check "payment on a cancelled order 409" "$STATUS" "409"
req POST /orders "{\"address\":$ADDRESS}" "$TOKEN"
ORDER3=$(jqv .id)
req POST "/orders/$ORDER3/payment" '{"method":"CREDIT_CARD","cardNumber":"4242424242424241","nameOnCard":"X","cvv":"123","expiry":"12/2099"}' "$TOKEN"
check "Luhn-invalid card 400" "$STATUS" "400"
req POST "/orders/$ORDER3/payment" '{"method":"CREDIT_CARD","cardNumber":"4242424242424242","nameOnCard":"X","cvv":"123","expiry":"01/2001"}' "$TOKEN"
check "expired card 400" "$STATUS" "400"
req POST "/orders/$ORDER3/payment" '{"method":"UPI","upiId":"not-a-vpa"}' "$TOKEN"
check "invalid UPI id 400" "$STATUS" "400"

section "8. Authentication and authorisation errors (401, 404)"
req GET /cart;                       check "cart without token 401" "$STATUS" "401"
req GET /cart "" "garbage";          check "cart with bad token 401" "$STATUS" "401"
req GET /orders;                     check "orders without token 401" "$STATUS" "401"
req POST /auth/login '{"email":"nobody@example.com","password":"password123"}'
check "unknown user 401" "$STATUS" "401"
req POST /auth/login "{\"email\":\"$EMAIL\",\"password\":\"wrong-password\"}"
check "wrong password 401" "$STATUS" "401"
check "login does not reveal which part was wrong" "$(jqv .message)" "Invalid e-mail or password"
req POST /auth/register "{\"email\":\"other$RANDOM$(date +%s)@example.com\",\"password\":\"password123\",\"firstName\":\"O\",\"lastName\":\"T\"}"
OTHER=$(jqv .accessToken)
req GET "/orders/$ORDER" "" "$OTHER"
check "another user's order is 404, not 403" "$STATUS" "404"
req POST "/orders/$ORDER/cancel" "" "$OTHER"
check "another user cannot cancel it" "$STATUS" "404"

section "9. Not found and method errors"
req GET /books/999999;               check "unknown book 404" "$STATUS" "404"
req GET /authors/999999;             check "unknown author 404" "$STATUS" "404"
req GET /no-such-endpoint "" "$TOKEN"; check "unknown path 404" "$STATUS" "404"
req DELETE /categories "" "$TOKEN";  check "wrong method 405" "$STATUS" "405"
req POST /cart/items '{"bookId":999999}' "$TOKEN"
check "add unknown book 404" "$STATUS" "404"

section "10. Stock rules"
req GET "/books?q=Monsoon%20Hearts"; SCARCE=$(jqv '.content[0].id')
req POST /cart/items "{\"bookId\":$SCARCE,\"quantity\":13}" "$TOKEN"
check "more than the stock (12) is 409" "$STATUS" "409"

printf '\n----------------------------------------\n'
printf 'Passed: %d   Failed: %d\n' "$PASS" "$FAIL"
[[ "$FAIL" -eq 0 ]]
