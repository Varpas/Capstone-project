import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { BookCover } from '../components/BookCover';
import { BookCard } from '../components/BookCard';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { useCart } from '../context/CartContext';
import { useWishlist } from '../context/WishlistContext';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { 
  ShoppingCart, 
  Heart, 
  Star, 
  Globe, 
  Award, 
  ChevronRight, 
  UserCheck, 
  UserPlus, 
  Send,
  Building2,
  Calendar,
  Check
} from 'lucide-react';

export function BookDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const { isWishlisted, toggleWishlist, isFollowing, toggleFollowAuthor } = useWishlist();
  const { isAuthenticated, openAuthModal, user } = useAuth();
  const toast = useToast();

  const [book, setBook] = useState(null);
  const [author, setAuthor] = useState(null);
  const [relatedBooks, setRelatedBooks] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);

  // Review Form state
  const [rating, setRating] = useState(5);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  // Cover flip state (front / back)
  const [showBackCover, setShowBackCover] = useState(false);

  useEffect(() => {
    async function loadBookData() {
      setLoading(true);
      try {
        const bookData = await api.catalogue.getBook(id);
        setBook(bookData);

        // Fetch author, related books and reviews in parallel
        const [relatedData, reviewsData, authorData] = await Promise.all([
          api.catalogue.getRelatedBooks(id, 3).catch(() => []),
          api.reviews.list(id).catch(() => ({ content: [] })),
          bookData?.author?.id ? api.catalogue.getAuthor(bookData.author.id).catch(() => null) : null,
        ]);

        setRelatedBooks(relatedData || []);
        setReviews(reviewsData?.content || []);
        setAuthor(authorData);
      } catch (err) {
        console.error('Failed to load book detail', err);
        toast.error('Book not found or failed to load');
      } finally {
        setLoading(false);
      }
    }
    loadBookData();
    window.scrollTo(0, 0);
  }, [id]);

  if (loading) {
    return <LoadingSpinner text="Loading book details..." />;
  }

  if (!book) {
    return (
      <div className="max-w-4xl mx-auto p-12 text-center">
        <p className="text-slate-400">Book not found.</p>
        <Link to="/" className="text-sky-400 hover:underline mt-2 inline-block text-sm">
          Return to home
        </Link>
      </div>
    );
  }

  const wishlisted = isWishlisted(book.id);
  const following = author?.id ? isFollowing(author.id) : false;

  const handleReviewSubmit = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      toast.info('Please sign in to leave a review');
      openAuthModal('login');
      return;
    }
    if (!comment.trim()) {
      toast.error('Please write a short comment');
      return;
    }

    setSubmittingReview(true);
    try {
      const newReview = await api.reviews.create(book.id, {
        rating,
        comment: comment.trim(),
      });
      setReviews((prev) => [newReview, ...prev]);
      setComment('');
      setRating(5);
      toast.success('Thank you! Your review has been posted.');
    } catch (err) {
      toast.error(err.message || 'Failed to submit review');
    } finally {
      setSubmittingReview(false);
    }
  };

  const formatDeliveryDate = (dateStr) => {
    if (!dateStr) return 'Delivery in 3-5 days';
    const date = new Date(dateStr);
    return `Delivery by ${date.toLocaleDateString('en-GB', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
    })}`;
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      
      {/* Breadcrumb matching Slide 7 */}
      <nav className="flex items-center gap-1.5 text-xs text-slate-400 mb-6 flex-wrap">
        <Link to="/" className="hover:text-slate-200 transition-colors">Home</Link>
        <ChevronRight className="w-3.5 h-3.5" />
        {book.categories && book.categories[0] && (
          <>
            <Link
              to={`/?categoryId=${book.categories[0].id}`}
              className="hover:text-slate-200 transition-colors"
            >
              {book.categories[0].name}
            </Link>
            <ChevronRight className="w-3.5 h-3.5" />
          </>
        )}
        {book.categories && book.categories[1] && (
          <>
            <span className="text-slate-300">{book.categories[1].name}</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </>
        )}
        <span className="text-sky-400 font-medium truncate max-w-xs">{book.title}</span>
      </nav>

      {/* Main Grid: Left/Center Showcase + Right Sidebar */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left & Center Main Content (col-span-8 or 9) */}
        <div className="lg:col-span-8 space-y-8">
          
          {/* Top Showcase: Book Cover & Main Info Card */}
          <div className="bg-[#171a21] border border-[#262c3b] rounded-2xl p-5 sm:p-7 shadow-xl">
            <div className="flex flex-col sm:flex-row gap-6">
              
              {/* Cover Column with Front/Back Preview */}
              <div className="flex flex-col items-center shrink-0">
                <div className="relative group cursor-pointer" onClick={() => setShowBackCover(!showBackCover)}>
                  <BookCover
                    book={book}
                    className="w-44 h-64 sm:w-48 sm:h-72"
                    size="large"
                  />
                  {showBackCover && (
                    <div className="absolute inset-0 bg-[#1e222b] rounded-md p-4 flex flex-col justify-between text-xs text-slate-300 border border-slate-700 book-shadow">
                      <div>
                        <p className="text-[11px] font-semibold text-amber-300 uppercase tracking-wider mb-2">Back Cover Summary</p>
                        <p className="text-[11px] leading-relaxed text-slate-300 italic">
                          "{book.description}"
                        </p>
                      </div>
                      <div className="pt-2 border-t border-slate-700/60 flex items-center justify-between text-[10px] text-slate-400">
                        <span>ISBN {book.isbn}</span>
                        <span className="font-mono tracking-widest">||||| ||| ||</span>
                      </div>
                    </div>
                  )}
                </div>

                <button
                  onClick={() => setShowBackCover(!showBackCover)}
                  className="mt-2 text-[11px] text-sky-400 hover:text-sky-300 font-medium underline"
                >
                  {showBackCover ? 'Show Front Cover' : 'Flip to Back Cover'}
                </button>
              </div>

              {/* Info Column */}
              <div className="flex-1 flex flex-col justify-between space-y-4">
                <div>
                  <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
                    {book.title}
                  </h1>

                  <div className="mt-1 flex items-center gap-2">
                    <span className="text-xs text-slate-400">by</span>
                    <button
                      onClick={() => author?.id && navigate(`/writers?authorId=${author.id}`)}
                      className="text-xs text-sky-400 hover:text-sky-300 font-semibold hover:underline"
                    >
                      {book.author?.name || 'Author'}
                    </button>
                  </div>

                  <p className="text-xs text-slate-300 mt-3 leading-relaxed">
                    {book.description}
                  </p>

                  {book.publisher && (
                    <div className="flex items-center gap-1.5 mt-2.5 text-xs text-slate-400">
                      <Building2 className="w-3.5 h-3.5 text-slate-500" />
                      <span>Published by <strong className="text-slate-300 font-medium">{book.publisher.name}</strong></span>
                    </div>
                  )}

                  {/* Format & Category Tags */}
                  <div className="flex flex-wrap items-center gap-2 mt-3.5">
                    {book.format && (
                      <span className="text-xs text-slate-200 bg-[#252b38] px-2.5 py-1 rounded-lg font-medium border border-[#363f52]">
                        {book.format.charAt(0).toUpperCase() + book.format.slice(1).toLowerCase()}
                      </span>
                    )}
                    {book.categories?.map((cat) => (
                      <span
                        key={cat.id}
                        className="text-xs text-sky-300 bg-sky-950/40 border border-sky-800/40 px-2.5 py-1 rounded-lg font-medium"
                      >
                        {cat.name}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Price, Delivery & Actions */}
                <div className="pt-4 border-t border-[#262c3b] space-y-3">
                  <div className="flex items-baseline gap-3">
                    <span className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
                      ₹{Number(book.price).toFixed(0)}
                    </span>
                    <span className="text-xs text-slate-400 font-medium">
                      {formatDeliveryDate(book.estimatedDeliveryDate)}
                    </span>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex flex-wrap items-center gap-3">
                    <button
                      onClick={() => addToCart(book.id, 1, book.title)}
                      className="flex-1 flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white font-semibold text-sm transition-all shadow-lg shadow-blue-600/30"
                    >
                      <ShoppingCart className="w-4 h-4" />
                      <span>Add to Cart</span>
                    </button>

                    <button
                      onClick={() => toggleWishlist(book)}
                      className={`flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border text-sm font-medium transition-colors ${
                        wishlisted
                          ? 'bg-rose-500/20 border-rose-500/50 text-rose-300 hover:bg-rose-500/30'
                          : 'bg-[#222836] border-[#313a4d] text-slate-300 hover:text-white hover:border-slate-500'
                      }`}
                    >
                      <Heart className={`w-4 h-4 ${wishlisted ? 'fill-rose-500 text-rose-500' : ''}`} />
                      <span>{wishlisted ? 'In Wishlist' : 'Add to Wishlist'}</span>
                    </button>
                  </div>

                  {/* Metrics Row: Language, Rating, Sells */}
                  <div className="grid grid-cols-3 gap-2 pt-3 text-center border-t border-[#262c3b]/60">
                    <div className="p-2 rounded-lg bg-[#14161c]">
                      <div className="flex items-center justify-center gap-1 text-[11px] text-slate-400">
                        <Globe className="w-3.5 h-3.5 text-sky-400" /> Language
                      </div>
                      <div className="text-xs font-semibold text-white mt-0.5">{book.language || 'English'}</div>
                    </div>

                    <div className="p-2 rounded-lg bg-[#14161c]">
                      <div className="flex items-center justify-center gap-1 text-[11px] text-slate-400">
                        <Star className="w-3.5 h-3.5 text-amber-400 fill-amber-400" /> Rating
                      </div>
                      <div className="text-xs font-semibold text-white mt-0.5">
                        {book.averageRating ? `${Number(book.averageRating).toFixed(1)} / 5.0` : '5.0 ★'}
                      </div>
                    </div>

                    <div className="p-2 rounded-lg bg-[#14161c]">
                      <div className="flex items-center justify-center gap-1 text-[11px] text-slate-400">
                        <Award className="w-3.5 h-3.5 text-emerald-400" /> Sells
                      </div>
                      <div className="text-xs font-semibold text-white mt-0.5">
                        {book.copiesSold || 145} copies sold
                      </div>
                    </div>
                  </div>

                </div>
              </div>

            </div>
          </div>

          {/* Section: About the Writer (Slide 7) */}
          {author && (
            <div className="bg-[#171a21] border border-[#262c3b] rounded-2xl p-5 sm:p-6 shadow-xl">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-base font-bold text-white tracking-tight">About the writer</h2>
                <button
                  onClick={() => toggleFollowAuthor(author.id, author.name)}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                    following
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 hover:bg-emerald-500/30'
                      : 'bg-[#222836] text-slate-200 border border-[#313a4d] hover:bg-blue-600 hover:text-white'
                  }`}
                >
                  {following ? (
                    <>
                      <UserCheck className="w-3.5 h-3.5" /> Following
                    </>
                  ) : (
                    <>
                      <UserPlus className="w-3.5 h-3.5" /> Follow Author
                    </>
                  )}
                </button>
              </div>

              <div className="flex flex-col sm:flex-row gap-4 items-start">
                <div className="w-14 h-14 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-700 text-white font-bold flex items-center justify-center text-lg shrink-0 border-2 border-blue-400/40 shadow-md">
                  {author.name.charAt(0)}
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white">{author.name}</h3>
                  <p className="text-xs text-slate-300 mt-1 leading-relaxed">
                    {author.bio || `${author.name} is an accomplished author recognized for insightful works in this domain.`}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Section: Reviews (Slide 7) */}
          <div className="bg-[#171a21] border border-[#262c3b] rounded-2xl p-5 sm:p-6 shadow-xl space-y-6">
            <h2 className="text-base font-bold text-white tracking-tight">Reviews</h2>

            {/* Leave a review form */}
            <form onSubmit={handleReviewSubmit} className="bg-[#121418] border border-[#262c3b] rounded-xl p-4 space-y-3">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-300">Leave Your Review</label>
                <span className="text-[11px] text-slate-400 font-mono">{comment.length}/100</span>
              </div>

              <textarea
                value={comment}
                maxLength={100}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Share your thoughts about this book..."
                rows={3}
                className="w-full bg-[#171a21] border border-[#2e3442] focus:border-blue-500 rounded-lg p-3 text-xs text-white placeholder-slate-500 focus:outline-none resize-none"
              />

              <div className="flex items-center justify-between pt-1">
                {/* 5-Star interactive rating */}
                <div className="flex items-center gap-1">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      type="button"
                      key={star}
                      onClick={() => setRating(star)}
                      onMouseEnter={() => setHoverRating(star)}
                      onMouseLeave={() => setHoverRating(0)}
                      className="p-1 focus:outline-none transition-transform hover:scale-110"
                    >
                      <Star
                        className={`w-4 h-4 ${
                          (hoverRating || rating) >= star
                            ? 'text-amber-400 fill-amber-400'
                            : 'text-slate-600'
                        }`}
                      />
                    </button>
                  ))}
                  <span className="text-xs text-slate-400 ml-1.5 font-medium">{rating} Stars</span>
                </div>

                <button
                  type="submit"
                  disabled={submittingReview}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition-colors disabled:opacity-50"
                >
                  <span>Submit</span>
                  <Send className="w-3.5 h-3.5" />
                </button>
              </div>
            </form>

            {/* List of customer reviews */}
            <div className="space-y-3 pt-2">
              {reviews.length > 0 ? (
                reviews.map((rev) => (
                  <div key={rev.id} className="p-3.5 rounded-xl bg-[#14161c] border border-[#222733] space-y-1.5">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-slate-700 text-slate-200 text-xs flex items-center justify-center font-bold">
                          {rev.userName ? rev.userName.charAt(0).toUpperCase() : 'U'}
                        </div>
                        <span className="text-xs font-semibold text-slate-200">{rev.userName}</span>
                      </div>
                      <div className="flex items-center gap-0.5 text-amber-400">
                        {Array.from({ length: rev.rating }).map((_, i) => (
                          <Star key={i} className="w-3 h-3 fill-amber-400" />
                        ))}
                      </div>
                    </div>
                    <p className="text-xs text-slate-300 leading-relaxed pl-8">
                      {rev.comment}
                    </p>
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-500 italic">No reviews yet. Be the first to review this book!</p>
              )}
            </div>
          </div>

        </div>

        {/* Right Sidebar: Related Reads (Slide 7) */}
        <div className="lg:col-span-4 space-y-4">
          <div className="sticky top-20 bg-[#171a21] border border-[#262c3b] rounded-2xl p-4 shadow-xl">
            <h2 className="text-sm font-bold text-white tracking-tight mb-3.5 flex items-center gap-2">
              <span>Related Reads</span>
            </h2>

            {relatedBooks.length > 0 ? (
              <div className="space-y-3">
                {relatedBooks.map((relBook) => (
                  <BookCard key={relBook.id} book={relBook} />
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-500 py-4 text-center">No related books found.</p>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}
