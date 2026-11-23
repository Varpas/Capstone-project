import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { 
  BookOpen, 
  ShoppingCart, 
  User, 
  LogOut, 
  Gift, 
  Menu, 
  X, 
  BookMarked, 
  Package, 
  Users, 
  Sparkles 
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export function Navbar() {
  const { user, isAuthenticated, logout, openAuthModal } = useAuth();
  const { itemCount } = useCart();
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();

  const navLinkClass = ({ isActive }) =>
    `px-3 py-1.5 rounded-lg text-xs md:text-sm font-medium transition-colors ${
      isActive
        ? 'text-white bg-[#252a36]'
        : 'text-slate-300 hover:text-white hover:bg-[#1e222b]'
    }`;

  return (
    <header className="sticky top-0 z-40 bg-[#121418]/95 backdrop-blur-md border-b border-[#262c3b]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Left: Brand Logo */}
        <Link to="/" className="flex items-center gap-2.5 group">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-sky-400 flex items-center justify-center text-white shadow-md shadow-blue-500/20 group-hover:scale-105 transition-transform">
            <BookOpen className="w-5 h-5" />
          </div>
          <div className="flex flex-col">
            <span className="font-bold text-lg tracking-tight text-white flex items-center gap-1.5">
              Book Worm
            </span>
            <span className="text-[10px] text-slate-400 -mt-1 hidden sm:inline">E-Bookstore Platform</span>
          </div>
        </Link>

        {/* Center / Navigation Links (Desktop) */}
        <nav className="hidden md:flex items-center space-x-1 lg:space-x-3">
          <NavLink to="/orders" className={navLinkClass}>
            My Orders
          </NavLink>
          <NavLink to="/wishlist" className={navLinkClass}>
            My Wishlist
          </NavLink>
          <NavLink to="/writers" className={navLinkClass}>
            My Writers
          </NavLink>
        </nav>

        {/* Right: Cart & User actions */}
        <div className="flex items-center gap-3">
          
          {/* Cart Icon with badge */}
          <Link
            to="/checkout"
            className="relative p-2 rounded-xl bg-[#1e222b] hover:bg-[#282e3b] text-slate-200 hover:text-white border border-[#2e3442] transition-colors"
            title="Shopping Cart & Checkout"
          >
            <ShoppingCart className="w-5 h-5" />
            {itemCount > 0 && (
              <span className="absolute -top-1.5 -right-1.5 min-w-[20px] h-5 px-1 bg-rose-500 text-white text-[11px] font-bold rounded-full flex items-center justify-center shadow-md animate-pulse">
                {itemCount}
              </span>
            )}
          </Link>

          {/* User Menu / Auth Button */}
          {isAuthenticated ? (
            <div className="relative">
              <button
                onClick={() => setUserDropdownOpen(!userDropdownOpen)}
                className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-[#1e222b] hover:bg-[#282e3b] border border-[#2e3442] text-slate-200 transition-colors"
              >
                <div className="w-6 h-6 rounded-full bg-blue-600 text-white flex items-center justify-center text-xs font-bold uppercase">
                  {user.firstName ? user.firstName[0] : 'U'}
                </div>
                <span className="text-xs font-medium max-w-[90px] truncate hidden sm:inline">
                  {user.firstName}
                </span>
                {user.giftPoints > 0 && (
                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300 font-semibold hidden lg:inline">
                    ★ {user.giftPoints} pts
                  </span>
                )}
              </button>

              {/* Dropdown menu */}
              {userDropdownOpen && (
                <>
                  <div
                    className="fixed inset-0 z-40"
                    onClick={() => setUserDropdownOpen(false)}
                  />
                  <div className="absolute right-0 mt-2 w-56 rounded-xl bg-[#171a21] border border-[#2e3442] shadow-2xl py-2 z-50 text-xs">
                    <div className="px-4 py-2 border-b border-[#262c3b]">
                      <p className="font-semibold text-white truncate">
                        {user.firstName} {user.lastName}
                      </p>
                      <p className="text-[11px] text-slate-400 truncate">{user.email}</p>
                      <div className="mt-2 flex items-center justify-between text-amber-300 bg-amber-500/10 px-2.5 py-1 rounded-lg">
                        <span className="flex items-center gap-1 font-medium">
                          <Gift className="w-3.5 h-3.5" /> Gift Points
                        </span>
                        <span className="font-bold">{user.giftPoints} pts</span>
                      </div>
                    </div>

                    <Link
                      to="/orders"
                      onClick={() => setUserDropdownOpen(false)}
                      className="flex items-center gap-2 px-4 py-2.5 text-slate-300 hover:text-white hover:bg-[#222733]"
                    >
                      <Package className="w-4 h-4 text-sky-400" /> My Orders
                    </Link>
                    <Link
                      to="/wishlist"
                      onClick={() => setUserDropdownOpen(false)}
                      className="flex items-center gap-2 px-4 py-2.5 text-slate-300 hover:text-white hover:bg-[#222733]"
                    >
                      <BookMarked className="w-4 h-4 text-rose-400" /> My Wishlist
                    </Link>
                    <Link
                      to="/writers"
                      onClick={() => setUserDropdownOpen(false)}
                      className="flex items-center gap-2 px-4 py-2.5 text-slate-300 hover:text-white hover:bg-[#222733]"
                    >
                      <Users className="w-4 h-4 text-emerald-400" /> My Writers
                    </Link>

                    <div className="border-t border-[#262c3b] mt-1 pt-1">
                      <button
                        onClick={() => {
                          setUserDropdownOpen(false);
                          logout();
                        }}
                        className="w-full flex items-center gap-2 px-4 py-2 text-rose-400 hover:bg-rose-500/10 text-left"
                      >
                        <LogOut className="w-4 h-4" /> Sign Out
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          ) : (
            <button
              onClick={() => openAuthModal('login')}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white text-xs font-semibold shadow-md transition-colors"
            >
              <User className="w-4 h-4" />
              <span>Sign In</span>
            </button>
          )}

          {/* Mobile hamburger menu toggle */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 rounded-xl bg-[#1e222b] text-slate-300 hover:text-white border border-[#2e3442]"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Drawer Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-[#171a21] border-b border-[#262c3b] px-4 pt-2 pb-4 space-y-1 text-sm">
          <NavLink
            to="/"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-lg text-slate-300 hover:text-white hover:bg-[#222733]"
          >
            Home / Catalogue
          </NavLink>
          <NavLink
            to="/orders"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-lg text-slate-300 hover:text-white hover:bg-[#222733]"
          >
            My Orders
          </NavLink>
          <NavLink
            to="/wishlist"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-lg text-slate-300 hover:text-white hover:bg-[#222733]"
          >
            My Wishlist
          </NavLink>
          <NavLink
            to="/writers"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-lg text-slate-300 hover:text-white hover:bg-[#222733]"
          >
            My Writers
          </NavLink>
          <NavLink
            to="/checkout"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-lg text-slate-300 hover:text-white hover:bg-[#222733]"
          >
            Shopping Cart ({itemCount} items)
          </NavLink>
        </div>
      )}
    </header>
  );
}
