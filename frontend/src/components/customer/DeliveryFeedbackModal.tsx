'use client';

import React, { useState } from 'react';
import { Star, CheckCircle2, X, Heart, ShieldCheck, Sparkles } from 'lucide-react';

interface DeliveryFeedbackModalProps {
  isOpen: boolean;
  onClose: () => void;
  orderId: string;
  riderName?: string;
  onSubmitRating?: (rating: number, tags: string[], comment: string) => void;
}

export const DeliveryFeedbackModal: React.FC<DeliveryFeedbackModalProps> = ({
  isOpen,
  onClose,
  orderId,
  riderName = 'Ramesh Patel',
  onSubmitRating,
}) => {
  const [rating, setRating] = useState<number>(5);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [selectedTags, setSelectedTags] = useState<string[]>(['Lightning Fast ⚡', 'Polite Rider 😊']);
  const [comment, setComment] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);

  if (!isOpen) return null;

  const quickTags = [
    'Lightning Fast ⚡',
    'Polite Rider 😊',
    'Tamper Seal Intact 📦',
    'Items Fresh & Cold 🥦',
    'Careful Handling 🛡️',
    'Correct Address 📍',
  ];

  const toggleTag = (tag: string) => {
    setSelectedTags((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]
    );
  };

  const handleSubmit = () => {
    setIsSubmitted(true);
    if (onSubmitRating) {
      onSubmitRating(rating, selectedTags, comment);
    }
    setTimeout(() => {
      onClose();
      setIsSubmitted(false);
    }, 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-brand-text/70 backdrop-blur-xs" onClick={onClose} />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-md bg-white rounded-card shadow-2xl z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-200">
        <div className="h-2 bg-gradient-to-r from-brand-primary via-brand-accent to-emerald-500" />

        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-full text-brand-muted hover:text-brand-text hover:bg-brand-bg transition-colors"
        >
          <X size={18} />
        </button>

        <div className="p-6 text-center">
          {!isSubmitted ? (
            <>
              <div className="w-14 h-14 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center mx-auto mb-3 shadow-xs">
                <CheckCircle2 size={32} />
              </div>

              <h3 className="text-lg font-black text-brand-text">
                Order #{orderId} Delivered!
              </h3>
              <p className="text-xs text-brand-muted mt-1 max-w-xs mx-auto">
                How was your 10-minute delivery experience with <strong className="text-brand-text">{riderName}</strong>?
              </p>

              {/* Star Rating */}
              <div className="flex items-center justify-center gap-2 my-5">
                {[1, 2, 3, 4, 5].map((star) => {
                  const active = (hoverRating || rating) >= star;
                  return (
                    <button
                      key={star}
                      type="button"
                      onMouseEnter={() => setHoverRating(star)}
                      onMouseLeave={() => setHoverRating(0)}
                      onClick={() => setRating(star)}
                      className="p-1 transition-transform hover:scale-125 focus:outline-none"
                    >
                      <Star
                        size={32}
                        className={active ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}
                      />
                    </button>
                  );
                })}
              </div>

              <span className="inline-block text-xs font-black text-brand-primary bg-brand-primary/10 px-3 py-1 rounded-full mb-4">
                {rating === 5 ? '★ Excellent (5.0)' : rating === 4 ? '★ Good (4.0)' : rating === 3 ? '★ Average (3.0)' : '★ Needs Improvement'}
              </span>

              {/* Tag Pills */}
              <div className="flex flex-wrap justify-center gap-1.5 mb-4">
                {quickTags.map((tag) => {
                  const isSelected = selectedTags.includes(tag);
                  return (
                    <button
                      key={tag}
                      type="button"
                      onClick={() => toggleTag(tag)}
                      className={`text-xs px-2.5 py-1 rounded-full font-bold transition-all ${
                        isSelected
                          ? 'bg-brand-primary text-white shadow-xs'
                          : 'bg-brand-bg text-brand-text hover:bg-brand-border/60 border border-brand-border'
                      }`}
                    >
                      {tag}
                    </button>
                  );
                })}
              </div>

              {/* Comment Input */}
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Leave an encouraging note for Ramesh or our dark store team..."
                rows={2}
                className="w-full p-2.5 bg-brand-bg border border-brand-border rounded-input text-xs text-brand-text placeholder-brand-muted focus:outline-none focus:ring-1 focus:ring-brand-primary mb-4"
              />

              <button
                onClick={handleSubmit}
                className="w-full py-3 bg-brand-primary hover:bg-brand-primaryHover text-white font-black text-xs sm:text-sm rounded-button transition-all shadow-md active:scale-98 flex items-center justify-center gap-2"
              >
                <Sparkles size={16} className="text-brand-accent" />
                <span>Submit Feedback &amp; Update Rider Rating</span>
              </button>
            </>
          ) : (
            <div className="py-6 space-y-3 animate-in zoom-in-95 duration-200">
              <div className="w-16 h-16 rounded-full bg-emerald-500 text-white flex items-center justify-center mx-auto shadow-md">
                <Heart size={32} className="fill-white" />
              </div>
              <h4 className="text-base font-black text-brand-text">Thank You for the Love!</h4>
              <p className="text-xs text-brand-muted max-w-xs mx-auto">
                Your rating has been updated live on Ramesh&apos;s delivery scorecard. We hope you enjoy your fresh order!
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DeliveryFeedbackModal;
