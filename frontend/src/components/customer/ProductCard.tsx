'use client';

import React from 'react';
import { Product } from '@/data/products';
import { useApp } from '@/context/AppContext';
import { Plus, Minus, Clock, Star } from 'lucide-react';

interface ProductCardProps {
  product: Product;
}

export const ProductCard: React.FC<ProductCardProps> = ({ product }) => {
  const { cart, addToCart, removeFromCart } = useApp();
  const quantity = cart[product.id] || 0;

  return (
    <div className="bg-white rounded-card border border-brand-border p-3 flex flex-col justify-between shadow-card hover:shadow-float hover:border-brand-primary/30 transition-all group relative">
      {/* Top Badges */}
      <div className="flex items-center justify-between mb-2">
        <span className="flex items-center gap-1 text-[10px] font-bold text-brand-primary bg-brand-primary/10 px-2 py-0.5 rounded-full">
          <Clock size={10} />
          {product.deliveryTime}
        </span>
        {product.discount > 0 && (
          <span className="text-[10px] font-black bg-brand-accent text-brand-text px-2 py-0.5 rounded-full shadow-sm">
            {product.discount}% OFF
          </span>
        )}
      </div>

      {/* Product Image */}
      <div className="relative w-full h-36 mb-3 overflow-hidden rounded-input bg-brand-bg flex items-center justify-center">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={product.image}
          alt={product.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
        />
        {product.badge && (
          <span className="absolute bottom-1.5 left-1.5 bg-brand-text/90 text-brand-accent text-[9px] font-extrabold px-1.5 py-0.5 rounded">
            {product.badge}
          </span>
        )}
      </div>

      {/* Product Details */}
      <div className="flex-1 flex flex-col">
        <span className="text-[11px] font-semibold text-brand-muted mb-0.5">
          {product.unit}
        </span>
        <h4 className="text-xs font-bold text-brand-text line-clamp-2 leading-tight mb-2 group-hover:text-brand-primary transition-colors">
          {product.name}
        </h4>

        {/* Rating */}
        <div className="flex items-center gap-1 mb-3 text-[10px] text-brand-muted">
          <span className="flex items-center text-amber-500 font-bold bg-amber-50 px-1.5 py-0.2 rounded">
            <Star size={10} fill="currentColor" className="mr-0.5" />
            {product.rating}
          </span>
          <span>({product.reviewsCount})</span>
        </div>
      </div>

      {/* Price & Add to Cart Action */}
      <div className="flex items-center justify-between pt-2 border-t border-brand-border/60 mt-auto">
        <div className="flex flex-col">
          <span className="text-sm font-black text-brand-text leading-none">
            ₹{product.price}
          </span>
          {product.mrp > product.price && (
            <span className="text-[10px] text-brand-muted line-through mt-0.5">
              ₹{product.mrp}
            </span>
          )}
        </div>

        {/* Add / Quantity Control */}
        {quantity === 0 ? (
          <button
            onClick={() => addToCart(product.id)}
            className="px-4 py-1.5 bg-white hover:bg-brand-primary text-brand-primary hover:text-white border-2 border-brand-primary rounded-button font-black text-xs transition-all shadow-sm active:scale-95 flex items-center gap-1"
          >
            <span>ADD</span>
            <Plus size={12} strokeWidth={3} />
          </button>
        ) : (
          <div className="flex items-center bg-brand-primary text-white rounded-button overflow-hidden shadow-sm">
            <button
              onClick={() => removeFromCart(product.id)}
              className="px-2.5 py-1.5 hover:bg-brand-primaryHover text-white font-bold transition-colors"
              aria-label="Decrease quantity"
            >
              <Minus size={12} strokeWidth={3} />
            </button>
            <span className="px-2 text-xs font-black min-w-[20px] text-center">
              {quantity}
            </span>
            <button
              onClick={() => addToCart(product.id)}
              className="px-2.5 py-1.5 hover:bg-brand-primaryHover text-white font-bold transition-colors"
              aria-label="Increase quantity"
            >
              <Plus size={12} strokeWidth={3} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProductCard;
