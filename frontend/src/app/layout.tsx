import type { Metadata, Viewport } from 'next';
import './globals.css';
import { AppProvider } from '@/context/AppContext';

export const metadata: Metadata = {
  title: 'NEXORA — 10-Minute Quick Commerce | Fresh Groceries & Essentials',
  description: 'Experience lightning fast 10-minute grocery delivery from your neighborhood dark store. 100% farm fresh produce, daily essentials, snacks, and milk.',
  icons: {
    icon: '/favicon.svg',
    shortcut: '/favicon.svg',
    apple: '/assets/brand/app-icon.svg',
  },
  openGraph: {
    title: 'NEXORA — 10-Minute Quick Commerce',
    description: 'Groceries and daily essentials delivered in 10 minutes flat.',
    type: 'website',
  },
};

export const viewport: Viewport = {
  themeColor: '#176B45',
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="antialiased min-h-screen bg-brand-bg text-brand-text">
        <AppProvider>
          {children}
        </AppProvider>
      </body>
    </html>
  );
}
