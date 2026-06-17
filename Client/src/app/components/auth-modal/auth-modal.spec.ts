import { of, throwError } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';

import { AuthModalComponent } from './auth-modal';
import { AuthService } from '../../core/auth/auth.service';

describe('AuthModalComponent', () => {
  function configure(
    authService = {
      register: vi.fn(),
      login: vi.fn(),
      completeEmailVerificationSignIn: vi.fn(),
    },
  ) {
    TestBed.configureTestingModule({
      imports: [AuthModalComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    });

    return authService;
  }

  it('blocks invalid signup submissions and displays validation messages', () => {
    const authService = configure();
    const fixture = TestBed.createComponent(AuthModalComponent);
    fixture.componentRef.setInput('mode', 'sign-up');
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.register).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('First name is required.');
    expect(fixture.nativeElement.textContent).toContain('Last name is required.');
    expect(fixture.nativeElement.textContent).toContain('Email is required.');
    expect(fixture.nativeElement.textContent).toContain('Password is required.');
  });

  it('submits a valid signup request and disables the submit button while loading', () => {
    const authService = configure();
    authService.register.mockReturnValue(of({ token: 'jwt-token' }));

    const fixture = TestBed.createComponent(AuthModalComponent);
    fixture.componentRef.setInput('mode', 'sign-up');
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'firstname', ' Pat ');
    setInputValue(fixture.nativeElement, 'lastname', ' Driver ');
    setInputValue(fixture.nativeElement, 'email', ' pat@example.com ');
    setInputValue(fixture.nativeElement, 'password', 'Password1!');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));

    expect(authService.register).toHaveBeenCalledWith({
      firstname: 'Pat',
      lastname: 'Driver',
      email: 'pat@example.com',
      password: 'Password1!',
    });
  });

  it('renders backend errors returned from login', () => {
    const authService = configure();
    authService.login.mockReturnValue(
      throwError(() => ({ message: 'Invalid account credentials', fieldMessages: [] })),
    );

    const fixture = TestBed.createComponent(AuthModalComponent);
    fixture.componentRef.setInput('mode', 'sign-in');
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'email', 'pat@example.com');
    setInputValue(fixture.nativeElement, 'password', 'wrong');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.login).toHaveBeenCalledWith({ email: 'pat@example.com', password: 'wrong' });
    expect(fixture.nativeElement.textContent).toContain('Invalid account credentials');
  });

  it('resets server errors and verification step when routed mode changes', () => {
    const authService = configure();
    authService.login.mockReturnValue(
      throwError(() => ({ message: 'Invalid account credentials', fieldMessages: [] })),
    );

    const fixture = TestBed.createComponent(AuthModalComponent);
    fixture.componentRef.setInput('mode', 'sign-in');
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'email', 'pat@example.com');
    setInputValue(fixture.nativeElement, 'password', 'wrong');
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit'),
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Invalid account credentials');

    fixture.componentRef.setInput('mode', 'sign-up');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Invalid account credentials');
    expect(fixture.componentInstance['step']()).toBe('credentials');
  });

  it('switches to code entry when sign-in requires verification', () => {
    const authService = configure();
    authService.login.mockReturnValue(
      of({
        verificationRequired: true,
        verificationChallenge: 'challenge-token',
      }),
    );

    const fixture = TestBed.createComponent(AuthModalComponent);
    fixture.componentRef.setInput('mode', 'sign-in');
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'email', 'pat@example.com');
    setInputValue(fixture.nativeElement, 'password', 'Password1!');
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit'),
    );
    fixture.detectChanges();

    expect(fixture.componentInstance['step']()).toBe('verify-code');
    expect(fixture.nativeElement.textContent).toContain('Verify your email');
    expect(fixture.nativeElement.querySelector('[formControlName="code"]')).not.toBeNull();
    expect(authService.login).toHaveBeenCalled();
  });

  it('completes sign-in verification and closes the modal', () => {
    const authService = configure();
    authService.login.mockReturnValue(
      of({
        verificationRequired: true,
        verificationChallenge: 'challenge-token',
      }),
    );
    authService.completeEmailVerificationSignIn.mockReturnValue(
      of({ token: 'session-token', emailVerified: true }),
    );

    const fixture = TestBed.createComponent(AuthModalComponent);
    const close = vi.fn();
    fixture.componentRef.setInput('mode', 'sign-in');
    fixture.componentInstance.close.subscribe(close);
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'email', 'pat@example.com');
    setInputValue(fixture.nativeElement, 'password', 'Password1!');
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit'),
    );
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'code', '123456');
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit'),
    );
    fixture.detectChanges();

    expect(authService.completeEmailVerificationSignIn).toHaveBeenCalledWith(
      'challenge-token',
      '123456',
    );
    expect(close).toHaveBeenCalled();
  });

  it('keeps route links for landing page mode switching by default', () => {
    configure();
    const fixture = TestBed.createComponent(AuthModalComponent);
    fixture.componentRef.setInput('mode', 'sign-in');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.hc-dialog-switch a')).not.toBeNull();
  });

  it('emits mode changes when routing links are disabled', () => {
    configure();
    const fixture = TestBed.createComponent(AuthModalComponent);
    const modeChanges: string[] = [];
    fixture.componentRef.setInput('mode', 'sign-in');
    fixture.componentRef.setInput('useRoutingLinks', false);
    fixture.componentInstance.modeChange.subscribe((mode) => modeChanges.push(mode));
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.hc-dialog-switch-button') as HTMLButtonElement).click();

    expect(modeChanges).toEqual(['sign-up']);
  });
});

function setInputValue(host: HTMLElement, controlName: string, value: string): void {
  const input = host.querySelector(`[formControlName="${controlName}"]`) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}
