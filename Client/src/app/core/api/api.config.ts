const localBackendOrigin = 'http://localhost:8080';

export const backendOrigin = resolveBackendOrigin(window.location.hostname, window.location.origin);

export const apiConfig = {
  authUrl: `${backendOrigin}/api/auth`,
  accountUrl: `${backendOrigin}/api/account`,
  vinUrl: `${backendOrigin}/api/vin`,
} as const;

export function resolveBackendOrigin(hostname: string, origin: string): string {
  return isLocalHost(hostname) ? localBackendOrigin : origin;
}

function isLocalHost(hostname: string): boolean {
  return hostname === 'localhost' || hostname === '127.0.0.1';
}
