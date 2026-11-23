import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { CategorySidebar } from '../components/CategorySidebar';
import { SearchFilterBar } from '../components/SearchFilterBar';
import { BookCard } from '../components/BookCard';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EmptyState } from '../components/EmptyState';
import { Sparkles, TrendingUp, Flame, BookOpen, Layers } from 'lucide-react';

export function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams();

  // Category & Filter states
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(
    searchParams.get('categoryId') ? Number(searchParams.get('categoryId')) : null
  );

  const [filters, setFilters] = useState({
    q: searchParams.get('q') || '',
    language: searchParams.get('language') || '',
    format: searchParams.get('format') || '',
    priceRange: searchParams.get('priceRange') || '',
    sort: searchParams.get('sort') || 'relevance',
  });

  // Home curated sections vs Search results
  const [homeData, setHomeData] = useState({
    recommended: [],
    bestsellers: [],
    newLaunches: [],
  });
  const [searchResults, setSearchResults] = useState(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  // Load categories
  useEffect(() => {
    async function fetchCategories() {
      try {
        const catList = await api.catalogue.getCategories();
        setCategories(catList || []);
      } catch (err) {
        console.error('Failed to load categories', err);
      }
    }
    fetchCategories();
  }, []);

  // Determine if user has active filters / search
  const isFiltered = Boolean(
    selectedCategory !== null ||
    filters.q ||
    filters.language ||
    filters.format ||
    filters.priceRange ||
    filters.sort !== 'relevance'
  );

  // Fetch data depending on filter state
  const loadContent = useCallback(async () => {
    setLoading(true);
    try {
      if (!isFiltered) {
        // Load default curated Home Page (Slide 5 / 6)
        const homeRes = await api.catalogue.getHome(6);
        setHomeData(homeRes || { recommended: [], bestsellers: [], newLaunches: [] });
        setSearchResults(null);
      } else {
        // Parse price range
        let minPrice = undefined;
        let maxPrice = undefined;
        if (filters.priceRange === '0-200') {
          minPrice = 0;
          maxPrice = 200;
        } else if (filters.priceRange === '200-400') {
          minPrice = 200;
          maxPrice = 400;
        } else if (filters.priceRange === '400-1000') {
          minPrice = 400;
        }

        const params = {
          q: filters.q || undefined,
          categoryId: selectedCategory || undefined,
          language: filters.language || undefined,
          format: filters.format || undefined,
          minPrice,
          maxPrice,
          sort: filters.sort || 'relevance',
          page,
          size: 18,
        };

        const res = await api.catalogue.getBooks(params);
        setSearchResults(res);
      }
    } catch (err) {
      console.error('Failed to load catalogue', err);
    } finally {
      setLoading(false);
    }
  }, [isFiltered, selectedCategory, filters, page]);

  useEffect(() => {
    loadContent();
  }, [loadContent]);

  const handleSelectCategory = (catId) => {
    setSelectedCategory(catId);
    setPage(0);
    const newParams = new URLSearchParams(searchParams);
    if (catId) {
      newParams.set('categoryId', catId);
    } else {
      newParams.delete('categoryId');
    }
    setSearchParams(newParams);
  };

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
    setPage(0);
    const newParams = new URLSearchParams(searchParams);
    if (value) {
      newParams.set(key, value);
    } else {
      newParams.delete(key);
    }
    setSearchParams(newParams);
  };

  const handleResetFilters = () => {
    setSelectedCategory(null);
    setFilters({
      q: '',
      language: '',
      format: '',
      priceRange: '',
      sort: 'relevance',
    });
    setSearchParams({});
    setPage(0);
  };

  const activeCategoryObj = categories.find((c) => c.id === selectedCategory);

  return (
    <div className="flex-1 flex flex-col md:flex-row min-h-[calc(100vh-4rem)]">
      
      {/* Category Sidebar (Left side, matching Slide 5/6) */}
      <CategorySidebar
        categories={categories}
        selectedCategoryId={selectedCategory}
        onSelectCategory={handleSelectCategory}
        className="hidden md:block"
      />

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col min-w-0 bg-[#121418]">
        
        {/* Top Search & Filter Bar */}
        <SearchFilterBar
          filters={filters}
          onFilterChange={handleFilterChange}
          onResetFilters={handleResetFilters}
        />

        {/* Mobile category pills */}
        <div className="md:hidden flex items-center gap-1.5 overflow-x-auto p-2.5 bg-[#171a21] border-b border-[#262c3b] scrollbar-none">
          <button
            onClick={() => handleSelectCategory(null)}
            className={`px-3 py-1 rounded-full text-xs shrink-0 font-medium ${
              selectedCategory === null
                ? 'bg-blue-600 text-white'
                : 'bg-[#222836] text-slate-300'
            }`}
          >
            All
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              onClick={() => handleSelectCategory(c.id)}
              className={`px-3 py-1 rounded-full text-xs shrink-0 font-medium ${
                selectedCategory === c.id
                  ? 'bg-blue-600 text-white'
                  : 'bg-[#222836] text-slate-300'
              }`}
            >
              {c.name}
            </button>
          ))}
        </div>

        {/* Content Body */}
        <div className="flex-1 p-4 sm:p-6 space-y-8 max-w-7xl">
          {loading ? (
            <LoadingSpinner text="Fetching books from catalogue..." />
          ) : !isFiltered ? (
            // Default Slide 5/6 Curated Views:
            <>
              {/* Section 1: Recommended for You */}
              {homeData.recommended && homeData.recommended.length > 0 && (
                <section>
                  <div className="flex items-center gap-2 mb-3.5">
                    <Sparkles className="w-4 h-4 text-sky-400" />
                    <h2 className="text-base font-bold text-white tracking-tight">
                      Recommended for You
                    </h2>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
                    {homeData.recommended.map((book) => (
                      <BookCard key={book.id} book={book} />
                    ))}
                  </div>
                </section>
              )}

              {/* Section 2: Bestsellers this Month */}
              {homeData.bestsellers && homeData.bestsellers.length > 0 && (
                <section>
                  <div className="flex items-center gap-2 mb-3.5">
                    <TrendingUp className="w-4 h-4 text-amber-400" />
                    <h2 className="text-base font-bold text-white tracking-tight">
                      Bestsellers this Month
                    </h2>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
                    {homeData.bestsellers.map((book) => (
                      <BookCard key={book.id} book={book} />
                    ))}
                  </div>
                </section>
              )}

              {/* Section 3: New Launches */}
              {homeData.newLaunches && homeData.newLaunches.length > 0 && (
                <section>
                  <div className="flex items-center gap-2 mb-3.5">
                    <Flame className="w-4 h-4 text-rose-400" />
                    <h2 className="text-base font-bold text-white tracking-tight">
                      New Launches
                    </h2>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
                    {homeData.newLaunches.map((book) => (
                      <BookCard key={book.id} book={book} />
                    ))}
                  </div>
                </section>
              )}
            </>
          ) : (
            // Filtered / Search results view
            <section>
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                    {activeCategoryObj ? activeCategoryObj.name : 'Catalogue Results'}
                  </h2>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Found {searchResults?.totalElements || 0} books
                    {filters.q ? ` matching "${filters.q}"` : ''}
                  </p>
                </div>
                <button
                  onClick={handleResetFilters}
                  className="text-xs text-sky-400 hover:text-sky-300 font-medium"
                >
                  Clear all filters
                </button>
              </div>

              {searchResults?.content && searchResults.content.length > 0 ? (
                <>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
                    {searchResults.content.map((book) => (
                      <BookCard key={book.id} book={book} />
                    ))}
                  </div>

                  {/* Pagination */}
                  {searchResults.totalPages > 1 && (
                    <div className="flex items-center justify-center gap-2 mt-8 pt-4 border-t border-[#262c3b]">
                      <button
                        disabled={page === 0}
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                        className="px-3 py-1.5 rounded-lg bg-[#1e222b] text-slate-200 text-xs disabled:opacity-40 hover:bg-[#2a303d]"
                      >
                        Previous
                      </button>
                      <span className="text-xs text-slate-400 px-2">
                        Page {page + 1} of {searchResults.totalPages}
                      </span>
                      <button
                        disabled={page >= searchResults.totalPages - 1}
                        onClick={() => setPage((p) => p + 1)}
                        className="px-3 py-1.5 rounded-lg bg-[#1e222b] text-slate-200 text-xs disabled:opacity-40 hover:bg-[#2a303d]"
                      >
                        Next
                      </button>
                    </div>
                  )}
                </>
              ) : (
                <EmptyState
                  title="No books matched your criteria"
                  description="Try clearing your filters or searching with another keyword."
                  onAction={handleResetFilters}
                  actionText="Reset filters"
                />
              )}
            </section>
          )}
        </div>

      </main>
    </div>
  );
}
