import React, { useState } from 'react';

// Generates beautiful styled book covers matching Slide 5-10 wireframe visuals
export function BookCover({ book, className = "w-28 h-40", size = "normal" }) {
  const [imageError, setImageError] = useState(false);
  const title = book?.title || 'Book Title';
  const author = book?.author?.name || (typeof book?.author === 'string' ? book?.author : 'Author');
  const isbn = book?.isbn || '';

  // Theme palettes based on title or ISBN to match the exact visual style in slides
  const getCoverTheme = (str = '') => {
    const s = str.toLowerCase();
    if (s.includes('minimalism')) {
      return {
        bg: 'from-amber-400 to-yellow-500',
        text: 'text-dark-900',
        sub: 'text-dark-800',
        accent: 'bg-dark-900',
        icon: 'arch',
      };
    }
    if (s.includes('focus')) {
      return {
        bg: 'from-slate-100 to-slate-200',
        text: 'text-slate-900',
        sub: 'text-slate-700',
        accent: 'bg-red-500',
        icon: 'target',
      };
    }
    if (s.includes('learning')) {
      return {
        bg: 'from-red-600 to-rose-700',
        text: 'text-white',
        sub: 'text-rose-100',
        accent: 'bg-amber-300',
        icon: 'bulb',
      };
    }
    if (s.includes('success')) {
      return {
        bg: 'from-teal-800 to-slate-900',
        text: 'text-amber-300',
        sub: 'text-teal-100',
        accent: 'bg-amber-400',
        icon: 'mountain',
      };
    }
    if (s.includes('midnight') || s.includes('heist')) {
      return {
        bg: 'from-slate-900 via-indigo-950 to-blue-950',
        text: 'text-cyan-200',
        sub: 'text-slate-300',
        accent: 'bg-cyan-400',
        icon: 'moon',
      };
    }
    if (s.includes('stars')) {
      return {
        bg: 'from-purple-900 via-indigo-900 to-slate-950',
        text: 'text-purple-100',
        sub: 'text-purple-200',
        accent: 'bg-pink-400',
        icon: 'stars',
      };
    }
    if (s.includes('frontier') || s.includes('cosmos') || s.includes('kepler')) {
      return {
        bg: 'from-sky-950 via-slate-900 to-indigo-950',
        text: 'text-sky-200',
        sub: 'text-sky-300',
        accent: 'bg-sky-400',
        icon: 'planet',
      };
    }
    if (s.includes('house') || s.includes('witness') || s.includes('cold')) {
      return {
        bg: 'from-emerald-950 via-slate-900 to-teal-950',
        text: 'text-emerald-200',
        sub: 'text-emerald-300',
        accent: 'bg-emerald-400',
        icon: 'house',
      };
    }
    if (s.includes('kitten') || s.includes('rainy') || s.includes('moon who')) {
      return {
        bg: 'from-orange-400 to-amber-500',
        text: 'text-dark-900',
        sub: 'text-amber-950',
        accent: 'bg-amber-900',
        icon: 'paw',
      };
    }
    if (s.includes('chennai') || s.includes('monsoon') || s.includes('spice')) {
      return {
        bg: 'from-rose-900 via-amber-950 to-orange-950',
        text: 'text-amber-200',
        sub: 'text-rose-100',
        accent: 'bg-amber-400',
        icon: 'floral',
      };
    }
    // Default palette generator
    const hues = [
      { bg: 'from-blue-900 to-indigo-950', text: 'text-blue-100', sub: 'text-blue-200', accent: 'bg-blue-400', icon: 'book' },
      { bg: 'from-emerald-900 to-teal-950', text: 'text-emerald-100', sub: 'text-emerald-200', accent: 'bg-teal-400', icon: 'leaf' },
      { bg: 'from-amber-700 to-stone-900', text: 'text-amber-100', sub: 'text-amber-200', accent: 'bg-amber-400', icon: 'feather' },
      { bg: 'from-violet-900 to-fuchsia-950', text: 'text-violet-100', sub: 'text-violet-200', accent: 'bg-fuchsia-400', icon: 'star' },
    ];
    let hash = 0;
    for (let i = 0; i < str.length; i++) hash = (hash << 5) - hash + str.charCodeAt(i);
    return hues[Math.abs(hash) % hues.length];
  };

  const theme = getCoverTheme(title);

  // If a valid image URL exists and doesn't fail, we could show it, but the seed URLs are example placeholders (https://images.example.com/...).
  // Showing the stylized cover artwork provides the exact high-fidelity dark UI from the slides.
  const hasRealImage = book?.coverImageUrl && !book.coverImageUrl.includes('example.com') && !imageError;

  if (hasRealImage) {
    return (
      <div className={`relative shrink-0 overflow-hidden rounded-md book-shadow ${className}`}>
        <img
          src={book.coverImageUrl}
          alt={title}
          onError={() => setImageError(true)}
          className="w-full h-full object-cover"
        />
      </div>
    );
  }

  return (
    <div
      className={`relative shrink-0 overflow-hidden rounded-md bg-gradient-to-b ${theme.bg} book-shadow flex flex-col justify-between p-2.5 select-none transition-transform duration-200 ${className}`}
    >
      {/* Book spine line styling */}
      <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-black/20 border-r border-white/10" />

      {/* Top Header / Title */}
      <div className="relative pl-2 pt-0.5">
        <h4 className={`font-bold uppercase tracking-wider leading-tight text-center ${theme.text} ${size === 'large' ? 'text-lg' : 'text-[11px]'}`}>
          {title}
        </h4>
      </div>

      {/* Center Icon Graphic */}
      <div className="relative flex items-center justify-center my-auto py-1">
        {theme.icon === 'arch' && (
          <div className="w-8 h-8 rounded-t-full border-4 border-dark-900 border-b-0 flex items-end justify-center">
            <div className="w-3 h-3 rounded-t-full border-2 border-dark-900 border-b-0 mb-0.5" />
          </div>
        )}
        {theme.icon === 'target' && (
          <div className="w-8 h-8 rounded-full border-2 border-red-500 flex items-center justify-center">
            <div className="w-4 h-4 rounded-full border-2 border-red-500 flex items-center justify-center">
              <div className="w-1.5 h-1.5 rounded-full bg-red-500" />
            </div>
          </div>
        )}
        {theme.icon === 'bulb' && (
          <div className="flex flex-col items-center">
            <div className="w-6 h-6 rounded-full bg-amber-300 shadow-[0_0_12px_rgba(252,211,77,0.8)]" />
            <div className="w-3 h-1.5 bg-amber-200 rounded-b mt-0.5" />
          </div>
        )}
        {theme.icon === 'mountain' && (
          <div className="relative w-8 h-8 flex items-end justify-center">
            <div className="w-0 h-0 border-l-[14px] border-l-transparent border-r-[14px] border-r-transparent border-b-[20px] border-b-amber-400" />
            <div className="absolute -top-1 w-2 h-2 rounded-full bg-amber-300" />
          </div>
        )}
        {theme.icon === 'moon' && (
          <div className="w-7 h-7 rounded-full bg-cyan-200 shadow-[0_0_10px_rgba(103,232,249,0.7)]" />
        )}
        {theme.icon === 'stars' && (
          <div className="flex gap-1 items-center">
            <div className="w-2 h-2 rounded-full bg-pink-300 animate-pulse" />
            <div className="w-4 h-4 rounded-full bg-pink-200" />
            <div className="w-2 h-2 rounded-full bg-pink-300" />
          </div>
        )}
        {theme.icon === 'planet' && (
          <div className="relative w-7 h-7 rounded-full bg-sky-400 flex items-center justify-center">
            <div className="absolute w-10 h-3 border border-sky-200 rounded-full rotate-[-25deg]" />
          </div>
        )}
        {theme.icon === 'house' && (
          <div className="w-6 h-6 bg-emerald-400/80 [clip-path:polygon(50%_0%,100%_40%,100%_100%,0%_100%,0%_40%)]" />
        )}
        {theme.icon === 'paw' && (
          <div className="w-7 h-7 rounded-full bg-dark-900 flex items-center justify-center text-amber-400 font-bold text-xs">
            🐾
          </div>
        )}
        {['book', 'leaf', 'feather', 'floral', 'star'].includes(theme.icon) && (
          <div className="w-6 h-6 rounded-full border border-white/30 flex items-center justify-center">
            <div className="w-2 h-2 rounded-full bg-white/60" />
          </div>
        )}
      </div>

      {/* Bottom Author Name */}
      <div className="relative pl-2 text-center pb-0.5">
        <p className={`text-[9px] font-semibold tracking-wider uppercase truncate ${theme.sub}`}>
          {author}
        </p>
      </div>

      {/* Subtle glare reflection */}
      <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-bl from-white/10 to-transparent pointer-events-none rounded-tr" />
    </div>
  );
}
