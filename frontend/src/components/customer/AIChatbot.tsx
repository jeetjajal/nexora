'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import { X, Send, Sparkles, Bot, ShoppingCart, Plus } from 'lucide-react';

interface ChatMessage {
  id: string;
  sender: 'bot' | 'user';
  text: string;
  suggestions?: string[];
  productAction?: {
    name: string;
    productId: string;
    price: number;
  }[];
}

export const AIChatbot: React.FC = () => {
  const { isAIOpen, setIsAIOpen, addToCart, setIsCartOpen } = useApp();
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'm1',
      sender: 'bot',
      text: "👋 Hi! I am Nexora AI, your 10-minute grocery assistant. How can I help you today? You can ask me for recipe ingredients, healthy snacks, or delivery queries!",
      suggestions: [
        'Add ingredients for Morning Tea ☕',
        'Healthy snacks under ₹50 🥨',
        'How fast do you deliver? ⚡',
      ],
    },
  ]);
  const [input, setInput] = useState('');

  if (!isAIOpen) return null;

  const handleSend = (textToSend?: string) => {
    const q = (textToSend || input).trim();
    if (!q) return;

    const userMsg: ChatMessage = {
      id: `u-${Date.now()}`,
      sender: 'user',
      text: q,
    };

    setMessages(prev => [...prev, userMsg]);
    if (!textToSend) setInput('');

    // Generate smart contextual AI responses
    setTimeout(() => {
      let botResponse: ChatMessage;

      const lower = q.toLowerCase();
      if (lower.includes('tea') || lower.includes('chai')) {
        botResponse = {
          id: `b-${Date.now()}`,
          sender: 'bot',
          text: "Here are the top ingredients for a refreshing hot cup of tea! You can add them straight to your 10-minute cart:",
          productAction: [
            { name: 'Amul Taaza Milk (500 ml)', productId: 'p1', price: 27 },
            { name: 'Nescafe Classic Instant Coffee', productId: 'p20', price: 195 },
          ],
        };
      } else if (lower.includes('snack') || lower.includes('50')) {
        botResponse = {
          id: `b-${Date.now()}`,
          sender: 'bot',
          text: "Here are our customer-favorite munchies ready for 8-minute delivery:",
          productAction: [
            { name: "Lay's Magic Masala", productId: 'p15', price: 20 },
            { name: 'Haldiram Aloo Bhujia', productId: 'p17', price: 49 },
            { name: 'Doritos Cheese Nachos', productId: 'p16', price: 50 },
          ],
        };
      } else if (lower.includes('delivery') || lower.includes('fast') || lower.includes('speed')) {
        botResponse = {
          id: `b-${Date.now()}`,
          sender: 'bot',
          text: "⚡ Nexora delivers in 8 to 12 minutes! Our localized dark stores in your neighborhood use high-speed AI routing and electric vehicle delivery partners to ensure freshness at lightning speed.",
        };
      } else {
        botResponse = {
          id: `b-${Date.now()}`,
          sender: 'bot',
          text: `I've checked our dark store catalog for "${q}". We have freshly stocked dairy, fresh vegetables, snacks, and daily essentials ready for instant dispatch!`,
          productAction: [
            { name: 'Amul Taaza Fresh Milk', productId: 'p1', price: 27 },
            { name: 'Hybrid Red Tomatoes (500g)', productId: 'p5', price: 34 },
          ],
        };
      }

      setMessages(prev => [...prev, botResponse]);
    }, 600);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:justify-end p-0 sm:p-4 pointer-events-none">
      <div
        className="fixed inset-0 bg-brand-text/40 backdrop-blur-xs pointer-events-auto sm:hidden"
        onClick={() => setIsAIOpen(false)}
      />

      <div className="w-full sm:w-96 bg-white rounded-t-card sm:rounded-card border border-brand-border shadow-modal pointer-events-auto flex flex-col h-[520px] max-h-[90vh] z-10 overflow-hidden animate-in slide-in-from-bottom duration-200">
        {/* Header */}
        <div className="bg-brand-primary text-white p-3.5 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-brand-accent text-brand-text flex items-center justify-center">
              <Bot size={18} />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h4 className="text-sm font-black">Nexora AI Shopping Bot</h4>
                <span className="w-2 h-2 rounded-full bg-brand-accent animate-ping" />
              </div>
              <p className="text-[10px] text-white/80">Always active • 10-Min Delivery</p>
            </div>
          </div>
          <button
            onClick={() => setIsAIOpen(false)}
            className="p-1 rounded-full hover:bg-white/10 text-white"
          >
            <X size={18} />
          </button>
        </div>

        {/* Messages Feed */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-brand-bg text-xs">
          {messages.map((m) => (
            <div
              key={m.id}
              className={`flex flex-col ${m.sender === 'user' ? 'items-end' : 'items-start'}`}
            >
              <div
                className={`max-w-[85%] rounded-card p-3 shadow-sm ${
                  m.sender === 'user'
                    ? 'bg-brand-primary text-white rounded-br-none'
                    : 'bg-white text-brand-text border border-brand-border rounded-bl-none'
                }`}
              >
                <p className="leading-relaxed">{m.text}</p>
              </div>

              {/* Product Actions if Bot returned items */}
              {m.productAction && (
                <div className="mt-2 w-full space-y-1.5">
                  {m.productAction.map((prod) => (
                    <div
                      key={prod.productId}
                      className="bg-white p-2 rounded-input border border-brand-border shadow-sm flex items-center justify-between text-xs"
                    >
                      <div>
                        <span className="font-bold text-brand-text block">{prod.name}</span>
                        <span className="text-[11px] text-brand-primary font-black">₹{prod.price}</span>
                      </div>
                      <button
                        onClick={() => {
                          addToCart(prod.productId);
                          setIsCartOpen(true);
                        }}
                        className="px-2.5 py-1 bg-brand-primary hover:bg-brand-primaryHover text-white rounded text-[11px] font-bold flex items-center gap-1 shadow-xs"
                      >
                        <Plus size={11} strokeWidth={3} />
                        <span>Add</span>
                      </button>
                    </div>
                  ))}
                </div>
              )}

              {/* Suggestions pills */}
              {m.suggestions && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {m.suggestions.map((s, idx) => (
                    <button
                      key={idx}
                      onClick={() => handleSend(s)}
                      className="text-[11px] bg-white border border-brand-border text-brand-primary hover:bg-brand-primary hover:text-white px-2.5 py-1 rounded-full transition-colors shadow-xs"
                    >
                      {s}
                    </button>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Input Bar */}
        <div className="p-2.5 bg-white border-t border-brand-border">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSend();
            }}
            className="flex items-center gap-2"
          >
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask anything or request ingredients..."
              className="flex-1 px-3 py-2 text-xs bg-brand-bg border border-brand-border rounded-input focus:outline-none focus:ring-1 focus:ring-brand-primary"
            />
            <button
              type="submit"
              className="p-2 bg-brand-primary hover:bg-brand-primaryHover text-white rounded-button transition-colors shrink-0 shadow-sm"
            >
              <Send size={15} />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default AIChatbot;
