import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { isSameBackendOrigin } from '../api/api.config';
import { AuthService } from './auth.service';

export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        isSameBackendOrigin(req.url) &&
        authService.isSignedIn() &&
        !req.url.includes('/api/auth/authenticate') &&
        !req.url.includes('/api/auth/register')
      ) {
        authService.clearSession();
      }

      return throwError(() => error);
    }),
  );
};
