'use client';

import React, { useState } from 'react';
import {
  Star,
  X,
  Heart,
  CheckCircle2,
  AlertCircle,
  ThumbsUp,
  Coins,
  Send,
} from 'lucide-react';

export interface CustomerFeedbackModalProps {
  isOpen: boolean;
  onClose: () => void;
  orderId: string;
  riderName?: string;
  onFeedbackSubmitted?: (feedback: any) => void;
}

const EXPERIENCE_TAGS = [
  '⚡ Lightning 8-Min Delivery',
  '🥬 Crisp & Fresh Items',
  '🛵 Courteous & Polite Rider',
  '📦 Secure Sealed Packaging',
  '✨ 100% Correct Order',
  '🔔 Followed Doorstep Instructions',
];

const TIP_OPTIONS = [
  { label: 'No Tip', amount: 0 },
  { label: '☕ ₹10 (Chai)', amount: 10 },
  { label: '🥤 ₹20 (Juice)', amount: 20 },
  { label: '⭐ ₹50 (Hero Tip)', amount: 50 },
];

export const CustomerFeedbackModal: React.FC<CustomerFeedbackModalProps> = ({
  isOpen,
  onClose,
  orderId,
  riderName = 'Ramesh Patel',
  onFeedbackSubmitted,
}) => {
  const [rating, setRating] = useState<number>(5);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [selectedTags, setSelectedTags] = useState<string[]>([
    '⚡ Lightning 8-Min Delivery',
    '🛵 Courteous & Polite Rider',
  ]);
  const [comment, setComment] = useState('');
  const [selectedTip, setSelectedTip] = useState<number>(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const toggleTag = (tag: string) => {
    setSelectedTags((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (rating < 1) {
      setErrorMsg('Please tap a star to give a rating');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);

    try {
      const res = await fetch(`/api/orders/${orderId}/feedback`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          rating,
          tags: selectedTags,
          comment,
          tipAmount: selectedTip,
          tipPaid: selectedTip > 0,
        }),
      });

      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || 'Failed to submit review');
      }

      setIsSubmitted(true);
      if (onFeedbackSubmitted) {
        onFeedbackSubmitted(json.data);
      }
      setTimeout(() => {
        onClose();
      }, 2000);
    } catch (err: any) {
      setErrorMsg(err.message || 'Error saving review');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-brand-text/60 backdrop-blur-sm" onClick={onClose} />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-md bg-white rounded-3xl shadow-modal z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="bg-brand-primary text-white p-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-8 h-8 rounded-full bg-brand-accent text-brand-text flex items-center justify-center font-black">
              ⭐
            </span>
            <div>
              <h3 className="text-sm font-black">Delivery Experience</h3>
              <p className="text-[11px] text-white/80">Order #{orderId} • Delivered</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-full hover:bg-white/10 text-white">
            <X size={18} />
          </button>
        </div>

        {isSubmitted ? (
          <div className="p-8 text-center space-y-3">
            <div className="w-14 h-14 bg-emerald-100 text-emerald-700 rounded-full flex items-center justify-center mx-auto">
              <CheckCircle2 size={32} />
            </div>
            <h4 className="text-base font-black text-brand-text">Thank You for Your Feedback!</h4>
            <p className="text-xs text-brand-muted">
              Your 5-star rating and review have been shared with rider {riderName}.
              {selectedTip > 0 && ` A tip of ₹${selectedTip} has been credited to the rider.`}
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-5 space-y-4 max-h-[75vh] overflow-y-auto">
            {errorMsg && (
              <div className="p-2.5 bg-red-50 text-red-700 border border-red-200 rounded-xl text-xs flex items-center gap-2">
                <AlertCircle size={14} className="shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            {/* Rider Identity Banner */}
            <div className="p-3 bg-brand-bg rounded-2xl border border-brand-border flex items-center gap-3">
              <div className="w-11 h-11 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center font-black text-lg">
                🛵
              </div>
              <div className="flex-1">
                <h4 className="text-xs font-black text-brand-text">{riderName}</h4>
                <p className="text-[10px] text-brand-muted">Nexora EV Delivery Fleet Ahmedabad</p>
              </div>
            </div>

            {/* Star Rating Section */}
            <div className="text-center py-1">
              <label className="block text-xs font-bold text-brand-text mb-2">
                How was your 10-minute delivery?
              </label>
              <div className="flex items-center justify-center gap-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    onMouseEnter={() => setHoverRating(star)}
                    onMouseLeave={() => setHoverRating(0)}
                    className="p-1 transition-transform hover:scale-120 focus:outline-none"
                  >
                    <Star
                      size={28}
                      className={`transition-colors ${
                        (hoverRating || rating) >= star
                          ? 'fill-amber-400 text-amber-400 drop-shadow-xs'
                          : 'fill-slate-100 text-slate-300'
                      }`}
                    />
                  </button>
                ))}
              </div>
              <p className="text-[11px] font-black text-brand-primary mt-1.5">
                {rating === 5
                  ? 'Outstanding Experience! 🚀'
                  : rating === 4
                  ? 'Very Good 👍'
                  : rating === 3
                  ? 'Average'
                  : 'Needs Improvement'}
              </p>
            </div>

            {/* Experience Tags */}
            <div>
              <label className="block text-[11px] font-bold text-brand-muted mb-1.5 uppercase tracking-wider">
                What went great? (Select tags)
              </label>
              <div className="flex flex-wrap gap-1.5">
                {EXPERIENCE_TAGS.map((tag) => {
                  const isSelected = selectedTags.includes(tag);
                  return (
                    <button
                      key={tag}
                      type="button"
                      onClick={() => toggleTag(tag)}
                      className={`px-2.5 py-1 rounded-full text-[11px] font-bold transition-all border ${
                        isSelected
                          ? 'bg-brand-primary text-white border-brand-primary shadow-xs'
                          : 'bg-white text-slate-600 border-brand-border hover:bg-slate-50'
                      }`}
                    >
                      {tag}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Comments Field */}
            <div>
              <label className="block text-[11px] font-bold text-brand-muted mb-1 uppercase tracking-wider">
                Comments for {riderName} & Store
              </label>
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Share any special compliments or delivery suggestions..."
                rows={2}
                className="w-full px-3 py-2 text-xs bg-brand-bg border border-brand-border rounded-xl focus:outline-none focus:ring-1 focus:ring-brand-primary resize-none"
              />
            </div>

            {/* Rider Tip Section */}
            <div className="p-3 bg-amber-50/70 border border-amber-200 rounded-2xl space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-black text-amber-900 flex items-center gap-1.5">
                  <Heart size={13} className="text-amber-600 fill-amber-500" />
                  Add a Tip for {riderName}
                </span>
                <span className="text-[10px] text-amber-700 font-bold">100% goes to rider</span>
              </div>
              <div className="grid grid-cols-4 gap-1.5">
                {TIP_OPTIONS.map((tip) => (
                  <button
                    key={tip.amount}
                    type="button"
                    onClick={() => setSelectedTip(tip.amount)}
                    className={`p-1.5 rounded-lg border text-center transition-all text-xs font-bold ${
                      selectedTip === tip.amount
                        ? 'bg-amber-500 text-white border-amber-600 shadow-xs'
                        : 'bg-white text-amber-900 border-amber-200 hover:bg-amber-100/50'
                    }`}
                  >
                    {tip.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Submit Action */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3 bg-brand-primary hover:bg-brand-primaryHover disabled:opacity-70 text-white rounded-xl font-black text-xs flex items-center justify-center gap-2 shadow-card transition-all active:scale-98"
            >
              {isSubmitting ? (
                <span>Submitting review...</span>
              ) : (
                <>
                  <span>
                    Submit Review {selectedTip > 0 ? `& Tip ₹${selectedTip}` : ''}
                  </span>
                  <Send size={13} />
                </>
              )}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};

export default CustomerFeedbackModal;
