'use client';

import React, { useEffect, useState } from 'react';
import { useApp } from '@/context/AppContext';
import { ApiClient, OrderTrackingInfo } from '@/lib/api-client';
import { LiveDeliveryMap } from './LiveDeliveryMap';
import { CustomerFeedbackModal } from './CustomerFeedbackModal';
import {
  X,
  CheckCircle2,
  Phone,
  MapPin,
  Clock,
  ShieldCheck,
  AlertTriangle,
  RefreshCw,
  XCircle,
  Navigation,
  Star,
  Map as MapIcon,
  Activity,
} from 'lucide-react';

export const OrderTrackingModal: React.FC = () => {
  const {
    isTrackingOpen,
    setIsTrackingOpen,
    activeOrder,
    currentUser,
  } = useApp();

  const [trackingData, setTrackingData] = useState<OrderTrackingInfo | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [cancelMessage, setCancelMessage] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'MAP' | 'TIMELINE'>('MAP');
  const [isFeedbackOpen, setIsFeedbackOpen] = useState(false);

  // Poll tracking endpoint while modal is open
  useEffect(() => {
    if (!isTrackingOpen || !activeOrder) return;

    let isMounted = true;

    const fetchTracking = async () => {
      try {
        setIsPolling(true);
        const data = await ApiClient.trackOrder(activeOrder.id, currentUser?.accessToken);
        if (isMounted && data) {
          setTrackingData(data);
        }
      } catch {
        // Fallback gracefully
      } finally {
        if (isMounted) setIsPolling(false);
      }
    };

    fetchTracking();
    const intervalId = setInterval(fetchTracking, 4500);

    return () => {
      isMounted = false;
      clearInterval(intervalId);
    };
  }, [isTrackingOpen, activeOrder, currentUser?.accessToken]);

  if (!isTrackingOpen || !activeOrder) return null;

  const currentStatus = trackingData?.status || activeOrder.status;
  const isDelivered = currentStatus === 'DELIVERED';
  const isCancelled = currentStatus === 'CANCELLED';
  const isCancellable = currentStatus === 'PLACED' || currentStatus === 'CONFIRMED' || currentStatus === 'CREATED';

  const defaultTimeline = [
    { key: 'PLACED', label: 'Order Placed', time: 'Just now', desc: 'Received at Dark Store #04 SG Highway' },
    { key: 'CONFIRMED', label: 'Order Confirmed', time: '1 min ago', desc: 'Stock allocated to dark store queue' },
    { key: 'PACKING', label: 'Packing Items', time: '3 mins ago', desc: 'Picker barcode-scanning items' },
    { key: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', time: 'On the way', desc: 'Rider Ramesh Patel en route on EV' },
    { key: 'DELIVERED', label: 'Delivered', time: 'Completed', desc: 'Handed over at doorstep via OTP' },
  ];

  const timelineEvents = trackingData?.timeline && trackingData.timeline.length > 0
    ? trackingData.timeline
    : defaultTimeline.map((s, idx) => ({
        status: s.key,
        title: s.label,
        description: s.desc,
        timestamp: s.time,
        completed: idx <= (currentStatus === 'DELIVERED' ? 4 : currentStatus === 'OUT_FOR_DELIVERY' ? 3 : 2),
        current: idx === (currentStatus === 'DELIVERED' ? 4 : currentStatus === 'OUT_FOR_DELIVERY' ? 3 : 2),
      }));

  const handleCancelOrder = async () => {
    if (!confirm('Are you sure you want to cancel this order?')) return;
    setIsCancelling(true);
    setCancelMessage(null);
    try {
      await ApiClient.cancelOrder(activeOrder.id, currentUser?.accessToken);
      setCancelMessage('Order cancelled successfully. Stock restored.');
      // Refresh
      const data = await ApiClient.trackOrder(activeOrder.id, currentUser?.accessToken);
      setTrackingData(data);
    } catch (err: any) {
      setCancelMessage(err.message || 'Could not cancel order');
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
        {/* Backdrop */}
        <div
          className="fixed inset-0 bg-brand-text/60 backdrop-blur-sm"
          onClick={() => setIsTrackingOpen(false)}
        />

        {/* Modal Window */}
        <div className="relative w-full max-w-lg bg-white rounded-3xl shadow-modal z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-150 flex flex-col max-h-[92vh]">
          {/* Header */}
          <div className="bg-brand-primary text-white p-4 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2">
              <span className="w-8 h-8 rounded-full bg-brand-accent text-brand-text flex items-center justify-center font-black">
                ⚡
              </span>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-black">Live Order Tracker</h3>
                  <span className="text-[10px] bg-brand-accent text-brand-text px-2 py-0.5 rounded-full font-black">
                    {activeOrder.id}
                  </span>
                  {isPolling && (
                    <RefreshCw size={11} className="animate-spin text-white/70" />
                  )}
                </div>
                <p className="text-[11px] text-white/80">
                  {isDelivered
                    ? 'Successfully Delivered'
                    : isCancelled
                    ? 'Order Cancelled'
                    : `Rapid Delivery: Arriving in ~${trackingData?.estimatedDeliveryMinutes || activeOrder.etaMinutes} Mins`}
                </p>
              </div>
            </div>
            <button
              onClick={() => setIsTrackingOpen(false)}
              className="p-1.5 rounded-full hover:bg-white/10 text-white"
            >
              <X size={18} />
            </button>
          </div>

          {/* Tab Selector: Live Map vs Timeline */}
          <div className="bg-slate-100 p-1 border-b border-brand-border flex items-center justify-center gap-1 shrink-0">
            <button
              onClick={() => setViewMode('MAP')}
              className={`px-4 py-1.5 rounded-full text-xs font-bold flex items-center gap-1.5 transition-all ${
                viewMode === 'MAP'
                  ? 'bg-white text-brand-primary shadow-xs'
                  : 'text-slate-600 hover:text-brand-text'
              }`}
            >
              <MapIcon size={13} />
              <span>Interactive Road Map</span>
            </button>
            <button
              onClick={() => setViewMode('TIMELINE')}
              className={`px-4 py-1.5 rounded-full text-xs font-bold flex items-center gap-1.5 transition-all ${
                viewMode === 'TIMELINE'
                  ? 'bg-white text-brand-primary shadow-xs'
                  : 'text-slate-600 hover:text-brand-text'
              }`}
            >
              <Activity size={13} />
              <span>Dispatch Timeline</span>
            </button>
          </div>

          {/* Scrollable Modal Content */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {/* Delivery OTP & Status Banner */}
            <div className="bg-[#eef5ee] border border-brand-border p-3.5 rounded-2xl flex items-center justify-between">
              <div>
                <p className="text-[11px] font-bold text-brand-text">Delivery Verification PIN</p>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xl font-black tracking-widest text-brand-primary font-mono bg-white px-3 py-0.5 rounded-lg border border-brand-border shadow-xs">
                    {activeOrder.deliveryOtp}
                  </span>
                  <span className="text-[10px] text-brand-muted">Share with rider at doorstep</span>
                </div>
              </div>
              <div>
                {isCancelled ? (
                  <span className="text-[11px] font-bold text-red-700 bg-red-100 px-2.5 py-1 rounded-full">
                    Cancelled
                  </span>
                ) : isDelivered ? (
                  <span className="text-[11px] font-bold text-emerald-800 bg-emerald-100 px-2.5 py-1 rounded-full flex items-center gap-1">
                    <CheckCircle2 size={12} /> Delivered
                  </span>
                ) : (
                  <span className="text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-full flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping" />
                    In Transit
                  </span>
                )}
              </div>
            </div>

            {/* View 1: Real-Time Leaflet Road Map */}
            {viewMode === 'MAP' && (
              <div className="space-y-3">
                <LiveDeliveryMap
                  storeName={activeOrder.darkStore || 'Nexora Dark Store SG Highway #04'}
                  customerAddress={activeOrder.address}
                  riderName={trackingData?.assignedRiderName || activeOrder.riderName}
                  riderVehicle={trackingData?.assignedRiderVehicle || 'Hero Electric NYX EV'}
                  isDelivered={isDelivered}
                  etaMinutes={trackingData?.estimatedDeliveryMinutes || activeOrder.etaMinutes}
                />

                {/* Rider Card */}
                <div className="bg-white p-3 rounded-2xl border border-brand-border shadow-xs flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center font-black text-lg">
                      🛵
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <h4 className="text-xs font-black text-brand-text">
                          {trackingData?.assignedRiderName || activeOrder.riderName}
                        </h4>
                        <span className="text-[10px] bg-amber-100 text-amber-800 font-bold px-1.5 py-0.2 rounded">
                          ★ 4.92
                        </span>
                      </div>
                      <p className="text-[10px] text-brand-muted">
                        Nexora EV Fleet • {trackingData?.assignedRiderVehicle || 'Electric EV'}
                      </p>
                    </div>
                  </div>

                  <a
                    href={`tel:${trackingData?.assignedRiderPhone || activeOrder.riderPhone}`}
                    className="px-3 py-1.5 bg-brand-primary/10 hover:bg-brand-primary hover:text-white text-brand-primary rounded-xl text-xs font-bold flex items-center gap-1 transition-colors"
                  >
                    <Phone size={12} />
                    <span>Call Rider</span>
                  </a>
                </div>
              </div>
            )}

            {/* View 2: Progress Timeline */}
            {viewMode === 'TIMELINE' && (
              <div className="bg-white p-4 rounded-2xl border border-brand-border space-y-4">
                <div className="relative pl-6 space-y-5 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-brand-border">
                  {timelineEvents.map((s, idx) => {
                    const isDone = s.completed;
                    const isCurrent = s.current;

                    return (
                      <div key={s.status || idx} className="relative flex items-start justify-between">
                        <div
                          className={`absolute -left-6 top-0.5 w-5 h-5 rounded-full flex items-center justify-center transition-all ${
                            isDone
                              ? 'bg-brand-primary text-white shadow-xs ring-4 ring-brand-primary/15'
                              : 'bg-white border-2 border-brand-border text-transparent'
                          }`}
                        >
                          {isDone && <CheckCircle2 size={13} strokeWidth={3} />}
                        </div>

                        <div className="flex-1 pr-4">
                          <p className={`text-xs font-bold ${isCurrent ? 'text-brand-primary font-black' : isDone ? 'text-brand-text' : 'text-brand-muted'}`}>
                            {s.title}
                          </p>
                          <p className="text-[11px] text-brand-muted">{s.description}</p>
                        </div>

                        <span className="text-[10px] font-semibold text-brand-muted shrink-0">
                          {typeof s.timestamp === 'string' && s.timestamp.includes('T')
                            ? new Date(s.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                            : s.timestamp}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Delivered: Rating Prompt Button */}
            {isDelivered && (
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-2xl flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Star size={16} className="text-amber-500 fill-amber-400" />
                  <span className="text-xs font-bold text-amber-900">How was your delivery?</span>
                </div>
                <button
                  type="button"
                  onClick={() => setIsFeedbackOpen(true)}
                  className="px-3 py-1.5 bg-brand-primary text-white rounded-xl text-xs font-bold hover:bg-brand-primaryHover transition-all shadow-xs"
                >
                  Rate Rider & Tip
                </button>
              </div>
            )}

            {/* Delivery Destination Address */}
            <div className="bg-brand-bg p-3 rounded-2xl border border-brand-border text-xs flex items-start gap-2">
              <MapPin size={14} className="text-brand-primary mt-0.5 shrink-0" />
              <div>
                <p className="font-bold text-brand-text text-[11px]">Destination:</p>
                <p className="text-[11px] text-brand-muted">{activeOrder.address}</p>
              </div>
            </div>
          </div>

          {/* Footer Controls */}
          <div className="p-4 bg-white border-t border-brand-border flex items-center justify-between gap-3 shrink-0">
            {isCancellable && !isCancelled && !isDelivered ? (
              <button
                onClick={handleCancelOrder}
                disabled={isCancelling}
                className="py-2 px-3 bg-red-50 hover:bg-red-100 text-red-700 font-bold rounded-xl text-xs transition-colors border border-red-200 flex items-center gap-1.5"
              >
                <XCircle size={13} />
                <span>{isCancelling ? 'Cancelling...' : 'Cancel'}</span>
              </button>
            ) : null}

            <button
              onClick={() => setIsTrackingOpen(false)}
              className="flex-1 py-2.5 bg-brand-primary hover:bg-brand-primaryHover text-white font-black rounded-xl text-xs transition-colors shadow-sm text-center"
            >
              Back to Shopping
            </button>
          </div>
        </div>
      </div>

      {/* Customer Feedback Modal */}
      <CustomerFeedbackModal
        isOpen={isFeedbackOpen}
        onClose={() => setIsFeedbackOpen(false)}
        orderId={activeOrder.id}
        riderName={trackingData?.assignedRiderName || activeOrder.riderName}
      />
    </>
  );
};

export default OrderTrackingModal;
