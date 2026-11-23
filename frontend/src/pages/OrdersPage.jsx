import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { BookCover } from '../components/BookCover';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EmptyState } from '../components/EmptyState';
import { 
  Package, 
  RotateCcw, 
  ShoppingBag, 
  Calendar, 
  Clock, 
  AlertTriangle, 
  CheckCircle2, 
  XCircle,
  Truck
} from 'lucide-react';

export function OrdersPage() {
  const { isAuthenticated, openAuthModal } = useAuth();
  const { refreshCart } = useCart();
  const toast = useToast();
  const navigate = useNavigate();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);
  const [reorderingId, setReorderingId] = useState(null);

  const loadOrders = useCallback(async () => {
    if (!isAuthenticated) {
      setOrders([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const res = await api.orders.list({ size: 50 });
      setOrders(res?.content || []);
    } catch (err) {
      console.error('Failed to load orders', err);
      toast.error('Could not load order history');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const handleCancelOrder = async (orderId) => {
    if (!window.confirm('Are you sure you want to cancel this order? This will process a full refund within 48 hours.')) {
      return;
    }
    setCancellingId(orderId);
    try {
      const updatedOrder = await api.orders.cancel(orderId);
      setOrders((prev) => prev.map((o) => (o.id === orderId ? updatedOrder : o)));
      toast.success(`Order #${orderId} has been successfully cancelled`);
    } catch (err) {
      toast.error(err.message || 'Failed to cancel order (48-hour cancellation period may have passed)');
    } finally {
      setCancellingId(null);
    }
  };

  const handleReorder = async (orderId) => {
    setReorderingId(orderId);
    try {
      const res = await api.orders.reorder(orderId);
      await refreshCart();
      toast.success('Items added to your cart for re-order!');
      navigate('/checkout');
    } catch (err) {
      toast.error(err.message || 'Failed to reorder items');
    } finally {
      setReorderingId(null);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PAID':
        return (
          <span className="flex items-center gap-1 text-[11px] font-semibold text-emerald-400 bg-emerald-950/60 border border-emerald-800/40 px-2.5 py-0.5 rounded-full">
            <CheckCircle2 className="w-3 h-3" /> Confirmed & Paid
          </span>
        );
      case 'PLACED':
        return (
          <span className="flex items-center gap-1 text-[11px] font-semibold text-amber-300 bg-amber-950/60 border border-amber-800/40 px-2.5 py-0.5 rounded-full">
            <Clock className="w-3 h-3" /> Placed (Awaiting Payment)
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="flex items-center gap-1 text-[11px] font-semibold text-rose-400 bg-rose-950/60 border border-rose-800/40 px-2.5 py-0.5 rounded-full">
            <XCircle className="w-3 h-3" /> Cancelled
          </span>
        );
      default:
        return (
          <span className="text-[11px] font-medium text-slate-300 bg-slate-800 px-2.5 py-0.5 rounded-full">
            {status}
          </span>
        );
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

  if (!isAuthenticated) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <EmptyState
          icon={Package}
          title="Sign in to view your Order History"
          description="Track active shipments, cancel orders within 48 hours, or use Buy It Again to reorder your favorite titles."
          actionText="Sign In / Register"
          onAction={() => openAuthModal('login')}
        />
      </div>
    );
  }

  if (loading) {
    return <LoadingSpinner text="Loading your orders..." />;
  }

  if (orders.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <EmptyState
          icon={Package}
          title="No Orders Found"
          description="You haven't placed any orders yet. Discover great reads in our bookstore!"
          actionText="Explore Books"
          actionTo="/"
        />
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-[#262c3b] pb-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Package className="w-5 h-5 text-sky-400" />
            <span>My Orders & Purchase History</span>
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Manage your orders, track deliveries, or cancel within 48 hours of purchase.
          </p>
        </div>
        <span className="text-xs text-slate-400 self-start sm:self-auto bg-[#171a21] px-3 py-1 rounded-lg border border-[#262c3b]">
          {orders.length} orders total
        </span>
      </div>

      <div className="space-y-6">
        {orders.map((order) => {
          const orderDate = new Date(order.createdAt).toLocaleDateString('en-GB', {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          });

          return (
            <div
              key={order.id}
              className="bg-[#171a21] border border-[#262c3b] rounded-2xl overflow-hidden shadow-xl"
            >
              {/* Card Header */}
              <div className="bg-[#121418] border-b border-[#262c3b] p-4 sm:px-6 flex flex-wrap items-center justify-between gap-4 text-xs">
                <div className="flex items-center gap-4 flex-wrap">
                  <div>
                    <span className="text-slate-400">Order ID: </span>
                    <span className="font-mono font-bold text-white">#{order.id}</span>
                  </div>
                  <div className="text-slate-400">
                    Placed on <span className="text-slate-200">{orderDate}</span>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  {getStatusBadge(order.status)}
                </div>
              </div>

              {/* Items List */}
              <div className="p-4 sm:p-6 space-y-4">
                <div className="divide-y divide-[#262c3b]/60 space-y-4">
                  {order.items?.map((item, idx) => (
                    <div
                      key={idx}
                      className={`flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 ${
                        idx > 0 ? 'pt-4' : ''
                      }`}
                    >
                      <div className="flex items-center gap-3.5 flex-1 min-w-0">
                        <BookCover
                          book={{ title: item.title, author: item.authorName }}
                          className="w-16 h-24 shrink-0"
                        />
                        <div className="min-w-0 flex-1">
                          <Link
                            to={`/books/${item.bookId}`}
                            className="text-sm font-semibold text-white hover:text-sky-300 transition-colors line-clamp-1"
                          >
                            {item.title}
                          </Link>
                          <p className="text-xs text-sky-400 mt-0.5">by {item.authorName}</p>
                          <div className="flex items-center gap-3 mt-1.5 text-xs text-slate-400">
                            <span>Qty: <strong className="text-slate-200">{item.quantity}</strong></span>
                            <span>Price: <strong className="text-slate-200">₹{Number(item.unitPrice).toFixed(0)}</strong></span>
                          </div>
                          <div className="flex items-center gap-1.5 text-[11px] text-emerald-400 mt-1">
                            <Truck className="w-3.5 h-3.5" />
                            <span>{formatDeliveryDate(item.estimatedDeliveryDate)}</span>
                          </div>
                        </div>
                      </div>

                      <div className="text-right sm:self-center">
                        <span className="text-sm font-bold text-white">
                          ₹{Number(item.lineTotal || item.unitPrice * item.quantity).toFixed(0)}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Footer bar with Total & Actions ("Buy it again" & "Cancel order within 48 hrs") */}
                <div className="pt-4 mt-4 border-t border-[#262c3b] flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 text-xs">
                  <div>
                    <span className="text-slate-400">Total Paid: </span>
                    <span className="text-base font-bold text-white">
                      ₹{Number(order.price?.total || 0).toFixed(0)}
                    </span>
                    {order.price?.pointsRedeemed > 0 && (
                      <span className="text-[11px] text-amber-300 ml-2">
                        ({order.price.pointsRedeemed} gift pts redeemed)
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2.5">
                    {/* 48-Hour Cancellation Button */}
                    {order.cancellable && order.status !== 'CANCELLED' && (
                      <button
                        onClick={() => handleCancelOrder(order.id)}
                        disabled={cancellingId === order.id}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/30 transition-colors disabled:opacity-50"
                        title="Cancel within 48 hours of placing order"
                      >
                        <RotateCcw className="w-3.5 h-3.5" />
                        <span>{cancellingId === order.id ? 'Cancelling...' : 'Cancel Order (48 hrs)'}</span>
                      </button>
                    )}

                    {/* Buy It Again / Reorder Feature */}
                    <button
                      onClick={() => handleReorder(order.id)}
                      disabled={reorderingId === order.id}
                      className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white font-semibold transition-colors disabled:opacity-50 shadow-md shadow-blue-500/20"
                    >
                      <ShoppingBag className="w-3.5 h-3.5" />
                      <span>{reorderingId === order.id ? 'Adding to cart...' : 'Buy It Again'}</span>
                    </button>
                  </div>

                </div>

              </div>
            </div>
          );
        })}
      </div>

    </div>
  );
}
