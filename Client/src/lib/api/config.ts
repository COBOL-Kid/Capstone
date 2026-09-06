export const backendOrigin =
  typeof window === 'undefined' ? '' : resolveBackendOrigin(window.location.origin);

export const apiConfig = {
  authUrl: `${backendOrigin}/api/auth`,
  accountUrl: `${backendOrigin}/api/account`,
  vinUrl: `${backendOrigin}/api/vin`,
  maintenanceUrl: `${backendOrigin}/api/maintenance`,
  recallUrl: `${backendOrigin}/api/recall`,
} as const;

export function resolveBackendOrigin(origin: string): string {
  return origin;
}

export function isSameBackendOrigin(url: string): boolean {
  try {
    return new URL(url, backendOrigin || 'http://localhost').origin === backendOrigin;
  } catch {
    return false;
  }
}
