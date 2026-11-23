const API_BASE = '/api/v1';

export class ApiError extends Error {
  constructor(message, status, details = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

export function getToken() {
  return localStorage.getItem('bookworm_token');
}

export function setToken(token) {
  if (token) {
    localStorage.setItem('bookworm_token', token);
  } else {
    localStorage.removeItem('bookworm_token');
  }
}

export async function apiRequest(endpoint, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const errorMsg = (isJson && (data.message || data.error)) || response.statusText || 'Request failed';
    throw new ApiError(errorMsg, response.status, data);
  }

  return data;
}

// API methods
export const api = {
  // Auth
  auth: {
    login: (credentials) => apiRequest('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
    register: (userData) => apiRequest('/auth/register', { method: 'POST', body: JSON.stringify(userData) }),
    me: () => apiRequest('/users/me'),
  },

  // Catalogue
  catalogue: {
    getHome: (limit = 6) => apiRequest(`/home?limit=${limit}`),
    getCategories: () => apiRequest('/categories'),
    getPublishers: () => apiRequest('/publishers'),
    getBooks: (params = {}) => {
      const query = new URLSearchParams();
      Object.entries(params).forEach(([key, val]) => {
        if (val !== undefined && val !== null && val !== '') {
          query.append(key, val);
        }
      });
      const qStr = query.toString();
      return apiRequest(`/books${qStr ? `?${qStr}` : ''}`);
    },
    getBook: (bookId) => apiRequest(`/books/${bookId}`),
    getRelatedBooks: (bookId, limit = 3) => apiRequest(`/books/${bookId}/related?limit=${limit}`),
    getAuthor: (authorId) => apiRequest(`/authors/${authorId}`),
  },

  // Cart
  cart: {
    getCart: () => apiRequest('/cart'),
    addItem: (bookId, quantity = 1) => apiRequest('/cart/items', {
      method: 'POST',
      body: JSON.stringify({ bookId, quantity }),
    }),
    setQuantity: (bookId, quantity) => apiRequest(`/cart/items/${bookId}`, {
      method: 'PUT',
      body: JSON.stringify({ quantity }),
    }),
    removeItem: (bookId) => apiRequest(`/cart/items/${bookId}`, { method: 'DELETE' }),
    clearCart: () => apiRequest('/cart', { method: 'DELETE' }),
  },

  // Checkout & Orders
  checkout: {
    quote: (pricingReq) => apiRequest('/checkout/quote', {
      method: 'POST',
      body: JSON.stringify(pricingReq),
    }),
    placeOrder: (orderReq) => apiRequest('/orders', {
      method: 'POST',
      body: JSON.stringify(orderReq),
    }),
    pay: (orderId, paymentReq) => apiRequest(`/orders/${orderId}/payment`, {
      method: 'POST',
      body: JSON.stringify(paymentReq),
    }),
  },

  orders: {
    list: (params = {}) => {
      const query = new URLSearchParams();
      if (params.status) query.append('status', params.status);
      if (params.page !== undefined) query.append('page', params.page);
      if (params.size !== undefined) query.append('size', params.size);
      const qStr = query.toString();
      return apiRequest(`/orders${qStr ? `?${qStr}` : ''}`);
    },
    get: (orderId) => apiRequest(`/orders/${orderId}`),
    cancel: (orderId) => apiRequest(`/orders/${orderId}/cancel`, { method: 'POST' }),
    reorder: (orderId) => apiRequest(`/orders/${orderId}/reorder`, { method: 'POST' }),
  },

  // Addresses
  addresses: {
    list: () => apiRequest('/addresses'),
    create: (address) => apiRequest('/addresses', {
      method: 'POST',
      body: JSON.stringify(address),
    }),
    update: (id, address) => apiRequest(`/addresses/${id}`, {
      method: 'PUT',
      body: JSON.stringify(address),
    }),
    delete: (id) => apiRequest(`/addresses/${id}`, { method: 'DELETE' }),
  },

  // Wishlist & Writers
  engagement: {
    getWishlist: () => apiRequest('/wishlist'),
    addToWishlist: (bookId) => apiRequest(`/wishlist/${bookId}`, { method: 'PUT' }),
    removeFromWishlist: (bookId) => apiRequest(`/wishlist/${bookId}`, { method: 'DELETE' }),
    getWriters: () => apiRequest('/me/writers'),
    followAuthor: (authorId) => apiRequest(`/me/writers/${authorId}`, { method: 'PUT' }),
    unfollowAuthor: (authorId) => apiRequest(`/me/writers/${authorId}`, { method: 'DELETE' }),
  },

  // Reviews
  reviews: {
    list: (bookId, page = 0, size = 20) => apiRequest(`/books/${bookId}/reviews?page=${page}&size=${size}`),
    create: (bookId, review) => apiRequest(`/books/${bookId}/reviews`, {
      method: 'POST',
      body: JSON.stringify(review),
    }),
  },

  // Gift Points
  giftPoints: {
    get: (page = 0, size = 20) => apiRequest(`/me/gift-points?page=${page}&size=${size}`),
  },

  // Recommendations
  recommendations: {
    list: (limit = 6) => apiRequest(`/recommendations?limit=${limit}`),
  },
};
