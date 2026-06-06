import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { firstValueFrom, isObservable, of } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from './auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  async function runGuard(authService: Partial<AuthService>) {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    });

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    return isObservable(result) ? firstValueFrom(result) : result;
  }

  it('allows activation after session validation succeeds', async () => {
    const authService = {
      validateSession: vi.fn().mockReturnValue(of(true)),
    };

    await expect(runGuard(authService)).resolves.toBe(true);
    expect(authService.validateSession).toHaveBeenCalled();
  });

  it('redirects to sign in when session validation fails', async () => {
    const authService = {
      validateSession: vi.fn().mockReturnValue(of(false)),
    };

    const result = await runGuard(authService);

    expect(authService.validateSession).toHaveBeenCalled();
    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/sign-in');
  });
});
