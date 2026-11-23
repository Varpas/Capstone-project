import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { BookCover } from '../components/BookCover';
import { PaymentModal } from '../components/PaymentModal';
import { EmptyState } from '../components/EmptyState';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { 
  ShoppingCart, 
  Trash2, 
  Plus, 
  Minus, 
  ChevronRight, 
  MapPin, 
  Tag, 
  Gift, 
  CreditCard,
  ShieldCheck,
  Check
} from 'lucide-react';

export function CheckoutPage() {
  const navigate = useNavigate();
  const { user, isAuthenticated, openAuthModal, refreshUser } = useAuth();
  const { cart, updateQuantity, removeFromCart, clearCart, refreshCart } = useCart();
  const toast = useToast();

  // Saved addresses state
  const [savedAddresses, setSavedAddresses] = useState([]);
  const [useSavedAddress, setUseSavedAddress] = useState(false);
  const [selectedAddressId, setSelectedAddressId] = useState(null);

  // Address form fields (Slide 8) - predefined with demo values
  const [addressForm, setAddressForm] = useState({
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com',
    phoneCountryCode: '+91',
    phone: '9876543210',
    addressLine: '221B MG Road, Indiranagar',
    city: 'Bengaluru',
    state: 'Karnataka',
    pinCode: '560001',
    country: 'India',
  });
  const [saveAddressToAccount, setSaveAddressToAccount] = useState(true);

  // Quote / pricing calculation state
  const [couponCode, setCouponCode] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState('');
  const [pointsToRedeem, setPointsToRedeem] = useState('');
  const [appliedPoints, setAppliedPoints] = useState(0);

  const [priceBreakdown, setPriceBreakdown] = useState(null);
  const [quoteLoading, setQuoteLoading] = useState(false);

  // Payment modal state
  const [paymentModalOpen, setPaymentModalOpen] = useState(false);
  const [isProcessingPayment, setIsProcessingPayment] = useState(false);

  // Pre-fill user profile info if logged in
  useEffect(() => {
    if (user) {
      setAddressForm((prev) => ({
        ...prev,
        firstName: user.firstName || prev.firstName,
        lastName: user.lastName || prev.lastName,
        email: user.email || prev.email,
        phone: user.phone?.replace(/^\+91/, '') || prev.phone,
      }));
    }
  }, [user]);

  // Load saved addresses
  useEffect(() => {
    async function loadAddresses() {
      if (isAuthenticated) {
        try {
          const list = await api.addresses.list();
          setSavedAddresses(list || []);
          if (list && list.length > 0) {
            const defaultAddr = list.find((a) => a.isDefault) || list[0];
            setSelectedAddressId(defaultAddr.id);
            setUseSavedAddress(true);
            applyAddressObj(defaultAddr);
          }
        } catch (err) {
          console.warn('Could not load addresses', err);
        }
      }
    }
    loadAddresses();
  }, [isAuthenticated]);

  const applyAddressObj = (addr) => {
    if (!addr) return;
    setAddressForm({
      firstName: addr.firstName || '',
      lastName: addr.lastName || '',
      email: addr.email || '',
      phoneCountryCode: addr.phoneCountryCode || '+91',
      phone: addr.phone || '',
      addressLine: addr.addressLine || '',
      city: addr.city || '',
      state: addr.state || '',
      pinCode: addr.pinCode || '',
      country: addr.country || 'India',
    });
  };

  // Re-fetch pricing quote whenever coupon or points change
  const fetchQuote = useCallback(async () => {
    if (!cart?.items || cart.items.length === 0 || !isAuthenticated) {
      setPriceBreakdown(null);
      return;
    }
    setQuoteLoading(true);
    try {
      const quote = await api.checkout.quote({
        couponCode: appliedCoupon || undefined,
        pointsToRedeem: appliedPoints > 0 ? appliedPoints : undefined,
      });
      setPriceBreakdown(quote);
    } catch (err) {
      console.warn('Quote error:', err);
      // Fallback estimate
      const subtotal = Number(cart.subtotal || 0);
      const tax = subtotal * 0.12;
      const delivery = subtotal >= 500 ? 0 : 40;
      setPriceBreakdown({
        itemCount: cart.itemCount,
        subtotal,
        tax,
        deliveryCharge: delivery,
        couponCode: appliedCoupon,
        couponDiscount: 0,
        pointsRedeemed: 0,
        pointsDiscount: 0,
        total: subtotal + tax + delivery,
      });
    } finally {
      setQuoteLoading(false);
    }
  }, [cart, isAuthenticated, appliedCoupon, appliedPoints]);

  useEffect(() => {
    fetchQuote();
  }, [fetchQuote]);

  const handleApplyCoupon = (e) => {
    e.preventDefault();
    if (!couponCode.trim()) return;
    setAppliedCoupon(couponCode.trim().toUpperCase());
    toast.info(`Applied coupon: ${couponCode.trim().toUpperCase()}`);
  };

  const handleApplyPoints = (e) => {
    e.preventDefault();
    const num = parseInt(pointsToRedeem, 10);
    if (isNaN(num) || num <= 0) {
      setAppliedPoints(0);
      return;
    }
    const maxAvailable = user?.giftPoints || 0;
    if (num > maxAvailable) {
      toast.error(`You only have ${maxAvailable} gift points available`);
      return;
    }
    setAppliedPoints(num);
    toast.success(`Redeemed ${num} gift points (-₹${num})`);
  };

  const handleSavedAddressChange = (addrId) => {
    setSelectedAddressId(addrId);
    const addr = savedAddresses.find((a) => a.id === Number(addrId));
    if (addr) applyAddressObj(addr);
  };

  const validateAddress = () => {
    if (
      !addressForm.firstName.trim() ||
      !addressForm.lastName.trim() ||
      !addressForm.email.trim() ||
      !addressForm.phone.trim() ||
      !addressForm.addressLine.trim() ||
      !addressForm.city.trim() ||
      !addressForm.state.trim() ||
      !addressForm.pinCode.trim()
    ) {
      toast.error('Please fill in all shipping address fields');
      return false;
    }
    return true;
  };

  const handleProceedToPayment = () => {
    if (!isAuthenticated) {
      toast.info('Please sign in to proceed to checkout');
      openAuthModal('login');
      return;
    }
    if (!cart?.items || cart.items.length === 0) {
      toast.error('Your cart is empty');
      return;
    }
    if (!validateAddress()) return;

    setPaymentModalOpen(true);
  };

  const handleProcessPayment = async (paymentPayload) => {
    setIsProcessingPayment(true);
    try {
      // Step 1: Place the order
      const usingSaved = useSavedAddress && selectedAddressId;
      const orderReq = {
        couponCode: appliedCoupon || undefined,
        pointsToRedeem: appliedPoints > 0 ? appliedPoints : undefined,
        addressId: usingSaved ? selectedAddressId : undefined,
        address: usingSaved ? undefined : {
          firstName: addressForm.firstName.trim(),
          lastName: addressForm.lastName.trim(),
          email: addressForm.email.trim(),
          phoneCountryCode: addressForm.phoneCountryCode || '+91',
          phone: addressForm.phone.trim(),
          addressLine: addressForm.addressLine.trim(),
          city: addressForm.city.trim(),
          state: addressForm.state.trim(),
          pinCode: addressForm.pinCode.trim(),
          country: addressForm.country || 'India',
          isDefault: saveAddressToAccount,
        },
        saveAddress: usingSaved ? undefined : saveAddressToAccount,
      };

      const placedOrder = await api.checkout.placeOrder(orderReq);

      // Step 2: Pay for the order
      const paymentResult = await api.checkout.pay(placedOrder.id, paymentPayload);

      // Step 3: Refresh user state & cart
      await refreshUser();
      await refreshCart();

      toast.success('Payment completed successfully!');
      setPaymentModalOpen(false);

      // Step 4: Navigate to Slide 10 Confirmation Page with order details
      navigate('/confirmation', {
        state: {
          order: placedOrder,
          payment: paymentResult,
        },
      });
    } catch (err) {
      console.error('Order/Payment failed', err);
      toast.error(err.message || 'Payment failed. Please try again.');
    } finally {
      setIsProcessingPayment(false);
    }
  };

  if (!isAuthenticated && (!cart?.items || cart.items.length === 0)) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <EmptyState
          icon={ShoppingCart}
          title="Sign in to view your Shopping Cart"
          description="Log in to access your saved items, gift points, and proceed to checkout."
          actionText="Sign In / Register"
          onAction={() => openAuthModal('login')}
        />
      </div>
    );
  }

  if (cart?.items?.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <EmptyState
          icon={ShoppingCart}
          title="Your Shopping Cart is Empty"
          description="Explore our wide range of books and add your favorite reads to your cart."
          actionText="Explore Catalogue"
          actionTo="/"
        />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      
      {/* Breadcrumb (Slide 8) */}
      <nav className="flex items-center gap-1.5 text-xs text-slate-400 mb-6 flex-wrap">
        <Link to="/" className="hover:text-slate-200 transition-colors">Home</Link>
        <ChevronRight className="w-3.5 h-3.5" />
        <span className="text-slate-300">Catalogue</span>
        <ChevronRight className="w-3.5 h-3.5" />
        <span className="text-sky-400 font-medium">Checkout</span>
      </nav>

      {/* Main Grid: Left Cart & Address + Right Grand Total Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left Column: Cart items & Address Form (col-span-8) */}
        <div className="lg:col-span-8 space-y-8">
          
          {/* Section 1: Shopping Cart Items (Slide 8) */}
          <div className="bg-[#171a21] border border-[#262c3b] rounded-2xl p-5 sm:p-6 shadow-xl">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                <ShoppingCart className="w-4 h-4 text-sky-400" />
                <span>Shopping Cart</span>
                <span className="text-xs text-slate-400 font-normal">({cart.items.length} items)</span>
              </h2>
              <button
                onClick={clearCart}
                className="text-xs text-rose-400 hover:text-rose-300 font-medium"
              >
                Clear Cart
              </button>
            </div>

            <div className="space-y-3.5">
              {cart.items.map((item) => {
                const book = item.book;
                return (
                  <div
                    key={book.id}
                    className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-3.5 rounded-xl bg-[#121418] border border-[#262c3b]"
                  >
                    {/* Book Cover + Info */}
                    <div className="flex items-center gap-3.5 min-w-0 flex-1">
                      <BookCover book={book} className="w-16 h-24" />
                      <div className="min-w-0 flex-1">
                        <Link
                          to={`/books/${book.id}`}
                          className="text-sm font-semibold text-white hover:text-sky-300 transition-colors line-clamp-1"
                        >
                          {book.title}
                        </Link>
                        <p className="text-xs text-slate-400 mt-0.5">
                          by <span className="text-sky-400">{book.author?.name || 'Author'}</span>
                        </p>
                        <div className="flex items-center gap-2 mt-1.5">
                          {book.format && (
                            <span className="text-[10px] text-slate-300 bg-[#252b38] px-1.5 py-0.5 rounded font-medium">
                              {book.format.charAt(0).toUpperCase() + book.format.slice(1).toLowerCase()}
                            </span>
                          )}
                          <span className="text-xs font-bold text-white">
                            ₹{Number(book.price).toFixed(0)}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Quantity controls & Line Total */}
                    <div className="flex items-center justify-between sm:justify-end gap-5 w-full sm:w-auto pt-2 sm:pt-0 border-t sm:border-t-0 border-[#262c3b]">
                      <div className="flex items-center border border-[#2e3442] rounded-lg bg-[#171a21]">
                        <button
                          onClick={() => {
                            if (item.quantity > 1) {
                              updateQuantity(book.id, item.quantity - 1);
                            } else {
                              removeFromCart(book.id, book.title);
                            }
                          }}
                          className="p-1.5 text-slate-400 hover:text-white transition-colors"
                        >
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <span className="px-3 text-xs font-semibold text-white">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => updateQuantity(book.id, item.quantity + 1)}
                          className="p-1.5 text-slate-400 hover:text-white transition-colors"
                        >
                          <Plus className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <div className="text-right min-w-[70px]">
                        <div className="text-sm font-bold text-white">
                          ₹{Number(item.lineTotal || (book.price * item.quantity)).toFixed(0)}
                        </div>
                      </div>

                      <button
                        onClick={() => removeFromCart(book.id, book.title)}
                        className="text-slate-500 hover:text-rose-400 p-1.5 transition-colors"
                        title="Remove item"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                  </div>
                );
              })}
            </div>
          </div>

          {/* Section 2: Address Details (Slide 8) */}
          <div className="bg-[#171a21] border border-[#262c3b] rounded-2xl p-5 sm:p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                <MapPin className="w-4 h-4 text-sky-400" />
                <span>Delivery Address</span>
              </h2>

              {savedAddresses.length > 0 && (
                <label className="flex items-center gap-2 text-xs text-slate-300 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={useSavedAddress}
                    onChange={(e) => setUseSavedAddress(e.target.checked)}
                    className="rounded bg-[#121418] border-slate-700 text-blue-600 focus:ring-0"
                  />
                  <span>Use Saved Address</span>
                </label>
              )}
            </div>

            {/* Saved Address Dropdown Selector */}
            {useSavedAddress && savedAddresses.length > 0 && (
              <div className="p-3 rounded-xl bg-[#121418] border border-[#262c3b]">
                <label className="block text-[11px] text-slate-400 mb-1.5 font-medium">Select Saved Address</label>
                <select
                  value={selectedAddressId || ''}
                  onChange={(e) => handleSavedAddressChange(e.target.value)}
                  className="w-full bg-[#171a21] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-xs text-white focus:outline-none"
                >
                  {savedAddresses.map((addr) => (
                    <option key={addr.id} value={addr.id}>
                      {addr.firstName} {addr.lastName} — {addr.addressLine}, {addr.city}, {addr.pinCode}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Address Form Inputs (Slide 8 Wireframe fields) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 text-xs">
              <div>
                <label className="block text-slate-400 mb-1 font-medium">First Name</label>
                <input
                  type="text"
                  required
                  value={addressForm.firstName}
                  onChange={(e) => setAddressForm({ ...addressForm, firstName: e.target.value })}
                  placeholder="First Name"
                  className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-medium">Last Name</label>
                <input
                  type="text"
                  required
                  value={addressForm.lastName}
                  onChange={(e) => setAddressForm({ ...addressForm, lastName: e.target.value })}
                  placeholder="Last Name"
                  className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                />
              </div>

              <div className="sm:col-span-2">
                <label className="block text-slate-400 mb-1 font-medium">Address (Street / House / Apartment)</label>
                <input
                  type="text"
                  required
                  value={addressForm.addressLine}
                  onChange={(e) => setAddressForm({ ...addressForm, addressLine: e.target.value })}
                  placeholder="Address Line"
                  className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-medium">E-mail</label>
                <input
                  type="email"
                  required
                  value={addressForm.email}
                  onChange={(e) => setAddressForm({ ...addressForm, email: e.target.value })}
                  placeholder="e-mail"
                  className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-slate-400 mb-1 font-medium">City</label>
                  <input
                    type="text"
                    required
                    value={addressForm.city}
                    onChange={(e) => setAddressForm({ ...addressForm, city: e.target.value })}
                    placeholder="City"
                    className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-slate-400 mb-1 font-medium">Pin / Postal</label>
                  <input
                    type="text"
                    required
                    value={addressForm.pinCode}
                    onChange={(e) => setAddressForm({ ...addressForm, pinCode: e.target.value })}
                    placeholder="560001"
                    className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-medium">Phone Number</label>
                <div className="flex gap-1.5">
                  <span className="bg-[#121418] border border-[#2e3442] text-slate-300 px-2.5 py-2 rounded-xl text-xs flex items-center">
                    +91
                  </span>
                  <input
                    type="tel"
                    required
                    value={addressForm.phone}
                    onChange={(e) => setAddressForm({ ...addressForm, phone: e.target.value })}
                    placeholder="9876543210"
                    className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-slate-400 mb-1 font-medium">State</label>
                  <input
                    type="text"
                    required
                    value={addressForm.state}
                    onChange={(e) => setAddressForm({ ...addressForm, state: e.target.value })}
                    placeholder="State"
                    className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-slate-400 mb-1 font-medium">Country</label>
                  <input
                    type="text"
                    required
                    value={addressForm.country}
                    onChange={(e) => setAddressForm({ ...addressForm, country: e.target.value })}
                    placeholder="India"
                    className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                  />
                </div>
              </div>

            </div>

            <div className="pt-2">
              <label className="flex items-center gap-2 text-xs text-slate-300 cursor-pointer">
                <input
                  type="checkbox"
                  checked={saveAddressToAccount}
                  onChange={(e) => setSaveAddressToAccount(e.target.checked)}
                  className="rounded bg-[#121418] border-slate-700 text-blue-600 focus:ring-0"
                />
                <span>Save this address for faster future checkouts</span>
              </label>
            </div>
          </div>

        </div>

        {/* Right Column: Grand Total Card (Slide 8) */}
        <div className="lg:col-span-4">
          <div className="sticky top-20 bg-[#171a21] border border-[#262c3b] rounded-2xl overflow-hidden shadow-xl">
            
            {/* Illustrated Banner Header matching Slide 8 */}
            <div className="h-28 bg-gradient-to-tr from-blue-900 via-indigo-900 to-sky-800 p-4 relative flex items-center justify-between overflow-hidden">
              <div className="relative z-10">
                <span className="text-[11px] font-semibold text-sky-200 uppercase tracking-wider">Order Summary</span>
                <h3 className="text-xl font-bold text-white tracking-tight">Grand Total</h3>
              </div>
              <div className="text-4xl select-none opacity-80 z-10">📚</div>
              {/* Pattern circles */}
              <div className="absolute -right-6 -bottom-6 w-32 h-32 rounded-full bg-blue-500/10 blur-xl" />
              <div className="absolute -left-6 -top-6 w-32 h-32 rounded-full bg-sky-400/10 blur-xl" />
            </div>

            <div className="p-5 space-y-4 text-xs">
              
              {/* Line items calculation */}
              <div className="space-y-2.5 pb-4 border-b border-[#262c3b]">
                <div className="flex justify-between text-slate-300">
                  <span>Price ({cart.itemCount} items)</span>
                  <span className="font-semibold text-white">
                    ₹{Number(priceBreakdown?.subtotal || cart.subtotal || 0).toFixed(2)}
                  </span>
                </div>

                <div className="flex justify-between text-slate-300">
                  <span>Tax (12% GST)</span>
                  <span className="font-semibold text-white">
                    ₹{Number(priceBreakdown?.tax || (cart.subtotal * 0.12)).toFixed(2)}
                  </span>
                </div>

                <div className="flex justify-between text-slate-300">
                  <span>Delivery Charges</span>
                  <span className="font-semibold text-emerald-400">
                    {priceBreakdown?.deliveryCharge === 0 || cart.subtotal >= 500 ? 'FREE' : '₹40.00'}
                  </span>
                </div>

                {priceBreakdown?.couponDiscount > 0 && (
                  <div className="flex justify-between text-emerald-400">
                    <span>Coupon Discount ({appliedCoupon})</span>
                    <span className="font-semibold">-₹{Number(priceBreakdown.couponDiscount).toFixed(2)}</span>
                  </div>
                )}

                {priceBreakdown?.pointsDiscount > 0 && (
                  <div className="flex justify-between text-amber-300">
                    <span>Gift Points Redeemed ({appliedPoints} pts)</span>
                    <span className="font-semibold">-₹{Number(priceBreakdown.pointsDiscount).toFixed(2)}</span>
                  </div>
                )}
              </div>

              {/* Coupon Code Input (Slide 8) */}
              <form onSubmit={handleApplyCoupon} className="space-y-1">
                <label className="block text-[11px] text-slate-400 font-medium">Apply Coupon</label>
                <div className="flex gap-1.5">
                  <div className="relative flex-1">
                    <Tag className="w-3.5 h-3.5 text-slate-500 absolute left-2.5 top-2.5" />
                    <input
                      type="text"
                      value={couponCode}
                      onChange={(e) => setCouponCode(e.target.value)}
                      placeholder="e.g. FLAT100"
                      className="w-full bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl pl-8 pr-2 py-1.5 text-white uppercase text-xs placeholder-slate-500 focus:outline-none"
                    />
                  </div>
                  <button
                    type="submit"
                    className="px-3 py-1.5 rounded-xl bg-[#222836] hover:bg-blue-600 hover:text-white border border-[#313a4d] text-slate-200 font-medium text-xs transition-colors"
                  >
                    Apply
                  </button>
                </div>
              </form>

              {/* Redeem Gift Points Input (Slide 8) */}
              {user && (
                <form onSubmit={handleApplyPoints} className="space-y-1 pt-1">
                  <div className="flex justify-between text-[11px]">
                    <span className="text-slate-400 font-medium flex items-center gap-1">
                      <Gift className="w-3 h-3 text-amber-400" /> Redeem Gift Points
                    </span>
                    <span className="text-amber-300 font-semibold">{user.giftPoints} pts available</span>
                  </div>
                  <div className="flex gap-1.5">
                    <input
                      type="number"
                      min={0}
                      max={user.giftPoints}
                      value={pointsToRedeem}
                      onChange={(e) => setPointsToRedeem(e.target.value)}
                      placeholder="Points to redeem"
                      className="flex-1 bg-[#121418] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-1.5 text-white text-xs placeholder-slate-500 focus:outline-none"
                    />
                    <button
                      type="submit"
                      className="px-3 py-1.5 rounded-xl bg-[#222836] hover:bg-amber-600 hover:text-white border border-[#313a4d] text-slate-200 font-medium text-xs transition-colors"
                    >
                      Redeem
                    </button>
                  </div>
                </form>
              )}

              {/* Total Amount & Pay Now Button (Slide 8) */}
              <div className="pt-3 border-t border-[#262c3b] space-y-4">
                <div className="flex items-baseline justify-between">
                  <span className="text-sm font-semibold text-slate-200">Total Amount</span>
                  <span className="text-2xl font-extrabold text-white tracking-tight">
                    ₹{Number(priceBreakdown?.total || (cart.subtotal * 1.12)).toFixed(0)}
                  </span>
                </div>

                <button
                  type="button"
                  onClick={handleProceedToPayment}
                  disabled={quoteLoading}
                  className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white font-bold text-sm shadow-lg shadow-blue-500/25 transition-all disabled:opacity-50"
                >
                  <CreditCard className="w-4 h-4" />
                  <span>Pay Now</span>
                </button>

                <div className="flex items-center justify-center gap-1.5 text-[10px] text-slate-400 pt-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Safe & Secure 256-Bit SSL Checkout</span>
                </div>
              </div>

            </div>

          </div>
        </div>

      </div>

      {/* Slide 9 Payment Modal */}
      <PaymentModal
        isOpen={paymentModalOpen}
        onClose={() => setPaymentModalOpen(false)}
        totalAmount={priceBreakdown?.total || (cart.subtotal * 1.12)}
        onProcessPayment={handleProcessPayment}
        isProcessing={isProcessingPayment}
      />

    </div>
  );
}
