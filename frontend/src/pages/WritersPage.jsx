import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useWishlist } from '../context/WishlistContext';
import { BookCard } from '../components/BookCard';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EmptyState } from '../components/EmptyState';
import { Users, UserCheck, UserPlus, BookOpen } from 'lucide-react';

export function WritersPage() {
  const [searchParams] = useSearchParams();
  const highlightAuthorId = searchParams.get('authorId');

  const { isAuthenticated, openAuthModal } = useAuth();
  const { followedAuthorIds, toggleFollowAuthor, isFollowing } = useWishlist();

  const [followedWriters, setFollowedWriters] = useState([]);
  const [authorBooks, setAuthorBooks] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadWritersData() {
      setLoading(true);
      try {
        if (isAuthenticated) {
          const list = await api.engagement.getWriters();
          setFollowedWriters(list || []);

          // Fetch books by these authors
          if (list && list.length > 0) {
            const booksMap = {};
            await Promise.all(
              list.map(async (w) => {
                const res = await api.catalogue.getBooks({ authorId: w.id, size: 4 });
                booksMap[w.id] = res?.content || [];
              })
            );
            setAuthorBooks(booksMap);
          }
        }
      } catch (err) {
        console.error('Failed to load followed writers', err);
      } finally {
        setLoading(false);
      }
    }
    loadWritersData();
  }, [isAuthenticated, followedAuthorIds]);

  if (!isAuthenticated) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <EmptyState
          icon={Users}
          title="Sign in to view My Writers"
          description="Follow your favorite authors to see their latest releases and dedicated catalogues."
          actionText="Sign In / Register"
          onAction={() => openAuthModal('login')}
        />
      </div>
    );
  }

  if (loading) {
    return <LoadingSpinner text="Loading your followed authors..." />;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      <div className="flex items-center justify-between border-b border-[#262c3b] pb-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Users className="w-5 h-5 text-emerald-400" />
            <span>My Writers</span>
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Authors you follow and their published titles.
          </p>
        </div>
        <span className="text-xs text-slate-400 bg-[#171a21] px-3 py-1 rounded-lg border border-[#262c3b]">
          {followedWriters.length} followed {followedWriters.length === 1 ? 'writer' : 'writers'}
        </span>
      </div>

      {followedWriters.length > 0 ? (
        <div className="space-y-8">
          {followedWriters.map((writer) => {
            const isHighlighted = highlightAuthorId && Number(highlightAuthorId) === writer.id;
            const books = authorBooks[writer.id] || [];

            return (
              <div
                key={writer.id}
                className={`bg-[#171a21] border rounded-2xl p-5 sm:p-6 shadow-xl transition-all ${
                  isHighlighted ? 'border-sky-500/70 ring-1 ring-sky-500/50' : 'border-[#262c3b]'
                }`}
              >
                {/* Author Info Header */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-[#262c3b]">
                  <div className="flex items-center gap-3.5">
                    <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-700 text-white font-bold flex items-center justify-center text-lg shrink-0">
                      {writer.name.charAt(0)}
                    </div>
                    <div>
                      <h2 className="text-base font-bold text-white">{writer.name}</h2>
                      <p className="text-xs text-slate-300 mt-0.5 max-w-xl leading-relaxed">
                        {writer.bio}
                      </p>
                    </div>
                  </div>

                  <button
                    onClick={() => toggleFollowAuthor(writer.id, writer.name)}
                    className="self-start sm:self-auto flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 hover:bg-rose-500/20 hover:text-rose-300 hover:border-rose-500/40 transition-colors group"
                  >
                    <UserCheck className="w-3.5 h-3.5 group-hover:hidden" />
                    <span className="group-hover:hidden">Following</span>
                    <span className="hidden group-hover:inline">Unfollow</span>
                  </button>
                </div>

                {/* Author's Books */}
                <div className="mt-4">
                  <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3 flex items-center gap-1.5">
                    <BookOpen className="w-3.5 h-3.5 text-sky-400" /> Books by {writer.name}
                  </h3>

                  {books.length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
                      {books.map((book) => (
                        <BookCard key={book.id} book={book} />
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 italic">No books listed.</p>
                  )}
                </div>

              </div>
            );
          })}
        </div>
      ) : (
        <EmptyState
          icon={Users}
          title="You are not following any writers yet"
          description="Explore book details and follow your favorite authors to stay updated with their works."
          actionText="Discover Books"
          actionTo="/"
        />
      )}

    </div>
  );
}
