import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { unauthorizedInterceptor } from './core/auth/unauthorized.interceptor';
import { AppBootstrapService } from './core/bootstrap/app-bootstrap.service';
import { credentialsInterceptor } from './core/http/credentials.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN',
      }),
      withInterceptors([credentialsInterceptor, unauthorizedInterceptor]),
    ),
    provideAppInitializer(() => {
      const bootstrap = inject(AppBootstrapService);
      return firstValueFrom(bootstrap.run());
    }),
  ],
};
