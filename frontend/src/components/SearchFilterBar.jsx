import React from 'react';
import { Search, SlidersHorizontal, X } from 'lucide-react';

export function SearchFilterBar({
  filters,
  onFilterChange,
  onResetFilters,
}) {
  const { q = '', language = '', format = '', priceRange = '', sort = 'relevance' } = filters;

  return (
    <div className="bg-[#171a21]/80 backdrop-blur border-b border-[#262c3b] p-3 sm:px-4">
      <div className="flex flex-col lg:flex-row items-stretch lg:items-center gap-2.5">
        
        {/* Search input */}
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={q}
            onChange={(e) => onFilterChange('q', e.target.value)}
            placeholder="Search you want to read here"
            className="w-full bg-[#121418] border border-[#2e3442] hover:border-[#3d4659] focus:border-blue-500 rounded-xl pl-9 pr-8 py-2 text-xs sm:text-sm text-white placeholder-slate-400 transition-colors focus:outline-none"
          />
          {q && (
            <button
              onClick={() => onFilterChange('q', '')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white p-0.5"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Filters and Sort Row */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
          
          {/* Language dropdown */}
          <div className="flex flex-col">
            <label className="text-[10px] text-slate-400 uppercase font-semibold px-1 mb-0.5">Language</label>
            <select
              value={language}
              onChange={(e) => onFilterChange('language', e.target.value)}
              className="bg-[#121418] border border-[#2e3442] rounded-lg px-2.5 py-1.5 text-slate-200 focus:outline-none focus:border-blue-500"
            >
              <option value="">All</option>
              <option value="English">English</option>
              <option value="Spanish">Spanish</option>
            </select>
          </div>

          {/* Format dropdown */}
          <div className="flex flex-col">
            <label className="text-[10px] text-slate-400 uppercase font-semibold px-1 mb-0.5">Format</label>
            <select
              value={format}
              onChange={(e) => onFilterChange('format', e.target.value)}
              className="bg-[#121418] border border-[#2e3442] rounded-lg px-2.5 py-1.5 text-slate-200 focus:outline-none focus:border-blue-500"
            >
              <option value="">All (Paperback, ebook etc)</option>
              <option value="PAPERBACK">Paperback</option>
              <option value="HARDCOVER">Hardcover</option>
              <option value="EBOOK">eBook</option>
            </select>
          </div>

          {/* Price Range */}
          <div className="flex flex-col">
            <label className="text-[10px] text-slate-400 uppercase font-semibold px-1 mb-0.5">Price Range</label>
            <select
              value={priceRange}
              onChange={(e) => onFilterChange('priceRange', e.target.value)}
              className="bg-[#121418] border border-[#2e3442] rounded-lg px-2.5 py-1.5 text-slate-200 focus:outline-none focus:border-blue-500"
            >
              <option value="">All</option>
              <option value="0-200">Under ₹200</option>
              <option value="200-400">₹200 to ₹400</option>
              <option value="400-1000">₹400 & Above</option>
            </select>
          </div>

          {/* Sort By */}
          <div className="flex flex-col">
            <label className="text-[10px] text-slate-400 uppercase font-semibold px-1 mb-0.5">Sort by</label>
            <select
              value={sort}
              onChange={(e) => onFilterChange('sort', e.target.value)}
              className="bg-[#121418] border border-[#2e3442] rounded-lg px-2.5 py-1.5 text-slate-200 focus:outline-none focus:border-blue-500"
            >
              <option value="relevance">Relevance</option>
              <option value="price_asc">Price: Low to High</option>
              <option value="price_desc">Price: High to Low</option>
              <option value="rating">Top Rated</option>
              <option value="newest">Newest</option>
            </select>
          </div>

        </div>

      </div>
    </div>
  );
}
