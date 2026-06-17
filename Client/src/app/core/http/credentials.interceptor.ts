import { HttpInterceptorFn } from '@angular/common/http';

import { isSameBackendOrigin } from '../api/api.config';

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isSameBackendOrigin(req.url)) {
    return next(req);
  }

  return next(req.clone({ withCredentials: true }));
};
