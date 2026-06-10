export const backendOrigin = resolveBackendOrigin(window.location.origin);

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
