/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          primary: '#176B45',      // Leaf Green
          primaryHover: '#125638', // Darker Leaf Green
          accent: '#D8F36A',       // Zesty Lime
          bg: '#F7F8F2',           // Fresh Cream
          text: '#173327',         // Forest Ink
          surface: '#FFFFFF',      // Pure White
          border: '#E3E8DE',       // Subtle Border
          muted: '#68776D',        // Muted Forest Gray
          danger: '#B42318',       // Error Red
          warning: '#8A5A00',      // Caution Amber
          info: '#2459A9',         // Info Blue
        },
      },
      borderRadius: {
        'card': '16px',
        'button': '12px',
        'input': '10px',
      },
      boxShadow: {
        'card': '0 4px 16px rgba(23, 51, 39, 0.06)',
        'float': '0 10px 30px rgba(23, 51, 39, 0.12)',
        'modal': '0 20px 40px rgba(23, 51, 39, 0.18)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
