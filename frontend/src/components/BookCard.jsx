import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, Heart, Check } from 'lucide-react';
import { BookCover } from './BookCover';
import { useCart } from '../context/CartContext';
import { useWishlist } from '../context/WishlistContext';

export function BookCard({ book, layout = 'horizontal' }) {
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const { isWishlisted, toggleWishlist } = useWishlist();

  if (!book) return null;

  const wishlisted = isWishlisted(book.id);
  const authorName = book.author?.name || (typeof book.author === 'string' ? book.author : 'Unknown Author');
  const authorId = book.author?.id;

  // Format delivery date
  const formatDeliveryDate = (dateStr) => {
    if (!dateStr) return 'Delivery in 3-5 days';
    const date = new Date(dateStr);
    return `Delivery by ${date.toLocaleDateString('en-GB', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
    })}`;
  };

  const handleCardClick = () => {
    navigate(`/books/${book.id}`);
  };

  const handleAddToCart = (e) => {
    e.stopPropagation();
    addToCart(book.id, 1, book.title);
  };

  const handleWishlist = (e) => {
    e.stopPropagation();
    toggleWishlist(book);
  };

  const handleAuthorClick = (e) => {
    e.stopPropagation();
    if (authorId) {
      navigate(`/writers?authorId=${authorId}`);
    }
  };

  // Horizontal Card layout matching Slide 5/6 wireframes perfectly
  return (
    <div
      onClick={handleCardClick}
      className="group relative flex flex-row gap-3.5 p-3 rounded-xl bg-[#171a21]/90 hover:bg-[#1e222b] border border-[#262c3b]/70 hover:border-[#3b82f6]/40 transition-all duration-200 cursor-pointer shadow-md hover:shadow-xl overflow-hidden"
    >
      {/* Book Cover */}
      <div className="shrink-0 transition-transform duration-200 group-hover:scale-[1.02]">
        <BookCover book={book} className="w-24 h-36" />
      </div>

      {/* Book Details */}
      <div className="flex flex-col justify-between flex-1 min-w-0 pr-1">
        <div>
          {/* Title */}
          <h3 className="text-sm font-semibold text-white tracking-tight leading-snug group-hover:text-sky-300 transition-colors line-clamp-1">
            {book.title}
          </h3>

          {/* Author */}
          <p className="text-xs text-sky-400 hover:text-sky-300 transition-colors mt-0.5 line-clamp-1 font-medium">
            <span className="text-slate-400 font-normal">by </span>
            <span onClick={handleAuthorClick} className="hover:underline">
              {authorName}
            </span>
          </p>

          {/* Description snippet */}
          <p className="text-[11px] text-slate-400 line-clamp-2 mt-1 leading-normal font-light">
            {book.description || 'A compelling read for all book enthusiasts.'}
          </p>

          {/* Format & Categories */}
          <div className="flex flex-wrap items-center gap-1.5 mt-2">
            {book.format && (
              <span className="text-[10px] text-slate-300 bg-[#252b38] px-1.5 py-0.5 rounded font-medium">
                {book.format.charAt(0).toUpperCase() + book.format.slice(1).toLowerCase()}
              </span>
            )}
            {book.categories && book.categories.length > 0 && (
              <span className="text-[10px] text-sky-300/90 font-medium">
                {book.categories.map((c) => c.name).slice(0, 2).join(', ')}
              </span>
            )}
          </div>
        </div>

        {/* Bottom Price, Delivery & Actions */}
        <div className="mt-2 pt-2 border-t border-[#262c3b]/50 flex items-center justify-between">
          <div>
            <div className="text-sm font-bold text-white tracking-tight">
              ₹{Number(book.price).toFixed(0)}
            </div>
            <div className="text-[10px] text-slate-400 font-normal">
              {formatDeliveryDate(book.estimatedDeliveryDate)}
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={handleWishlist}
              title={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
              className={`p-1.5 rounded-lg border transition-all ${
                wishlisted
                  ? 'bg-rose-500/20 border-rose-500/50 text-rose-400'
                  : 'bg-[#222836] border-[#313a4d] text-slate-400 hover:text-rose-400 hover:border-rose-400/50'
              }`}
            >
              <Heart className={`w-3.5 h-3.5 ${wishlisted ? 'fill-current' : ''}`} />
            </button>
            <button
              onClick={handleAddToCart}
              title="Add to cart"
              className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white text-xs font-medium transition-colors shadow-sm"
            >
              <ShoppingCart className="w-3.5 h-3.5" />
              <span className="hidden sm:inline text-[11px]">Add</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
