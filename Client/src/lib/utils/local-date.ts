/** Calendar date (YYYY-MM-DD) in the user's local timezone. */
export function localDateIso(date: Date = new Date()): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Calendar date (YYYY-MM-DD) from an ISO-8601 timestamp in the user's local timezone. */
export function localDateIsoFromTimestamp(isoTimestamp: string): string {
  return localDateIso(new Date(isoTimestamp));
}
