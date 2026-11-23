import React from 'react';
import { Layers } from 'lucide-react';

export function CategorySidebar({
  categories = [],
  selectedCategoryId,
  onSelectCategory,
  className = '',
}) {
  return (
    <aside
      className={`w-56 shrink-0 bg-[#171a21]/95 border-r border-[#262c3b] p-3 flex flex-col h-full overflow-y-auto ${className}`}
    >
      <div className="flex items-center gap-2 px-2 py-2 mb-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
        <Layers className="w-4 h-4 text-sky-400" />
        <span>Categories</span>
      </div>

      <div className="space-y-0.5">
        {/* 'All' option */}
        <button
          onClick={() => onSelectCategory(null)}
          className={`w-full text-left px-3 py-2 rounded-lg text-xs font-medium transition-all duration-150 flex items-center justify-between ${
            selectedCategoryId === null
              ? 'bg-blue-600/20 text-sky-300 font-semibold border-l-2 border-sky-400 pl-2.5'
              : 'text-slate-300 hover:text-white hover:bg-[#202530]'
          }`}
        >
          <span>All</span>
        </button>

        {/* Dynamic / Seed Categories */}
        {categories.map((cat) => {
          const isSelected = selectedCategoryId === cat.id;
          return (
            <button
              key={cat.id}
              onClick={() => onSelectCategory(cat.id)}
              className={`w-full text-left px-3 py-2 rounded-lg text-xs font-medium transition-all duration-150 flex items-center justify-between ${
                isSelected
                  ? 'bg-blue-600/20 text-sky-300 font-semibold border-l-2 border-sky-400 pl-2.5'
                  : 'text-slate-300 hover:text-white hover:bg-[#202530]'
              }`}
            >
              <span className="truncate">{cat.name}</span>
            </button>
          );
        })}
      </div>
    </aside>
  );
}
