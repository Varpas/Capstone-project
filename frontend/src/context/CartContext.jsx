import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import { useAuth } from './AuthContext';
import { useToast } from './ToastContext';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { isAuthenticated, openAuthModal } = useAuth();
  const toast = useToast();
  const [cart, setCart] = useState({ items: [], itemCount: 0, subtotal: 0 });
  const [loading, setLoading] = useState(false);

  const fetchCart = useCallback(async () => {
    if (!isAuthenticated) {
      setCart({ items: [], itemCount: 0, subtotal: 0 });
      return;
    }
    try {
      setLoading(true);
      const data = await api.cart.getCart();
      setCart(data || { items: [], itemCount: 0, subtotal: 0 });
    } catch (err) {
      console.warn('Could not load cart:', err);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchCart();
  }, [fetchCart]);

  const addToCart = async (bookId, quantity = 1, bookTitle = 'Book') => {
    if (!isAuthenticated) {
      toast.info('Please sign in to add items to your cart');
      openAuthModal('login');
      return false;
    }
    try {
      const updated = await api.cart.addItem(bookId, quantity);
      setCart(updated);
      toast.success(`"${bookTitle}" added to cart`);
      return true;
    } catch (err) {
      toast.error(err.message || 'Could not add to cart');
      return false;
    }
  };

  const updateQuantity = async (bookId, quantity) => {
    if (!isAuthenticated) return;
    try {
      const updated = await api.cart.setQuantity(bookId, quantity);
      setCart(updated);
    } catch (err) {
      toast.error(err.message || 'Could not update quantity');
    }
  };

  const removeFromCart = async (bookId, bookTitle = 'Item') => {
    if (!isAuthenticated) return;
    try {
      const updated = await api.cart.removeItem(bookId);
      setCart(updated || { items: [], itemCount: 0, subtotal: 0 });
      toast.info(`Removed ${bookTitle} from cart`);
    } catch (err) {
      toast.error(err.message || 'Could not remove item');
    }
  };

  const clearCart = async () => {
    if (!isAuthenticated) return;
    try {
      await api.cart.clearCart();
      setCart({ items: [], itemCount: 0, subtotal: 0 });
    } catch (err) {
      console.error('Failed to clear cart', err);
    }
  };

  return (
    <CartContext.Provider
      value={{
        cart,
        itemCount: cart?.itemCount || 0,
        subtotal: cart?.subtotal || 0,
        loading,
        addToCart,
        updateQuantity,
        removeFromCart,
        clearCart,
        refreshCart: fetchCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}
