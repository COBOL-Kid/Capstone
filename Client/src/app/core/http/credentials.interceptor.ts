import { HttpInterceptorFn } from '@angular/common/http';

import { backendOrigin } from '../api/api.config';

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(backendOrigin)) {
    return next(req);
  }

  return next(req.clone({ withCredentials: true }));
};
