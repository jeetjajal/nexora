'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Navigation, AlertCircle, RefreshCw, Layers } from 'lucide-react';

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
  lastPingTime?: string | Date;
  etaMinutes?: number;
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
  lastPingTime,
  etaMinutes = 8,
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<any>(null);
  const riderMarkerRef = useRef<any>(null);
  const routeLineRef = useRef<any>(null);

  const [isLeafletLoaded, setIsLeafletLoaded] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Compute resolved rider position (interpolated between store and customer if not explicitly provided)
  const currentRiderPos: LocationCoordinates = isDelivered
    ? customerLocation
    : riderLocation || {
        lat: storeLocation.lat + (customerLocation.lat - storeLocation.lat) * 0.65,
        lng: storeLocation.lng + (customerLocation.lng - storeLocation.lng) * 0.65,
      };

  // Check if location is stale (older than 2 minutes)
  const isStale = lastPingTime
    ? Date.now() - new Date(lastPingTime).getTime() > 120000
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

  // 2. Initialize and update Leaflet Map
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
        html: `<div style="background:#0c831f; color:#fff; width:30px; height:30px; border-radius:50%; display:flex; align-items:center; justify-content:center; border:2.5px solid #fff; box-shadow:0 2px 6px rgba(0,0,0,0.3); font-size:14px;">🏬</div>`,
        iconSize: [30, 30],
        iconAnchor: [15, 15],
      });

      const customerIcon = L.divIcon({
        className: 'custom-map-icon',
        html: `<div style="background:#d92d20; color:#fff; width:30px; height:30px; border-radius:50%; display:flex; align-items:center; justify-content:center; border:2.5px solid #fff; box-shadow:0 2px 6px rgba(0,0,0,0.3); font-size:14px;">🏠</div>`,
        iconSize: [30, 30],
        iconAnchor: [15, 15],
      });

      const riderIcon = L.divIcon({
        className: 'custom-map-icon',
        html: `<div style="background:#fbc02d; color:#1e3a1f; width:34px; height:34px; border-radius:50%; display:flex; align-items:center; justify-content:center; border:2.5px solid #0c831f; box-shadow:0 3px 8px rgba(0,0,0,0.35); font-size:18px; transform: scale(1.08);">🛵</div>`,
        iconSize: [34, 34],
        iconAnchor: [17, 17],
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
        .bindPopup(`<b>${riderName}</b> (${riderVehicle})<br/>${isDelivered ? 'Delivered' : 'In Transit'}`);

      riderMarkerRef.current = riderMarker;

      // Draw Polyline Route
      const routePoints = [
        [storeLocation.lat, storeLocation.lng],
        [currentRiderPos.lat, currentRiderPos.lng],
        [customerLocation.lat, customerLocation.lng],
      ];

      const polyline = L.polyline(routePoints, {
        color: '#0c831f',
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
      // Update rider marker position
      if (riderMarkerRef.current) {
        riderMarkerRef.current.setLatLng([currentRiderPos.lat, currentRiderPos.lng]);
      }
      if (routeLineRef.current) {
        routeLineRef.current.setLatLngs([
          [storeLocation.lat, storeLocation.lng],
          [currentRiderPos.lat, currentRiderPos.lng],
          [customerLocation.lat, customerLocation.lng],
        ]);
      }
    }
  }, [isLeafletLoaded, currentRiderPos.lat, currentRiderPos.lng, storeLocation, customerLocation, isDelivered]);

  return (
    <div className="relative w-full rounded-2xl overflow-hidden border border-brand-border bg-slate-100 shadow-inner">
      {/* Map Canvas Container */}
      <div ref={mapContainerRef} className="w-full h-52 sm:h-64 z-0" />

      {/* Floating Status Pill */}
      <div className="absolute top-2.5 left-2.5 z-10 flex flex-col gap-1.5">
        <div className="bg-white/95 backdrop-blur-md px-2.5 py-1 rounded-full border border-brand-border shadow-xs flex items-center gap-1.5 text-[11px] font-bold text-brand-text">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping" />
          <span>Live GPS Telemetry</span>
          <span className="text-brand-muted">•</span>
          <span className="text-brand-primary">{isDelivered ? 'Arrived' : `~${etaMinutes} mins away`}</span>
        </div>

        {isStale && (
          <div className="bg-amber-50/95 border border-amber-300 text-amber-800 px-2 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1">
            <AlertCircle size={10} className="text-amber-600" />
            <span>Weak GPS Signal (showing last route)</span>
          </div>
        )}
      </div>

      {/* Map Legend Overlay at Bottom Right */}
      <div className="absolute bottom-2.5 right-2.5 z-10 bg-white/90 backdrop-blur-xs px-2 py-1 rounded-lg border border-brand-border shadow-xs text-[10px] text-brand-muted flex items-center gap-2">
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-emerald-600" />
          <span>Store</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-amber-500" />
          <span>Rider</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-red-500" />
          <span>You</span>
        </div>
      </div>

      {/* Fallback if Leaflet fails to load */}
      {loadError && (
        <div className="absolute inset-0 bg-slate-100/90 flex flex-col items-center justify-center p-4 text-center z-20">
          <AlertCircle size={22} className="text-amber-600 mb-1" />
          <p className="text-xs font-bold text-brand-text">Road Map Offline</p>
          <p className="text-[10px] text-brand-muted">Real-time coordinates are being tracked in radar mode.</p>
        </div>
      )}
    </div>
  );
};

export default LiveDeliveryMap;
