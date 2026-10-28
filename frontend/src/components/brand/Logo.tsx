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
}) => {
  return (
    <div
      className={`shrink-0 relative overflow-hidden rounded-xl shadow-xs inline-flex items-center justify-center ${className}`}
      style={{ width: size, height: size }}
    >
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src="/assets/brand/app-icon.png"
        alt="Nexora"
        className="w-full h-full object-cover rounded-xl"
        style={{ width: `${size}px`, height: `${size}px` }}
      />
    </div>
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
