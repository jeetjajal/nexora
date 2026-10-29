/**
 * Authoritative Server Configuration & Backend Resolver.
 * Resolves backend URL from environment with consistent priority:
 * 1. BACKEND_URL (standard server-side env)
 * 2. BACKEND_API_URL (alternative server-side env)
 * 3. NEXT_PUBLIC_BACKEND_URL (public client/edge env)
 * 4. NEXT_PUBLIC_API_URL (public client/edge env)
 * 5. Production Render backend: https://nexora-backend-fw14.onrender.com (in production/Vercel)
 * 6. http://localhost:8080 (local development default)
 */
export function getBackendUrl(): string {
  if (process.env.BACKEND_URL) return process.env.BACKEND_URL;
  if (process.env.BACKEND_API_URL) return process.env.BACKEND_API_URL;
  if (process.env.NEXT_PUBLIC_BACKEND_URL) return process.env.NEXT_PUBLIC_BACKEND_URL;
  if (process.env.NEXT_PUBLIC_API_URL) return process.env.NEXT_PUBLIC_API_URL;
  if (process.env.NODE_ENV === 'production' || process.env.VERCEL) {
    return 'https://nexora-backend-fw14.onrender.com';
  }
  return 'http://localhost:8080';
}
