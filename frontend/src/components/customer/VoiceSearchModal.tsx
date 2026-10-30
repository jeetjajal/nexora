'use client';

import React, { useState, useEffect, useRef } from 'react';
import { useApp } from '@/context/AppContext';
import {
  Mic,
  MicOff,
  X,
  Volume2,
  Search,
  ShoppingCart,
  CheckCircle2,
  AlertCircle,
  Plus,
} from 'lucide-react';

export interface VoiceSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
}

type SupportedLanguage = 'gu-IN' | 'hi-IN' | 'en-IN';

interface MatchedVoiceProduct {
  product: any;
  detectedQuantity: number;
  matchedKeyword: string;
}

export const VoiceSearchModal: React.FC<VoiceSearchModalProps> = ({ isOpen, onClose }) => {
  const { products, addToCart, setSearchQuery } = useApp();

  const [language, setLanguage] = useState<SupportedLanguage>('gu-IN');
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [recognitionError, setRecognitionError] = useState<string | null>(null);
  const [matchedProducts, setMatchedProducts] = useState<MatchedVoiceProduct[]>([]);
  const [addedItems, setAddedItems] = useState<Record<string, boolean>>({});

  const recognitionRef = useRef<any>(null);

  // Stop recognition
  const stopListening = () => {
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop();
      } catch {}
      recognitionRef.current = null;
    }
    setIsListening(false);
  };

  // Start speech recognition
  const startListening = () => {
    setRecognitionError(null);
    setTranscript('');
    setMatchedProducts([]);

    if (typeof window === 'undefined') return;

    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setRecognitionError(
        'Speech recognition is not supported in this browser. Please use Chrome, Edge, or Safari.'
      );
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.lang = language;
      recognition.continuous = false;
      recognition.interimResults = true;

      recognition.onstart = () => {
        setIsListening(true);
      };

      recognition.onresult = (event: any) => {
        let currentTranscript = '';
        for (let i = event.resultIndex; i < event.results.length; ++i) {
          currentTranscript += event.results[i][0].transcript;
        }
        setTranscript(currentTranscript);

        if (event.results[0].isFinal) {
          processTranscript(currentTranscript);
        }
      };

      recognition.onerror = (event: any) => {
        setIsListening(false);
        if (event.error === 'not-allowed') {
          setRecognitionError('Microphone permission was denied. Please allow microphone access in your browser settings.');
        } else if (event.error === 'no-speech') {
          setRecognitionError('No speech detected. Please try speaking again.');
        } else {
          setRecognitionError(`Speech recognition error: ${event.error}`);
        }
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognition;
      recognition.start();
    } catch (err: any) {
      setRecognitionError(err.message || 'Failed to start microphone');
      setIsListening(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      startListening();
    } else {
      stopListening();
    }
    return () => stopListening();
  }, [isOpen, language]);

  // Multilingual entity & keyword matching
  const processTranscript = (spokenText: string) => {
    const text = spokenText.toLowerCase();

    // Multilingual Dictionary mapping spoken terms to product keywords
    const keywordMap: Record<string, string[]> = {
      // Gujarati
      'દૂધ': ['milk', 'dairy', 'amul'],
      'બ્રેડ': ['bread', 'bakery', 'pav'],
      'માખણ': ['butter', 'amul'],
      'બટાકા': ['potato', 'vegetable'],
      'ડુંગળી': ['onion', 'vegetable'],
      'ટામેટા': ['tomato', 'vegetable'],
      'કેળા': ['banana', 'fruit'],
      'સફરજન': ['apple', 'fruit'],
      'તેલ': ['oil', 'mustard'],
      'ઘી': ['ghee', 'amul'],
      'ચીપ્સ': ['chips', 'lays', 'snacks'],
      'સોડા': ['coke', 'pepsi', 'drink'],

      // Hindi
      'दूध': ['milk', 'dairy', 'amul'],
      'ब्रेड': ['bread', 'bakery', 'pav'],
      'मक्खन': ['butter', 'amul'],
      'आलू': ['potato', 'vegetable'],
      'प्याज': ['onion', 'vegetable'],
      'टमाटर': ['tomato', 'vegetable'],
      'केला': ['banana', 'fruit'],
      'सेब': ['apple', 'fruit'],
      'तेल': ['oil', 'mustard'],
      'घी': ['ghee', 'amul'],
      'चिप्स': ['chips', 'lays', 'snacks'],
      'चाय': ['tea', 'chai', 'coffee'],

      // English
      'milk': ['milk', 'dairy'],
      'bread': ['bread', 'bakery'],
      'butter': ['butter'],
      'potato': ['potato'],
      'onion': ['onion'],
      'tomato': ['tomato'],
      'chips': ['chips', 'lays'],
      'biscuit': ['biscuit', 'cookie'],
      'water': ['water', 'drink'],
    };

    // Quantity parser
    let detectedQty = 1;
    if (text.includes('બે') || text.includes('દો') || text.includes('two') || text.includes(' 2 ')) {
      detectedQty = 2;
    } else if (text.includes('ત્રણ') || text.includes('तीन') || text.includes('three') || text.includes(' 3 ')) {
      detectedQty = 3;
    } else if (text.includes('ચાર') || text.includes('चार') || text.includes('four') || text.includes(' 4 ')) {
      detectedQty = 4;
    } else if (text.includes('પાંચ') || text.includes('पांच') || text.includes('five') || text.includes(' 5 ')) {
      detectedQty = 5;
    }

    const matches: MatchedVoiceProduct[] = [];
    const addedProductIds = new Set<string | number>();

    // Check transcript against dictionary terms
    for (const [spokenTerm, targetKeywords] of Object.entries(keywordMap)) {
      if (text.includes(spokenTerm)) {
        for (const kw of targetKeywords) {
          const found = products.filter(
            (p) =>
              p.name.toLowerCase().includes(kw) &&
              !addedProductIds.has(p.id)
          );

          for (const prod of found.slice(0, 2)) {
            addedProductIds.add(prod.id);
            matches.push({
              product: prod,
              detectedQuantity: detectedQty,
              matchedKeyword: spokenTerm,
            });
          }
        }
      }
    }

    // Direct search fallback if no dictionary keyword hit
    if (matches.length === 0) {
      const words = text.split(/\s+/).filter((w) => w.length >= 3);
      for (const word of words) {
        const found = products.filter(
          (p) =>
            p.name.toLowerCase().includes(word) && !addedProductIds.has(p.id)
        );
        for (const prod of found.slice(0, 3)) {
          addedProductIds.add(prod.id);
          matches.push({
            product: prod,
            detectedQuantity: detectedQty,
            matchedKeyword: word,
          });
        }
      }
    }

    setMatchedProducts(matches);
  };

  const handleAddToCart = (item: MatchedVoiceProduct) => {
    addToCart(item.product);
    setAddedItems((prev) => ({ ...prev, [item.product.id]: true }));
    setTimeout(() => {
      setAddedItems((prev) => ({ ...prev, [item.product.id]: false }));
    }, 2500);
  };

  const handleApplyToSearch = () => {
    if (transcript.trim()) {
      setSearchQuery(transcript.trim());
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-brand-text/75 backdrop-blur-md" onClick={onClose} />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-lg bg-white rounded-3xl shadow-modal z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-150 flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="bg-[#173327] text-white p-4 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2">
            <span className="w-8 h-8 rounded-full bg-brand-accent text-brand-text flex items-center justify-center font-black">
              🎙️
            </span>
            <div>
              <h3 className="text-sm font-black">Multilingual Voice Search</h3>
              <p className="text-[11px] text-white/80">Gujarati • Hindi • English</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-full hover:bg-white/10 text-white">
            <X size={18} />
          </button>
        </div>

        {/* Language Tabs */}
        <div className="bg-slate-100 p-2 border-b border-brand-border flex items-center justify-center gap-2 shrink-0">
          {[
            { id: 'gu-IN', label: 'ગુજરાતી (Gujarati)' },
            { id: 'hi-IN', label: 'हिंदी (Hindi)' },
            { id: 'en-IN', label: 'English' },
          ].map((lang) => (
            <button
              key={lang.id}
              type="button"
              onClick={() => {
                setLanguage(lang.id as SupportedLanguage);
              }}
              className={`px-3 py-1 rounded-full text-xs font-bold transition-all ${
                language === lang.id
                  ? 'bg-brand-primary text-white shadow-xs'
                  : 'bg-white text-slate-700 border border-slate-200 hover:bg-slate-50'
              }`}
            >
              {lang.label}
            </button>
          ))}
        </div>

        {/* Listening Interactive Area */}
        <div className="p-6 text-center space-y-4">
          <div className="relative flex items-center justify-center">
            {isListening && (
              <div className="absolute w-24 h-24 rounded-full bg-emerald-500/20 animate-ping" />
            )}
            <button
              type="button"
              onClick={isListening ? stopListening : startListening}
              className={`relative w-20 h-20 rounded-full flex items-center justify-center transition-all shadow-md active:scale-95 ${
                isListening
                  ? 'bg-emerald-600 text-white shadow-emerald-500/50'
                  : 'bg-slate-200 text-slate-700 hover:bg-slate-300'
              }`}
            >
              {isListening ? (
                <Mic size={36} className="animate-pulse" />
              ) : (
                <MicOff size={32} />
              )}
            </button>
          </div>

          <div>
            <h4 className="text-sm font-black text-brand-text">
              {isListening ? 'Listening... Speak now' : 'Tap Microphone to Speak'}
            </h4>
            <p className="text-xs text-brand-muted mt-1">
              Try saying: <span className="font-bold text-brand-primary font-serif">"બે લીટર દૂધ"</span> or <span className="font-bold text-brand-primary">"ब्रेड और बटर"</span>
            </p>
          </div>

          {/* Transcript Display Box */}
          <div className="p-3 bg-brand-bg rounded-2xl border border-brand-border min-h-[50px] flex items-center justify-center text-center">
            {transcript ? (
              <p className="text-sm font-bold text-brand-text font-medium italic">
                "{transcript}"
              </p>
            ) : (
              <span className="text-xs text-brand-muted">
                {isListening ? 'Listening for speech...' : 'Recognized words will appear here'}
              </span>
            )}
          </div>

          {recognitionError && (
            <div className="p-2.5 bg-red-50 text-red-700 border border-red-200 rounded-xl text-xs flex items-center gap-2 text-left">
              <AlertCircle size={15} className="shrink-0" />
              <span>{recognitionError}</span>
            </div>
          )}
        </div>

        {/* Matched Products Card List */}
        {matchedProducts.length > 0 && (
          <div className="p-4 bg-slate-50 border-t border-brand-border space-y-2 flex-1 overflow-y-auto max-h-[36vh]">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                Recognized Items ({matchedProducts.length})
              </span>
              <button
                type="button"
                onClick={handleApplyToSearch}
                className="text-[11px] font-bold text-brand-primary hover:underline flex items-center gap-1"
              >
                <Search size={11} />
                <span>Search All Results</span>
              </button>
            </div>

            <div className="space-y-2">
              {matchedProducts.map(({ product, detectedQuantity }) => (
                <div
                  key={product.id}
                  className="p-3 bg-white rounded-2xl border border-brand-border flex items-center justify-between gap-3 shadow-xs"
                >
                  <div className="flex items-center gap-3">
                    <img
                      src={product.imageUrl || '/assets/brand/app-icon.png'}
                      alt={product.name}
                      className="w-12 h-12 rounded-xl object-contain bg-slate-50 border border-slate-100 p-1"
                    />
                    <div>
                      <h5 className="text-xs font-bold text-brand-text line-clamp-1">{product.name}</h5>
                      <p className="text-[11px] font-black text-brand-primary">₹{product.price}</p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleAddToCart({ product, detectedQuantity, matchedKeyword: '' })}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all shadow-xs ${
                      addedItems[product.id]
                        ? 'bg-emerald-600 text-white'
                        : 'bg-brand-primary text-white hover:bg-brand-primaryHover'
                    }`}
                  >
                    {addedItems[product.id] ? (
                      <>
                        <CheckCircle2 size={13} />
                        <span>Added!</span>
                      </>
                    ) : (
                      <>
                        <Plus size={13} />
                        <span>Add Qty {detectedQuantity}</span>
                      </>
                    )}
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="p-4 bg-white border-t border-brand-border flex items-center justify-between gap-2 shrink-0">
          <button
            type="button"
            onClick={handleApplyToSearch}
            disabled={!transcript.trim()}
            className="flex-1 py-2.5 bg-brand-primary hover:bg-brand-primaryHover disabled:opacity-50 text-white rounded-xl font-black text-xs transition-colors shadow-xs"
          >
            Search For "{transcript.slice(0, 20) || '...'}"
          </button>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-bold text-xs transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default VoiceSearchModal;
