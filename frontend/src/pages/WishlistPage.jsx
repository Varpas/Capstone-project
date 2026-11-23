import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useWishlist } from '../context/WishlistContext';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { BookCard } from '../components/BookCard';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EmptyState } from '../components/EmptyState';
import { Heart, BookMarked, ShoppingCart } from 'lucide-react';

export function WishlistPage() {
  const { isAuthenticated, openAuthModal } = useAuth();
  const { wishlist, loading, refreshWishlist } = useWishlist();

  useEffect(() => {
    refreshWishlist();
  }, [refreshWishlist]);

  if (!isAuthenticated) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <EmptyState
          icon={BookMarked}
          title="Sign in to view your Wishlist"
          description="Save books you love and revisit them anytime."
          actionText="Sign In / Register"
          onAction={() => openAuthModal('login')}
        />
      </div>
    );
  }

  if (loading) {
    return <LoadingSpinner text="Loading your wishlist..." />;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      <div className="flex items-center justify-between border-b border-[#262c3b] pb-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Heart className="w-5 h-5 text-rose-500 fill-rose-500" />
            <span>My Wishlist</span>
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Your saved titles, available for quick purchase whenever you're ready.
          </p>
        </div>
        <span className="text-xs text-slate-400 bg-[#171a21] px-3 py-1 rounded-lg border border-[#262c3b]">
          {wishlist.length} saved {wishlist.length === 1 ? 'book' : 'books'}
        </span>
      </div>

      {wishlist.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {wishlist.map((book) => (
            <BookCard key={book.id} book={book} />
          ))}
        </div>
      ) : (
        <EmptyState
          icon={BookMarked}
          title="Your Wishlist is empty"
          description="Click the heart icon on any book to add it to your wishlist."
          actionText="Explore Catalogue"
          actionTo="/"
        />
      )}

    </div>
  );
}
