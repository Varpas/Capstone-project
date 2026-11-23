/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        dark: {
          950: '#0d0f12',
          900: '#121418',
          850: '#171a21',
          800: '#1e222b',
          750: '#252a36',
          700: '#2e3442',
          600: '#434b5e',
          500: '#64748b',
          400: '#94a3b8',
          300: '#cbd5e1',
          200: '#e2e8f0',
          100: '#f1f5f9',
        },
        brand: {
          blue: '#2563eb',
          'blue-hover': '#1d4ed8',
          yellow: '#f59e0b',
          accent: '#38bdf8',
        }
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
