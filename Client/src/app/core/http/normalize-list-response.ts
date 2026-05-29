import { map, Observable } from 'rxjs';

/** Maps null/undefined bodies (e.g. 204 No Content) to an empty array for list GETs. */
export function normalizeListResponse<T>(
  source: Observable<T[] | null | undefined>,
): Observable<T[]> {
  return source.pipe(map((items) => items ?? []));
}
