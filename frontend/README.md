# Book Worm — Frontend Application

A responsive, modern e-bookstore frontend built with React, Vite, and Tailwind CSS. The interface is styled to match the dark theme wireframe mockups from Slides 5–10 of the Capstone specifications.

---

## 🎨 Screen & Wireframe Mapping

| Slide # | Wireframe Screen | Component / Route | Key Features |
|---|---|---|---|
| **Slide 5 & 6** | **Landing & Catalogue Page** | [`/` (`HomePage.jsx`)](src/pages/HomePage.jsx:1) | • Category sidebar navigation ("All", "Romance", "Mystery", etc.)<br>• Search & filter bar (Language, Format, Price, Sort)<br>• Curated sections: *Recommended for You*, *Bestsellers this Month*, *New Launches*<br>• Real-time search & filtered results grid |
| **Slide 7** | **Product Detail Page** | [`/books/:id` (`BookDetailPage.jsx`)](src/pages/BookDetailPage.jsx:1) | • Breadcrumb navigation (`Home / Category / Subcategory / Title`)<br>• Front cover & back cover flip preview with barcode<br>• Format badges, tags, pricing, delivery dates<br>• Quick actions: *Add to Cart*, *Add to Wishlist*<br>• *About the writer* with bio and Follow/Unfollow action<br>• *Reviews* section with interactive 5-star rating submission<br>• *Related Reads* sidebar recommendations |
| **Slide 8** | **Shopping Cart & Checkout** | [`/checkout` (`CheckoutPage.jsx`)](src/pages/CheckoutPage.jsx:1) | • Shopping cart item list with quantity selector (`- 1 +`) & remove<br>• Delivery address form with *Use Saved Address* option<br>• Grand Total card with coupon discounts (`FLAT100`, `WELCOME10`)<br>• Gift point redemption calculator<br>• *Pay Now* button triggering payment modal |
| **Slide 9** | **Payment Screen** | [`PaymentModal.jsx`](src/components/PaymentModal.jsx:1) | • Payment method tabs: *Credit Card*, *Debit card*, *UPI*, *Wallet*<br>• Form validation for card numbers, CVV, expiry date, UPI VPA<br>• Secure simulated payment submission calling `POST /orders/{id}/payment` |
| **Slide 10** | **Purchase Confirmation** | [`/confirmation` (`ConfirmationPage.jsx`)](src/pages/ConfirmationPage.jsx:1) | • Success banner: *"Your purchase of the following reads is successful"*<br>• Purchased item summaries with delivery estimates<br>• *Continue your Shopping* button |
| **Customer Journey** | **Order History & Reorder** | [`/orders` (`OrdersPage.jsx`)](src/pages/OrdersPage.jsx:1) | • **48-Hour Cancel Rule**: Cancel order button with instant refund<br>• **Buy It Again**: 1-click reorder into cart |
| **Engagement** | **Wishlist & My Writers** | [`/wishlist`](src/pages/WishlistPage.jsx:1), [`/writers`](src/pages/WritersPage.jsx:1) | • Saved book collection<br>• Followed author profiles and author bibliography |

---

## 🛠️ Tech Stack & Reusable Components

- **Framework**: React 18 + Vite
- **Styling**: Tailwind CSS (dark mode palette matching wireframes)
- **Icons**: Lucide React
- **Routing**: React Router v6
- **State & Context**:
  - `AuthContext`: Session management, JWT token persistence, modal trigger
  - `CartContext`: Cart count badge, live sync with Spring Boot backend
  - `WishlistContext`: Wishlisted books and followed authors
  - `ToastContext`: Toast notification alerts
- **Reusable Components**:
  - [`Navbar.jsx`](src/components/Navbar.jsx:1): Header with brand logo, nav links, live cart counter, points indicator, and user profile dropdown
  - [`BookCard.jsx`](src/components/BookCard.jsx:1): Wireframe-faithful book card with cover, metadata, delivery date, price, wishlist toggle, and cart button
  - [`BookCover.jsx`](src/components/BookCover.jsx:1): Cover art rendering engine with custom illustrations for every genre and book
  - [`CategorySidebar.jsx`](src/components/CategorySidebar.jsx:1): Responsive vertical category sidebar
  - [`SearchFilterBar.jsx`](src/components/SearchFilterBar.jsx:1): Responsive search input and filter dropdowns
  - [`AuthModal.jsx`](src/components/AuthModal.jsx:1): Sign in & Registration modal with 1-click demo credential filler

---

## 🚀 Running the Frontend

### 1. Local Development
```bash
cd frontend
npm install
npm run dev
```
The application will be accessible at `http://localhost:5173`. Requests to `/api/v1` are automatically proxied to `http://localhost:8080`.

### 2. Production Build
```bash
npm run build
```

### 3. Docker Run (Full Stack)
```bash
docker compose up --build
```
- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080/api/v1`
