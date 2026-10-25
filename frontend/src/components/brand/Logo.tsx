import React from 'react';

interface LogoProps {
  variant?: 'primary' | 'white' | 'monochrome' | 'icon';
  size?: 'sm' | 'md' | 'lg' | 'xl';
  showSubtitle?: boolean;
  className?: string;
}

export const NexoraIcon: React.FC<{ size?: number; className?: string; inverted?: boolean }> = ({
  size = 40,
  className = '',
  inverted = false,
}) => {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`shrink-0 ${className}`}
    >
      <defs>
        <linearGradient id={`gradLeaf-${size}`} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#D8F36A" />
          <stop offset="100%" stopColor="#A8E832" />
        </linearGradient>
      </defs>

      {/* Left Pillar */}
      <path
        d="M18 82V28C18 22.477 22.477 18 28 18H32C35.314 18 38 20.686 38 24V82H18Z"
        fill={inverted ? '#FFFFFF' : '#176B45'}
      />

      {/* Dynamic Forward Arrow / Fresh Leaf Diagonal */}
      <path
        d="M30 20L72 68C76 72.5 82 69 82 63V22C82 19.791 80.209 18 78 18H74C71.791 18 70 19.791 70 22V52L36 18C34 16 30 17 30 20Z"
        fill={`url(#gradLeaf-${size})`}
      />

      {/* Right Pillar */}
      <path
        d="M62 82V38C62 34.686 64.686 32 68 32H72C77.523 32 82 36.477 82 42V82H62Z"
        fill={inverted ? '#FFFFFF' : '#176B45'}
      />

      {/* Motion Spark Accent */}
      <circle cx="82" cy="18" r="6" fill="#D8F36A" />
    </svg>
  );
};

export const NexoraLogo: React.FC<LogoProps> = ({
  variant = 'primary',
  size = 'md',
  showSubtitle = true,
  className = '',
}) => {
  const isWhite = variant === 'white';

  const iconSizes = {
    sm: 28,
    md: 40,
    lg: 52,
    xl: 64,
  };

  const textSizes = {
    sm: 'text-xl',
    md: 'text-2xl',
    lg: 'text-3xl',
    xl: 'text-4xl',
  };

  if (variant === 'icon') {
    return <NexoraIcon size={iconSizes[size]} className={className} inverted={isWhite} />;
  }

  return (
    <div className={`flex items-center gap-2.5 select-none ${className}`}>
      <NexoraIcon size={iconSizes[size]} inverted={isWhite} />
      <div className="flex flex-col justify-center">
        <div className="flex items-center gap-1.5 leading-none">
          <span
            className={`font-black tracking-tight ${textSizes[size]} ${
              isWhite ? 'text-white' : 'text-brand-text'
            }`}
            style={{ fontFamily: "'Inter', sans-serif" }}
          >
            NEXORA
          </span>
          <span className="w-1.5 h-1.5 rounded-full bg-brand-accent animate-pulse" />
        </div>
        {showSubtitle && (
          <div className="flex items-center gap-1 mt-0.5">
            <span
              className={`text-[9px] font-bold uppercase tracking-widest ${
                isWhite ? 'text-brand-accent' : 'text-brand-muted'
              }`}
            >
              10-Min Delivery
            </span>
          </div>
        )}
      </div>
    </div>
  );
};

export default NexoraLogo;
