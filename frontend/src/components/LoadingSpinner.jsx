import React from 'react';
import { Loader2 } from 'lucide-react';

export function LoadingSpinner({ text = 'Loading...' }) {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-slate-400">
      <Loader2 className="w-8 h-8 animate-spin text-blue-500 mb-2" />
      <p className="text-xs font-medium tracking-wide">{text}</p>
    </div>
  );
}
