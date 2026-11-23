import React from 'react';
import { BookOpen, ShieldCheck, Truck, RotateCcw, Award } from 'lucide-react';

export function Footer() {
  return (
    <footer className="mt-auto bg-[#0e1014] border-t border-[#222733] text-slate-400 text-xs">
      {/* Value props banner */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 border-b border-[#1f2430]">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-blue-500/10 text-sky-400">
              <Truck className="w-5 h-5" />
            </div>
            <div>
              <p className="font-semibold text-white">Free Fast Delivery</p>
              <p className="text-[11px] text-slate-400">On all orders above ₹500</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-emerald-500/10 text-emerald-400">
              <RotateCcw className="w-5 h-5" />
            </div>
            <div>
              <p className="font-semibold text-white">48-Hr Easy Cancel</p>
              <p className="text-[11px] text-slate-400">No questions asked</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-amber-500/10 text-amber-400">
              <Award className="w-5 h-5" />
            </div>
            <div>
              <p className="font-semibold text-white">Gift Reward Points</p>
              <p className="text-[11px] text-slate-400">Earn 1 pt per ₹100 spent</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-purple-500/10 text-purple-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <p className="font-semibold text-white">100% Secure Checkout</p>
              <p className="text-[11px] text-slate-400">Cards, UPI & Wallets</p>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <BookOpen className="w-4 h-4 text-blue-500" />
          <span className="font-semibold text-slate-200">Book Worm</span>
          <span>— Fullstack E-Bookstore Platform</span>
        </div>
        <div className="text-slate-400 text-[11px]">
          Built with IBM Bob agentic workflows · Spring Boot + React + Tailwind CSS
        </div>
      </div>
    </footer>
  );
}
