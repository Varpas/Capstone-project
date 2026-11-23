import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import { useAuth } from './AuthContext';
import { useToast } from './ToastContext';

const WishlistContext = createContext(null);

export function WishlistProvider({ children }) {
  const { isAuthenticated, openAuthModal } = useAuth();
  const toast = useToast();
  const [wishlist, setWishlist] = useState([]);
  const [wishlistIds, setWishlistIds] = useState(new Set());
  const [followedAuthorIds, setFollowedAuthorIds] = useState(new Set());
  const [loading, setLoading] = useState(false);

  const fetchWishlistAndWriters = useCallback(async () => {
    if (!isAuthenticated) {
      setWishlist([]);
      setWishlistIds(new Set());
      setFollowedAuthorIds(new Set());
      return;
    }
    try {
      setLoading(true);
      const [wishlistData, writersData] = await Promise.all([
        api.engagement.getWishlist().catch(() => []),
        api.engagement.getWriters().catch(() => []),
      ]);
      setWishlist(wishlistData || []);
      setWishlistIds(new Set((wishlistData || []).map((b) => b.id)));
      setFollowedAuthorIds(new Set((writersData || []).map((w) => w.id)));
    } catch (err) {
      console.warn('Could not load wishlist/writers:', err);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchWishlistAndWriters();
  }, [fetchWishlistAndWriters]);

  const toggleWishlist = async (book) => {
    if (!isAuthenticated) {
      toast.info('Please sign in to save books to your wishlist');
      openAuthModal('login');
      return;
    }
    const bookId = book.id;
    const isCurrentlyWishlisted = wishlistIds.has(bookId);
    try {
      if (isCurrentlyWishlisted) {
        await api.engagement.removeFromWishlist(bookId);
        setWishlistIds((prev) => {
          const next = new Set(prev);
          next.delete(bookId);
          return next;
        });
        setWishlist((prev) => prev.filter((b) => b.id !== bookId));
        toast.info(`Removed "${book.title}" from Wishlist`);
      } else {
        await api.engagement.addToWishlist(bookId);
        setWishlistIds((prev) => new Set(prev).add(bookId));
        setWishlist((prev) => [...prev, book]);
        toast.success(`Saved "${book.title}" to Wishlist`);
      }
    } catch (err) {
      toast.error(err.message || 'Failed to update wishlist');
    }
  };

  const toggleFollowAuthor = async (authorId, authorName = 'Author') => {
    if (!isAuthenticated) {
      toast.info('Please sign in to follow writers');
      openAuthModal('login');
      return;
    }
    const isFollowing = followedAuthorIds.has(authorId);
    try {
      if (isFollowing) {
        await api.engagement.unfollowAuthor(authorId);
        setFollowedAuthorIds((prev) => {
          const next = new Set(prev);
          next.delete(authorId);
          return next;
        });
        toast.info(`Unfollowed ${authorName}`);
      } else {
        await api.engagement.followAuthor(authorId);
        setFollowedAuthorIds((prev) => new Set(prev).add(authorId));
        toast.success(`Following ${authorName}`);
      }
    } catch (err) {
      toast.error(err.message || 'Failed to follow/unfollow author');
    }
  };

  const isWishlisted = (bookId) => wishlistIds.has(bookId);
  const isFollowing = (authorId) => followedAuthorIds.has(authorId);

  return (
    <WishlistContext.Provider
      value={{
        wishlist,
        wishlistIds,
        followedAuthorIds,
        loading,
        toggleWishlist,
        toggleFollowAuthor,
        isWishlisted,
        isFollowing,
        refreshWishlist: fetchWishlistAndWriters,
      }}
    >
      {children}
    </WishlistContext.Provider>
  );
}

export function useWishlist() {
  const context = useContext(WishlistContext);
  if (!context) {
    throw new Error('useWishlist must be used within a WishlistProvider');
  }
  return context;
}
