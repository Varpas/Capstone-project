import React from 'react';
import { BookX } from 'lucide-react';
import { Link } from 'react-router-dom';

export function EmptyState({
  title = 'No books found',
  description = 'Try adjusting your search query or filter criteria.',
  actionText = 'Browse all books',
  actionTo = '/',
  onAction,
  icon: Icon = BookX,
}) {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center max-w-md mx-auto">
      <div className="w-14 h-14 rounded-2xl bg-[#1e222b] border border-[#2e3442] flex items-center justify-center text-slate-400 mb-4 shadow-inner">
        <Icon className="w-7 h-7 text-slate-400" />
      </div>
      <h3 className="text-base font-semibold text-white mb-1">{title}</h3>
      <p className="text-xs text-slate-400 mb-5 leading-relaxed">{description}</p>
      {onAction ? (
        <button
          onClick={onAction}
          className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-md transition-colors"
        >
          {actionText}
        </button>
      ) : actionTo ? (
        <Link
          to={actionTo}
          className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-md transition-colors"
        >
          {actionText}
        </Link>
      ) : null}
    </div>
  );
}
