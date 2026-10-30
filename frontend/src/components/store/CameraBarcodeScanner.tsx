'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Camera, X, CheckCircle2, AlertCircle, RefreshCw, Keyboard, Flashlight } from 'lucide-react';

export interface CameraBarcodeScannerProps {
  isOpen: boolean;
  onClose: () => void;
  onBarcodeScanned: (barcode: string) => void;
  expectedItemName?: string;
  expectedSku?: string;
}

export const CameraBarcodeScanner: React.FC<CameraBarcodeScannerProps> = ({
  isOpen,
  onClose,
  onBarcodeScanned,
  expectedItemName,
  expectedSku,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const scanIntervalRef = useRef<any>(null);
  const lastScannedRef = useRef<{ code: string; time: number }>({ code: '', time: 0 });

  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [manualCode, setManualCode] = useState('');
  const [lastScannedResult, setLastScannedResult] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);

  // Stop camera tracks cleanly
  const stopCamera = () => {
    if (scanIntervalRef.current) {
      clearInterval(scanIntervalRef.current);
      scanIntervalRef.current = null;
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
  };

  useEffect(() => {
    if (!isOpen) {
      stopCamera();
      return;
    }

    let isMounted = true;

    const startCamera = async () => {
      try {
        setCameraError(null);
        setHasPermission(null);

        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
          throw new Error('Camera access is not supported by your browser.');
        }

        const stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: 'environment',
            width: { ideal: 1280 },
            height: { ideal: 720 },
          },
          audio: false,
        });

        if (!isMounted) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }

        streamRef.current = stream;
        setHasPermission(true);

        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play().catch(() => {});
        }

        // Setup Barcode Detector if available
        if (typeof window !== 'undefined' && 'BarcodeDetector' in window) {
          const barcodeDetector = new (window as any).BarcodeDetector({
            formats: ['ean_13', 'upc_a', 'code_128', 'qr_code'],
          });

          scanIntervalRef.current = setInterval(async () => {
            if (!videoRef.current || videoRef.current.readyState < 2) return;

            try {
              const barcodes = await barcodeDetector.detect(videoRef.current);
              if (barcodes.length > 0) {
                const detectedCode = barcodes[0].rawValue;
                const now = Date.now();

                // Prevent duplicate trigger within 2 seconds
                if (
                  lastScannedRef.current.code === detectedCode &&
                  now - lastScannedRef.current.time < 2000
                ) {
                  return;
                }

                lastScannedRef.current = { code: detectedCode, time: now };
                setLastScannedResult(detectedCode);

                // Play haptic vibration if supported
                if (typeof navigator !== 'undefined' && navigator.vibrate) {
                  navigator.vibrate(100);
                }

                onBarcodeScanned(detectedCode);
              }
            } catch {
              // Ignore frame detection failures
            }
          }, 350);
        } else {
          // Fallback simulation/canvas loop for environments without native BarcodeDetector
          console.warn('Native BarcodeDetector not available, manual entry is active.');
        }
      } catch (err: any) {
        if (isMounted) {
          setHasPermission(false);
          setCameraError(err.message || 'Unable to access rear camera');
        }
      }
    };

    startCamera();

    return () => {
      isMounted = false;
      stopCamera();
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleManualSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!manualCode.trim()) return;
    onBarcodeScanned(manualCode.trim());
    setLastScannedResult(manualCode.trim());
    setManualCode('');
  };

  const toggleTorch = async () => {
    if (!streamRef.current) return;
    const track = streamRef.current.getVideoTracks()[0];
    if (track && (track.getCapabilities as any)?.()?.torch) {
      try {
        await (track as any).applyConstraints({
          advanced: [{ torch: !torchOn }],
        });
        setTorchOn(!torchOn);
      } catch {
        // Torch toggle not supported
      }
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-brand-text/75 backdrop-blur-md" onClick={onClose} />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-md bg-white rounded-3xl shadow-modal z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-150 flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="bg-[#173327] text-white p-4 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2">
            <span className="w-8 h-8 rounded-full bg-[#176B45] text-white flex items-center justify-center font-black">
              📷
            </span>
            <div>
              <h3 className="text-sm font-black">Camera Barcode Scanner</h3>
              <p className="text-[11px] text-white/80">EAN-13, UPC-A, Code 128</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-full hover:bg-white/10 text-white">
            <X size={18} />
          </button>
        </div>

        {/* Expected Item Reference */}
        {expectedItemName && (
          <div className="bg-amber-50 px-4 py-2 border-b border-amber-200 text-xs flex items-center justify-between text-amber-900">
            <div>
              <span className="text-[10px] uppercase font-bold text-amber-700 block">Item to Scan:</span>
              <span className="font-black text-xs text-amber-950">{expectedItemName}</span>
            </div>
            {expectedSku && (
              <span className="text-[10px] font-mono font-bold bg-white px-2 py-0.5 rounded border border-amber-200">
                SKU: {expectedSku}
              </span>
            )}
          </div>
        )}

        {/* Camera Viewport */}
        <div className="relative bg-black w-full h-64 sm:h-72 overflow-hidden flex items-center justify-center">
          <video
            ref={videoRef}
            playsInline
            muted
            className="w-full h-full object-cover"
          />

          {/* Scanner Reticle Overlay */}
          <div className="absolute inset-0 pointer-events-none flex items-center justify-center">
            <div className="relative w-64 h-36 border-2 border-emerald-400 rounded-2xl shadow-[0_0_0_9999px_rgba(0,0,0,0.45)]">
              {/* Corner Accents */}
              <div className="absolute -top-1 -left-1 w-4 h-4 border-t-3 border-l-3 border-emerald-400 rounded-tl" />
              <div className="absolute -top-1 -right-1 w-4 h-4 border-t-3 border-r-3 border-emerald-400 rounded-tr" />
              <div className="absolute -bottom-1 -left-1 w-4 h-4 border-b-3 border-l-3 border-emerald-400 rounded-bl" />
              <div className="absolute -bottom-1 -right-1 w-4 h-4 border-b-3 border-r-3 border-emerald-400 rounded-br" />

              {/* Scanning Red Laser Line Animation */}
              <div className="absolute left-2 right-2 h-0.5 bg-red-500 shadow-[0_0_8px_red] animate-pulse top-1/2 -translate-y-1/2" />
            </div>
          </div>

          {/* Camera Permission / Error Overlay */}
          {hasPermission === false && (
            <div className="absolute inset-0 bg-slate-900/90 text-white flex flex-col items-center justify-center p-4 text-center">
              <AlertCircle size={32} className="text-amber-400 mb-2" />
              <h4 className="text-sm font-bold">Camera Access Blocked</h4>
              <p className="text-xs text-slate-300 mt-1 max-w-xs">{cameraError}</p>
              <p className="text-[11px] text-emerald-400 mt-2">Use the manual barcode entry below</p>
            </div>
          )}

          {/* Last Scanned Tag */}
          {lastScannedResult && (
            <div className="absolute top-3 inset-x-4 bg-emerald-600 text-white px-3 py-1.5 rounded-full text-xs font-bold text-center shadow-md animate-in fade-in flex items-center justify-center gap-1.5">
              <CheckCircle2 size={14} />
              <span>Scanned: {lastScannedResult}</span>
            </div>
          )}
        </div>

        {/* Manual Barcode Fallback Input */}
        <div className="p-4 bg-white border-t border-brand-border space-y-3">
          <form onSubmit={handleManualSubmit} className="flex items-center gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                value={manualCode}
                onChange={(e) => setManualCode(e.target.value)}
                placeholder="Or type product barcode (e.g. 8901030...)"
                className="w-full pl-8 pr-3 py-2 text-xs bg-brand-bg border border-brand-border rounded-xl focus:outline-none focus:ring-1 focus:ring-brand-primary font-mono"
              />
              <Keyboard size={14} className="absolute left-2.5 top-2.5 text-brand-muted" />
            </div>
            <button
              type="submit"
              className="px-4 py-2 bg-[#176B45] hover:bg-[#125335] text-white text-xs font-bold rounded-xl transition-all shadow-xs shrink-0"
            >
              Verify
            </button>
          </form>

          <p className="text-[10px] text-brand-muted text-center">
            Point camera directly at the item barcode. The scanner auto-detects and confirms stock.
          </p>
        </div>
      </div>
    </div>
  );
};

export default CameraBarcodeScanner;
