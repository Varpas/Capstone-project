import React from 'react';
import { useLocation, Link, Navigate } from 'react-router-dom';
import { CheckCircle2, ShoppingBag, Package, Calendar, ArrowRight } from 'lucide-react';
import { BookCover } from '../components/BookCover';

export function ConfirmationPage() {
  const location = useLocation();
  const order = location.state?.order;
  const payment = location.state?.payment;

  if (!order) {
    return <Navigate to="/" replace />;
  }

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
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-12 animate-fadeIn">
      {/* Slide 10 Center Confirmation Card */}
      <div className="bg-[#171a21] border border-[#262c3b] rounded-3xl p-6 sm:p-10 shadow-2xl flex flex-col items-center text-center">
        
        {/* Green Checkmark Circle (Slide 10) */}
        <div className="w-16 h-16 rounded-full bg-emerald-500/20 border-2 border-emerald-500 flex items-center justify-center text-emerald-400 mb-5 shadow-lg shadow-emerald-500/20">
          <CheckCircle2 className="w-9 h-9" />
        </div>

        {/* Headline */}
        <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight max-w-lg">
          Your purchase of the following reads is successful
        </h1>

        <p className="text-xs text-slate-400 mt-2">
          Order Reference: <span className="font-mono text-sky-400 font-semibold">#{order.id}</span>
          {payment?.transactionRef && (
            <span> · Txn: <span className="font-mono text-slate-300">{payment.transactionRef}</span></span>
          )}
        </p>

        {/* Purchased Books List (Slide 10 Cards) */}
        <div className="w-full mt-8 grid grid-cols-1 sm:grid-cols-2 gap-4 text-left">
          {order.items?.map((item, idx) => (
            <div
              key={idx}
              className="flex items-start gap-4 p-4 rounded-2xl bg-[#121418] border border-[#262c3b]"
            >
              <BookCover
                book={{ title: item.title, author: item.authorName }}
                className="w-20 h-28"
              />
              <div className="flex-1 min-w-0 flex flex-col justify-between h-full">
                <div>
                  <h3 className="text-sm font-bold text-white line-clamp-1">{item.title}</h3>
                  <p className="text-xs text-sky-400 mt-0.5">by {item.authorName}</p>
                  <p className="text-[11px] text-slate-400 mt-1 line-clamp-1">
                    Qty: {item.quantity} · Format: Paperback
                  </p>
                </div>
                <div className="mt-3 pt-2 border-t border-[#262c3b]/60 flex items-center justify-between">
                  <span className="text-xs font-bold text-white">
                    ₹{Number(item.lineTotal || item.unitPrice * item.quantity).toFixed(0)}
                  </span>
                  <span className="text-[10px] text-slate-400">
                    {formatDeliveryDate(item.estimatedDeliveryDate)}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Order details summary row */}
        <div className="w-full mt-6 p-4 rounded-xl bg-[#121418]/60 border border-[#262c3b] flex flex-wrap items-center justify-between text-xs text-slate-300 gap-4">
          <div>
            <span className="text-slate-400">Delivering to: </span>
            <span className="font-semibold text-white">
              {order.shippingAddress?.addressLine}, {order.shippingAddress?.city}
            </span>
          </div>
          <div>
            <span className="text-slate-400">Total Paid: </span>
            <span className="font-bold text-emerald-400 text-sm">
              ₹{Number(order.price?.total || payment?.amount || 0).toFixed(0)}
            </span>
          </div>
        </div>

        {/* Action Buttons (Slide 10) */}
        <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
          <Link
            to="/"
            className="flex items-center gap-2 px-6 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white font-semibold text-xs shadow-lg shadow-blue-500/25 transition-all"
          >
            <span>Continue your Shopping</span>
            <ShoppingBag className="w-4 h-4" />
          </Link>

          <Link
            to="/orders"
            className="flex items-center gap-2 px-5 py-3 rounded-xl bg-[#1e222b] hover:bg-[#252a36] border border-[#2e3442] text-slate-300 hover:text-white font-semibold text-xs transition-colors"
          >
            <Package className="w-4 h-4" />
            <span>View in My Orders</span>
          </Link>
        </div>

      </div>
    </div>
  );
}
