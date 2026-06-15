import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, switchMap } from 'rxjs';

import { apiConfig } from '../api/api.config';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class AppBootstrapService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  run(): Observable<void> {
    return this.http
      .get(`${apiConfig.authUrl}/csrf`, { withCredentials: true, responseType: 'text' })
      .pipe(
        catchError(() => of(null)),
        switchMap(() => this.authService.validateSession()),
        map(() => void 0),
        catchError(() => of(void 0)),
      );
  }
}
