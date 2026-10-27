/**
 * Authoritative Server Configuration & Backend Resolver.
 * Resolves backend URL from environment with consistent priority:
 * 1. BACKEND_URL (standard server-side env)
 * 2. BACKEND_API_URL (alternative server-side env)
 * 3. NEXT_PUBLIC_API_URL (public client/edge env)
 * 4. http://localhost:8080 (local development default)
 */
export function getBackendUrl(): string {
  return (
    process.env.BACKEND_URL ||
    process.env.BACKEND_API_URL ||
    process.env.NEXT_PUBLIC_API_URL ||
    'http://localhost:8080'
  );
}
