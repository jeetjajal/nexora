'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Navigation, AlertCircle, RefreshCw, Zap, Wifi, WifiOff } from 'lucide-react';

export interface LocationCoordinates {
  lat: number;
  lng: number;
}

export interface LiveDeliveryMapProps {
  storeLocation?: LocationCoordinates;
  customerLocation?: LocationCoordinates;
  riderLocation?: LocationCoordinates | null;
  storeName?: string;
  customerAddress?: string;
  riderName?: string;
  riderVehicle?: string;
  isDelivered?: boolean;
  isRiderOnline?: boolean;
  lastPingTime?: string | Date;
  etaMinutes?: number;
  speedKmh?: number;
  streamMode?: 'SSE' | 'POLLING' | 'CONNECTING';
}

// Fallback coordinates for Ahmedabad Dark Store #04 and Bodakdev customer destination
const DEFAULT_STORE_COORDS: LocationCoordinates = { lat: 23.0373, lng: 72.5118 };
const DEFAULT_CUSTOMER_COORDS: LocationCoordinates = { lat: 23.0315, lng: 72.5072 };

export const LiveDeliveryMap: React.FC<LiveDeliveryMapProps> = ({
  storeLocation = DEFAULT_STORE_COORDS,
  customerLocation = DEFAULT_CUSTOMER_COORDS,
  riderLocation,
  storeName = 'Nexora Dark Store SG Highway #04',
  customerAddress = 'B-402, Shivalik Residency, SG Highway, Ahmedabad',
  riderName = 'Ramesh Patel',
  riderVehicle = 'Hero Electric NYX EV',
  isDelivered = false,
  isRiderOnline = true,
  lastPingTime,
  etaMinutes = 6,
  speedKmh = 24.5,
  streamMode = 'SSE',
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<any>(null);
  const riderMarkerRef = useRef<any>(null);
  const routeLineRef = useRef<any>(null);
  const prevRiderPosRef = useRef<LocationCoordinates | null>(null);

  const [isLeafletLoaded, setIsLeafletLoaded] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Compute resolved rider position (interpolated between store and customer if not explicitly provided)
  const currentRiderPos: LocationCoordinates = isDelivered
    ? customerLocation
    : riderLocation || {
        lat: storeLocation.lat + (customerLocation.lat - storeLocation.lat) * 0.65,
        lng: storeLocation.lng + (customerLocation.lng - storeLocation.lng) * 0.65,
      };

  // Check if location is stale (older than 90 seconds)
  const isStale = lastPingTime
    ? Date.now() - new Date(lastPingTime).getTime() > 90000
    : false;

  // 1. Load Leaflet CSS and JS dynamically
  useEffect(() => {
    if (typeof window === 'undefined') return;

    if ((window as any).L) {
      setIsLeafletLoaded(true);
      return;
    }

    // Inject CSS
    const existingCss = document.getElementById('leaflet-css');
    if (!existingCss) {
      const link = document.createElement('link');
      link.id = 'leaflet-css';
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      link.crossOrigin = '';
      document.head.appendChild(link);
    }

    // Inject JS
    const existingJs = document.getElementById('leaflet-js');
    if (!existingJs) {
      const script = document.createElement('script');
      script.id = 'leaflet-js';
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.crossOrigin = '';
      script.onload = () => setIsLeafletLoaded(true);
      script.onerror = () => setLoadError('Unable to load OpenStreetMap engine');
      document.body.appendChild(script);
    } else {
      existingJs.addEventListener('load', () => setIsLeafletLoaded(true));
    }
  }, []);

  // 2. Initialize and update Leaflet Map with smooth LERP interpolation
  useEffect(() => {
    if (!isLeafletLoaded || !mapContainerRef.current) return;
    const L = (window as any).L;
    if (!L) return;

    if (!mapInstanceRef.current) {
      // Create Map
      const map = L.map(mapContainerRef.current, {
        zoomControl: false,
        attributionControl: false,
      }).setView([currentRiderPos.lat, currentRiderPos.lng], 14);

      // OpenStreetMap Tile Layer
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
      }).addTo(map);

      // Custom HTML Icons
      const storeIcon = L.divIcon({
        className: 'custom-map-icon',
        html: `<div style="background:#176B45; color:#fff; width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center; border:2.5px solid #fff; box-shadow:0 2px 6px rgba(0,0,0,0.3); font-size:15px;">🏬</div>`,
        iconSize: [32, 32],
        iconAnchor: [16, 16],
      });

      const customerIcon = L.divIcon({
        className: 'custom-map-icon',
        html: `<div style="background:#d92d20; color:#fff; width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center; border:2.5px solid #fff; box-shadow:0 2px 6px rgba(0,0,0,0.3); font-size:15px;">🏠</div>`,
        iconSize: [32, 32],
        iconAnchor: [16, 16],
      });

      const riderIcon = L.divIcon({
        className: 'custom-map-icon',
        html: `<div style="background:#D8F36A; color:#176B45; width:36px; height:36px; border-radius:50%; display:flex; align-items:center; justify-content:center; border:3px solid #176B45; box-shadow:0 3px 10px rgba(0,0,0,0.35); font-size:18px; transform: scale(1.1); font-weight:bold;">⚡🛵</div>`,
        iconSize: [36, 36],
        iconAnchor: [18, 18],
      });

      // Markers
      L.marker([storeLocation.lat, storeLocation.lng], { icon: storeIcon })
        .addTo(map)
        .bindPopup(`<b>${storeName}</b><br/>Dark Store Origin`);

      L.marker([customerLocation.lat, customerLocation.lng], { icon: customerIcon })
        .addTo(map)
        .bindPopup(`<b>Delivery Destination</b><br/>${customerAddress}`);

      const riderMarker = L.marker([currentRiderPos.lat, currentRiderPos.lng], { icon: riderIcon })
        .addTo(map)
        .bindPopup(`<b>${riderName}</b> (${riderVehicle})<br/>${isDelivered ? 'Delivered' : `Speed: ${speedKmh} km/h • ${isRiderOnline ? 'Online' : 'Offline'}`}`);

      riderMarkerRef.current = riderMarker;
      prevRiderPosRef.current = { lat: currentRiderPos.lat, lng: currentRiderPos.lng };

      // Draw Polyline Route
      const routePoints = [
        [storeLocation.lat, storeLocation.lng],
        [currentRiderPos.lat, currentRiderPos.lng],
        [customerLocation.lat, customerLocation.lng],
      ];

      const polyline = L.polyline(routePoints, {
        color: '#176B45',
        weight: 4,
        opacity: 0.85,
        dashArray: '8, 8',
      }).addTo(map);

      routeLineRef.current = polyline;

      // Fit bounds to enclose all coordinates with padding
      const group = new L.featureGroup([
        L.marker([storeLocation.lat, storeLocation.lng]),
        L.marker([customerLocation.lat, customerLocation.lng]),
        riderMarker,
      ]);
      map.fitBounds(group.getBounds().pad(0.2));

      mapInstanceRef.current = map;
    } else {
      // Smooth Linear Interpolation (LERP) of Rider Marker
      const prev = prevRiderPosRef.current || currentRiderPos;
      const target = currentRiderPos;

      if (riderMarkerRef.current && (prev.lat !== target.lat || prev.lng !== target.lng)) {
        const startTime = performance.now();
        const duration = 800; // 800ms smooth gliding transition

        const animateGlide = (currentTime: number) => {
          const elapsed = currentTime - startTime;
          const progress = Math.min(elapsed / duration, 1.0);
          // Ease-out cubic curve
          const ease = 1 - Math.pow(1 - progress, 3);

          const curLat = prev.lat + (target.lat - prev.lat) * ease;
          const curLng = prev.lng + (target.lng - prev.lng) * ease;

          if (riderMarkerRef.current) {
            riderMarkerRef.current.setLatLng([curLat, curLng]);
          }

          if (routeLineRef.current) {
            routeLineRef.current.setLatLngs([
              [storeLocation.lat, storeLocation.lng],
              [curLat, curLng],
              [customerLocation.lat, customerLocation.lng],
            ]);
          }

          if (progress < 1.0) {
            requestAnimationFrame(animateGlide);
          } else {
            prevRiderPosRef.current = target;
          }
        };

        requestAnimationFrame(animateGlide);
      }
    }
  }, [isLeafletLoaded, currentRiderPos.lat, currentRiderPos.lng, storeLocation, customerLocation, isDelivered, isRiderOnline, speedKmh]);

  return (
    <div className="relative w-full rounded-2xl overflow-hidden border border-brand-border bg-slate-100 shadow-inner">
      {/* Map Canvas Container */}
      <div ref={mapContainerRef} className="w-full h-56 sm:h-64 z-0" />

      {/* Floating Status Bar at Top Left */}
      <div className="absolute top-2.5 left-2.5 z-10 flex flex-col gap-1.5">
        <div className="bg-white/95 backdrop-blur-md px-3 py-1 rounded-full border border-brand-border shadow-xs flex items-center gap-1.5 text-[11px] font-bold text-brand-text">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping" />
          <span>
            {streamMode === 'SSE' ? '⚡ Live Real-Time GPS (SSE)' : '📡 Polling Fallback Sync'}
          </span>
          <span className="text-brand-muted">•</span>
          <span className="text-brand-primary">{isDelivered ? 'Delivered' : `~${etaMinutes}m away`}</span>
        </div>

        {isStale && (
          <div className="bg-amber-50/95 border border-amber-300 text-amber-800 px-2.5 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1">
            <AlertCircle size={11} className="text-amber-600" />
            <span>GPS Signal Stale (&gt;90s) — displaying last known beacon</span>
          </div>
        )}

        {!isRiderOnline && (
          <div className="bg-rose-50/95 border border-rose-300 text-rose-800 px-2.5 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1">
            <WifiOff size={11} className="text-rose-600" />
            <span>Rider Offline / Reconnecting Telemetry</span>
          </div>
        )}
      </div>

      {/* Telemetry Indicator at Bottom Left */}
      <div className="absolute bottom-2.5 left-2.5 z-10 bg-white/90 backdrop-blur-xs px-2.5 py-1 rounded-lg border border-brand-border shadow-xs text-[10px] text-brand-text font-semibold flex items-center gap-2">
        <span className="flex items-center gap-1 text-brand-primary font-bold">
          <Zap size={11} fill="currentColor" /> {speedKmh.toFixed(1)} km/h
        </span>
        <span className="text-brand-border">|</span>
        <span className="text-brand-muted">{riderVehicle}</span>
      </div>

      {/* Map Legend Overlay at Bottom Right */}
      <div className="absolute bottom-2.5 right-2.5 z-10 bg-white/90 backdrop-blur-xs px-2.5 py-1 rounded-lg border border-brand-border shadow-xs text-[10px] text-brand-muted flex items-center gap-2">
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-[#176B45]" />
          <span>Store</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-brand-accent" />
          <span>Rider</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-red-500" />
          <span>Destination</span>
        </div>
      </div>

      {/* Fallback if Leaflet fails to load */}
      {loadError && (
        <div className="absolute inset-0 bg-slate-100/90 flex flex-col items-center justify-center p-4 text-center z-20">
          <AlertCircle size={22} className="text-amber-600 mb-1" />
          <p className="text-xs font-bold text-brand-text">OpenStreetMap Radar Engine Offline</p>
          <p className="text-[10px] text-brand-muted">Real-time GPS telemetry is active in text stream mode.</p>
        </div>
      )}
    </div>
  );
};

export default LiveDeliveryMap;
