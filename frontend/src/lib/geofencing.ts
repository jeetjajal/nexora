/**
 * NEXORA 2.0 GEOLOCATION & DARK STORE GEOFENCING UTILITY
 * Implements high-precision Haversine distance calculations and
 * dynamic dark-store dispatch determination across Ahmedabad service zones.
 */

export interface DarkStoreInfo {
  id: number;
  code: string;
  name: string;
  area: string;
  lat: number;
  lng: number;
  radiusKm: number;
  isOpen: boolean;
  baseEtaMinutes: number;
}

export const AHMEDABAD_DARK_STORES: DarkStoreInfo[] = [
  {
    id: 4,
    code: 'DS-AHM-04',
    name: 'Nexora Dark Store SG Highway #04',
    area: 'Bodakdev / SG Highway',
    lat: 23.0373,
    lng: 72.5118,
    radiusKm: 4.0,
    isOpen: true,
    baseEtaMinutes: 7,
  },
  {
    id: 2,
    code: 'DS-AHM-02',
    name: 'Nexora Dark Store Vastrapur #02',
    area: 'Vastrapur / Satellite',
    lat: 23.0350,
    lng: 72.5293,
    radiusKm: 3.5,
    isOpen: true,
    baseEtaMinutes: 8,
  },
  {
    id: 1,
    code: 'DS-AHM-01',
    name: 'Nexora Dark Store Prahlad Nagar #01',
    area: 'Prahlad Nagar / Anand Nagar',
    lat: 23.0125,
    lng: 72.5075,
    radiusKm: 3.5,
    isOpen: true,
    baseEtaMinutes: 8,
  },
  {
    id: 3,
    code: 'DS-AHM-03',
    name: 'Nexora Dark Store Bopal #03',
    area: 'South Bopal / Shela',
    lat: 23.0340,
    lng: 72.4630,
    radiusKm: 4.5,
    isOpen: true,
    baseEtaMinutes: 9,
  },
];

/**
 * Calculates great-circle distance between two points in kilometers using the Haversine formula
 */
export function calculateHaversineDistanceKm(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371; // Earth radius in kilometers
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Number((R * c).toFixed(2));
}

export interface GeofenceResult {
  isServiceable: boolean;
  selectedStore: DarkStoreInfo | null;
  distanceKm: number;
  estimatedDeliveryMinutes: number;
  availableStoresInZone: number;
  message: string;
}

/**
 * Determines the optimal, closest open Dark Store within the delivery radius
 */
export function resolveServiceableDarkStore(
  userLat: number,
  userLng: number
): GeofenceResult {
  let closestStore: DarkStoreInfo | null = null;
  let minDistance = Infinity;

  // Filter open stores within radius
  const eligibleStores: { store: DarkStoreInfo; dist: number }[] = [];

  for (const store of AHMEDABAD_DARK_STORES) {
    if (!store.isOpen) continue;
    const dist = calculateHaversineDistanceKm(userLat, userLng, store.lat, store.lng);

    if (dist <= store.radiusKm) {
      eligibleStores.push({ store, dist });
    }

    if (dist < minDistance) {
      minDistance = dist;
      closestStore = store;
    }
  }

  // Sort eligible stores by distance
  eligibleStores.sort((a, b) => a.dist - b.dist);

  if (eligibleStores.length > 0) {
    const winner = eligibleStores[0];
    const eta = Math.min(15, Math.max(7, Math.round(winner.store.baseEtaMinutes + winner.dist * 1.8)));

    return {
      isServiceable: true,
      selectedStore: winner.store,
      distanceKm: winner.dist,
      estimatedDeliveryMinutes: eta,
      availableStoresInZone: eligibleStores.length,
      message: `Delivering from ${winner.store.name} in ~${eta} mins (${winner.dist} km away)`,
    };
  }

  // Outside 4.5km geofence radius
  return {
    isServiceable: false,
    selectedStore: closestStore,
    distanceKm: minDistance,
    estimatedDeliveryMinutes: 0,
    availableStoresInZone: 0,
    message: `Location is ${minDistance} km away from nearest dark store. Delivery is currently limited to 4.5 km within Ahmedabad.`,
  };
}

/**
 * Fallback Geocoder for Ahmedabad neighborhoods
 */
export function resolveAddressToCoordinates(addressText: string): { lat: number; lng: number } {
  const lower = addressText.toLowerCase();

  if (lower.includes('bopal') || lower.includes('shela') || lower.includes('380058')) {
    return { lat: 23.0340, lng: 72.4630 };
  }
  if (lower.includes('vastrapur') || lower.includes('satellite') || lower.includes('380015')) {
    return { lat: 23.0350, lng: 72.5293 };
  }
  if (lower.includes('prahlad') || lower.includes('vejalpur') || lower.includes('380051')) {
    return { lat: 23.0125, lng: 72.5075 };
  }
  if (lower.includes('navrangpura') || lower.includes('ashram') || lower.includes('380009')) {
    return { lat: 23.0360, lng: 72.5590 };
  }
  // Default to SG Highway Bodakdev (Heart of Quick Commerce Hub)
  return { lat: 23.0373, lng: 72.5118 };
}
