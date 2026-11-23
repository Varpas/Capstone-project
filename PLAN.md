# Capstone Plan: E-Bookstore Platform (Spring Boot + PostgreSQL, built with IBM Bob)

## 1. Requirements Summary

**Objective:** Use an agentic IDE (IBM Bob) to build the backend of an e-commerce bookstore, and show how AI was used at each step.

**Deliverables**
1. Working Spring Boot backend + PostgreSQL, run locally. Cloud deployment (ROSA/ROKS) is optional.
2. OpenAPI specification.
3. AI-assisted UI (vibe coding) for the screens in the wireframes.
4. Test cases from an ICA agent (e.g. API Testcase Generator).
5. PR on a personal GitHub repo, with the Manager given access.
6. Recorded video walkthrough for Manager review.

**Customer journey (12 steps, from the slides)**
Login/auth → catalogue (category → brands → products with tentative delivery date) → related products → add to basket → recommendations from order history → select delivery address → redeem gift points → choose payment option → pay → confirmation → cancel order within 48 hrs. Order history has a "Buy it again" feature.

**Screens:** Login, Home/Landing, Catalogue, Product detail, Cart, Address and Payment, Payment screen (credit/debit card), Confirmation, Order history.

**Stack:** Java + Spring Boot, Maven, PostgreSQL, OpenAPI, React UI (the slides mention "test case generator for React"), Insomnia/Postman, Git/GitHub.

## 2. Open Questions / Assumptions
- The slides say "database type (MySQL/H2)" in step 4 but mandate PostgreSQL elsewhere. **Assumption: PostgreSQL**, plus H2 for unit tests only.
- The slides don't specify the Java version. **Assumption: Java 17+, Spring Boot 3.x.**
- The wireframes are images and didn't extract from the PDF. Open the PDF visually (slides 5–10) to confirm fields before modelling.
- Payment is **simulated**. No real gateway.
- The slides don't specify the auth mechanism. **Assumption: JWT with Spring Security.**
- Frontend scope is "AI augmentation on UI". **Assumption: a lightweight React UI** that consumes the API.

## 3. Data Model (draft)

> Superseded by [`docs/data-model.md`](docs/data-model.md), which was refined from the wireframes in phase 1. The table below is the initial draft.

| Entity | Key fields |
|---|---|
| User | id, name, email, password_hash, gift_points |
| Address | id, user_id, line1, city, postal_code, country, is_default |
| Category | id, name |
| Brand (publisher/author) | id, name |
| Product (Book) | id, title, author, isbn, price, stock, category_id, brand_id, image_url, delivery_days |
| RelatedProduct | product_id, related_product_id |
| Cart / CartItem | id, user_id; cart_id, product_id, quantity |
| Order | id, user_id, address_id, status, subtotal, points_redeemed, total, created_at |
| OrderItem | id, order_id, product_id, qty, unit_price |
| Payment | id, order_id, method (CREDIT/DEBIT), status, amount, created_at |

Order status: `PLACED → PAID → CANCELLED` (cancel allowed only within 48 hrs of `created_at`).

## 4. API Surface (draft)

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login` |
| Catalogue | `GET /categories`, `GET /categories/{id}/brands`, `GET /products?category=&brand=&q=`, `GET /products/{id}` (includes estimated delivery date), `GET /products/{id}/related` |
| Cart | `GET /cart`, `POST /cart/items`, `PUT /cart/items/{id}`, `DELETE /cart/items/{id}` |
| Recommendations | `GET /recommendations` (based on order history) |
| Addresses | `GET/POST/PUT/DELETE /addresses` |
| Checkout | `POST /orders` (address + redeem points), `POST /orders/{id}/payment` |
| Orders | `GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel` (48 hrs rule), `POST /orders/{id}/reorder` ("Buy it again") |
| Gift points | `GET /users/me/points` |

## 5. Execution Plan (maps to slide steps 1–9)

| # | Phase | Tasks | Output |
|---|---|---|---|
| 0 | Setup | Get IBM Bob access, install it, install JDK, Maven and PostgreSQL, create a GitHub repo, join the Slack channel | Working environment |
| 1 | Analyze wireframes | Review slides 5–10, list entities and features, finalize the data model (manual) | `docs/data-model.md` |
| 2 | OpenAPI spec | Prompt Bob with wireframes and entities, review and edit the spec | `openapi.yaml` |
| 3 | Generate Spring Boot code | Prompt Bob with the spec (Java version, PostgreSQL, Spring Web/Data JPA/Security/Validation, Flyway) | `backend/` skeleton: controllers, services, repositories, entities |
| 4 | Database | Create the DB, set `application.properties`, add Flyway migrations and seed data (categories, brands, ~30 books) | Schema and seed data |
| 5 | Business logic | Implement auth, catalogue and related products, cart, recommendations, checkout with gift points, simulated payment, 48-hr cancel, reorder | Complete APIs |
| 6 | Build and run | `mvn clean install`, `mvn spring-boot:run`, fix errors | App runs |
| 7 | API testing | Test all endpoints in Insomnia/Postman against the spec, verify rows in the DB, export the collection | `docs/insomnia-collection.json` |
| 8 | Automated tests | Use the ICA agent for API test cases and add JUnit/MockMvc tests. Cover the 48-hr cancel, points redemption and stock checks | `src/test/...` and the generated test artefacts |
| 9 | UI (AI-augmented) | Vibe-code the React screens with Bob (login, catalogue, cart, checkout, payment, confirmation, order history) | `frontend/` |
| 10 | Deployment | Local run via docker-compose (app + Postgres). Optional: ROSA/ROKS | `Dockerfile`, `docker-compose.yml` |
| 11 | Git and PR | `git init`, branch `feature/api-implementation`, commit, push, open a PR with API docs, add the Manager as collaborator | PR link |
| 12 | Video | Record a walkthrough covering the prompts used, the generated output, the manual edits, a demo of the journey and the PR | Video file |

## 6. Repository Layout (proposed)

```
Capstone-project/
├── backend/          # Spring Boot (Maven)
├── frontend/         # React UI
├── docs/             # data model, openapi.yaml, test collection, prompt log
├── docker-compose.yml
├── PLAN.md
└── README.md
```

## 7. How AI Usage Is Shown
- Keep a `docs/prompt-log.md` with the key Bob prompts, outputs, and what was changed by hand.
- Keep the spec-first workflow: spec → code → tests.
- Review all generated code, and note what Bob got wrong and how it was fixed.

## 8. Risks
| Risk | Mitigation |
|---|---|
| Wireframes ambiguous | Confirm fields visually and keep assumptions in the README |
| Generated code doesn't compile or drifts from the spec | Build after each generation and compare to `openapi.yaml` |
| Inconsistent DB choice | PostgreSQL is fixed; H2 only for tests |
| Cloud access unavailable | The local deployment satisfies the requirement |
| Time | Do the must-haves first (spec, backend, tests, PR, video). The UI is next, then the cloud deploy |

## 9. Definition of Done
- [ ] `openapi.yaml` is reviewed and matches the implementation
- [ ] All 12 journey steps are supported via API
- [ ] App builds and runs against PostgreSQL
- [ ] All endpoints verified manually and by automated tests
- [ ] ICA-generated tests are included
- [ ] UI covers the wireframe screens
- [ ] PR is open and the Manager has access
- [ ] Video is recorded and submitted
