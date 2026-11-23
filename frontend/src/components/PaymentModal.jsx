import React, { useState } from 'react';
import { X, CreditCard, Smartphone, Wallet, ShieldCheck, Check } from 'lucide-react';

export function PaymentModal({
  isOpen,
  onClose,
  totalAmount,
  onProcessPayment,
  isProcessing,
}) {
  const [method, setMethod] = useState('CREDIT'); // CREDIT, DEBIT, UPI, WALLET
  
  // Card form state
  const [cardNumber, setCardNumber] = useState('4532015112830366');
  const [nameOnCard, setNameOnCard] = useState('Aarav Sharma');
  const [cvv, setCvv] = useState('123');
  const [expiry, setExpiry] = useState('12/2028');

  // UPI & Wallet states
  const [upiId, setUpiId] = useState('aarav@okhdfcbank');
  const [walletProvider, setWalletProvider] = useState('Paytm');

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();

    let paymentPayload = {
      method: method === 'CREDIT' ? 'CREDIT_CARD' : method === 'DEBIT' ? 'DEBIT_CARD' : method === 'UPI' ? 'UPI' : 'WALLET',
    };

    if (method === 'CREDIT' || method === 'DEBIT') {
      paymentPayload = {
        ...paymentPayload,
        cardNumber: cardNumber.replace(/\s+/g, ''),
        nameOnCard,
        cvv,
        expiry,
      };
    } else if (method === 'UPI') {
      paymentPayload = {
        ...paymentPayload,
        upiId,
      };
    } else {
      paymentPayload = {
        ...paymentPayload,
        walletId: walletProvider,
      };
    }

    onProcessPayment(paymentPayload);
  };

  // Format card number with spaces for display
  const formatCardInput = (val) => {
    const raw = val.replace(/\D/g, '').substring(0, 16);
    return raw.replace(/(\d{4})(?=\d)/g, '$1 ');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      {/* Slide 9 Styled Payment Card */}
      <div className="relative w-full max-w-xl rounded-2xl bg-[#171a21] border border-[#2e3442] shadow-2xl overflow-hidden">
        
        {/* Modal Top Header */}
        <div className="flex items-center justify-between px-6 py-4 bg-[#121418] border-b border-[#262c3b]">
          <div className="flex items-center gap-2">
            <CreditCard className="w-5 h-5 text-sky-400" />
            <h2 className="text-base font-bold text-white tracking-tight">Complete Payment</h2>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm font-semibold text-emerald-400 bg-emerald-950/60 px-3 py-1 rounded-lg border border-emerald-800/50">
              Payable: ₹{Number(totalAmount).toFixed(0)}
            </span>
            <button
              onClick={onClose}
              disabled={isProcessing}
              className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-[#252a36] transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Modal Content: Split Tabs & Inputs (Slide 9 Wireframe) */}
        <div className="grid grid-cols-1 sm:grid-cols-12 min-h-[300px]">
          
          {/* Left Method Tabs (col-span-4) */}
          <div className="sm:col-span-4 bg-[#121418]/60 border-r border-[#262c3b] p-3 space-y-1.5">
            <button
              type="button"
              onClick={() => setMethod('CREDIT')}
              className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-semibold text-left transition-colors ${
                method === 'CREDIT'
                  ? 'bg-blue-600 text-white shadow-md'
                  : 'text-slate-300 hover:bg-[#1e222b] hover:text-white'
              }`}
            >
              <CreditCard className="w-4 h-4 shrink-0" />
              <span>Credit Card</span>
            </button>

            <button
              type="button"
              onClick={() => setMethod('DEBIT')}
              className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-semibold text-left transition-colors ${
                method === 'DEBIT'
                  ? 'bg-blue-600 text-white shadow-md'
                  : 'text-slate-300 hover:bg-[#1e222b] hover:text-white'
              }`}
            >
              <CreditCard className="w-4 h-4 shrink-0" />
              <span>Debit Card</span>
            </button>

            <button
              type="button"
              onClick={() => setMethod('UPI')}
              className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-semibold text-left transition-colors ${
                method === 'UPI'
                  ? 'bg-blue-600 text-white shadow-md'
                  : 'text-slate-300 hover:bg-[#1e222b] hover:text-white'
              }`}
            >
              <Smartphone className="w-4 h-4 shrink-0" />
              <span>UPI</span>
            </button>

            <button
              type="button"
              onClick={() => setMethod('WALLET')}
              className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-semibold text-left transition-colors ${
                method === 'WALLET'
                  ? 'bg-blue-600 text-white shadow-md'
                  : 'text-slate-300 hover:bg-[#1e222b] hover:text-white'
              }`}
            >
              <Wallet className="w-4 h-4 shrink-0" />
              <span>Wallet</span>
            </button>

            <div className="pt-4 px-2 flex items-center gap-2 text-[10px] text-slate-500">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
              <span>Simulated 256-bit Secure Gateway</span>
            </div>
          </div>

          {/* Right Inputs (col-span-8) */}
          <div className="sm:col-span-8 p-6 flex flex-col justify-between">
            <form onSubmit={handleSubmit} className="space-y-4 text-xs">
              
              {(method === 'CREDIT' || method === 'DEBIT') && (
                <>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-slate-400 mb-1 font-medium">Card Number</label>
                      <input
                        type="text"
                        required
                        value={cardNumber}
                        onChange={(e) => setCardNumber(e.target.value)}
                        placeholder="XXXX-XXXX-XXXX-XXXX"
                        maxLength={19}
                        className="w-full bg-[#1e222b] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white font-mono placeholder-slate-500 focus:outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-slate-400 mb-1 font-medium">Name on Card</label>
                      <input
                        type="text"
                        required
                        value={nameOnCard}
                        onChange={(e) => setNameOnCard(e.target.value)}
                        placeholder="Name"
                        className="w-full bg-[#1e222b] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-slate-400 mb-1 font-medium">CVV</label>
                      <input
                        type="password"
                        required
                        maxLength={4}
                        value={cvv}
                        onChange={(e) => setCvv(e.target.value)}
                        placeholder="XXX"
                        className="w-full bg-[#1e222b] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white font-mono placeholder-slate-500 focus:outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-slate-400 mb-1 font-medium">Date of Expiry</label>
                      <input
                        type="text"
                        required
                        value={expiry}
                        onChange={(e) => setExpiry(e.target.value)}
                        placeholder="MM/YYYY"
                        maxLength={7}
                        className="w-full bg-[#1e222b] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white font-mono placeholder-slate-500 focus:outline-none"
                      />
                    </div>
                  </div>
                </>
              )}

              {method === 'UPI' && (
                <div className="space-y-3">
                  <div>
                    <label className="block text-slate-400 mb-1 font-medium">UPI ID / VPA</label>
                    <input
                      type="text"
                      required
                      value={upiId}
                      onChange={(e) => setUpiId(e.target.value)}
                      placeholder="username@okhdfcbank"
                      className="w-full bg-[#1e222b] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white placeholder-slate-500 focus:outline-none"
                    />
                  </div>
                  <p className="text-[11px] text-slate-400 leading-relaxed">
                    A payment request will be sent to your UPI app (Google Pay, PhonePe, Paytm).
                  </p>
                </div>
              )}

              {method === 'WALLET' && (
                <div className="space-y-3">
                  <div>
                    <label className="block text-slate-400 mb-1 font-medium">Select Wallet</label>
                    <select
                      value={walletProvider}
                      onChange={(e) => setWalletProvider(e.target.value)}
                      className="w-full bg-[#1e222b] border border-[#2e3442] focus:border-blue-500 rounded-xl px-3 py-2 text-white focus:outline-none"
                    >
                      <option value="Paytm">Paytm Wallet</option>
                      <option value="PhonePe">PhonePe Wallet</option>
                      <option value="AmazonPay">Amazon Pay</option>
                      <option value="Mobikwik">MobiKwik</option>
                    </select>
                  </div>
                </div>
              )}

              {/* Pay Now Button (Slide 9) */}
              <div className="pt-4 flex justify-end">
                <button
                  type="submit"
                  disabled={isProcessing}
                  className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white font-semibold text-xs transition-colors shadow-lg shadow-blue-500/25 disabled:opacity-50"
                >
                  <CreditCard className="w-4 h-4" />
                  <span>{isProcessing ? 'Processing Transaction...' : 'Pay Now'}</span>
                </button>
              </div>

            </form>
          </div>

        </div>

      </div>
    </div>
  );
}
