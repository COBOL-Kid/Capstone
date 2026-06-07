import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AccountDrawerComponent } from './account-drawer';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/toast/toast.service';
import { AuthModalComponent } from '../auth-modal/auth-modal';

const accountDetails = {
  userId: 1,
  email: 'pat@example.com',
  firstName: 'Pat',
  lastName: 'Driver',
  userSms: null,
  emailVerified: true,
  emailVerifiedAt: '2026-01-02T03:04:00Z',
  createdAt: '2026-05-07T17:47:00Z',
  updatedAt: '2026-05-07T17:48:00Z',
};

function createAuthServiceStub(isSignedIn = false) {
  return {
    account: signal(isSignedIn ? accountDetails : null),
    isSignedIn: vi.fn().mockReturnValue(isSignedIn),
    getCurrentAccount: vi.fn().mockReturnValue(of(accountDetails)),
    validateSession: vi.fn().mockReturnValue(of(isSignedIn)),
    initiateAccountChange: vi
      .fn()
      .mockReturnValue(of({ changeType: 'PASSWORD', expiresInMinutes: 5 })),
    verifyAccountChange: vi.fn().mockReturnValue(of({ account: accountDetails })),
    resendAccountChangeCode: vi
      .fn()
      .mockReturnValue(of({ changeType: 'PASSWORD', expiresInMinutes: 5 })),
    getPendingAccountChange: vi.fn().mockReturnValue(of(null)),
    resendEmailVerification: vi.fn().mockReturnValue(of({ emailVerified: false })),
    logout: vi.fn().mockReturnValue(of(undefined)),
    clearSession: vi.fn(),
  };
}

function openChangeModal(fixture: ComponentFixture<AccountDrawerComponent>, label: string): void {
  const button = Array.from(
    fixture.nativeElement.querySelectorAll(
      '.account-drawer__actions .hc-btn',
    ) as NodeListOf<HTMLButtonElement>,
  ).find((item) => item.textContent?.includes(label));
  button?.click();
  fixture.detectChanges();
}

function submitPasswordDetailsStep(
  fixture: ComponentFixture<AccountDrawerComponent>,
  passwords: { currentPassword: string; newPassword: string; confirmPassword: string },
): void {
  const modal = fixture.nativeElement.querySelector('app-change-password-modal');
  const inputs = modal?.querySelectorAll('input[type="password"]') as NodeListOf<HTMLInputElement>;
  inputs[0].value = passwords.currentPassword;
  inputs[0].dispatchEvent(new Event('input'));
  inputs[1].value = passwords.newPassword;
  inputs[1].dispatchEvent(new Event('input'));
  inputs[2].value = passwords.confirmPassword;
  inputs[2].dispatchEvent(new Event('input'));
  fixture.detectChanges();

  const submitButton = modal?.querySelector('button[type="submit"]') as HTMLButtonElement;
  submitButton.click();
  fixture.detectChanges();
}

describe('AccountDrawerComponent', () => {
  let fixture: ComponentFixture<AccountDrawerComponent>;
  let component: AccountDrawerComponent;
  let authService: ReturnType<typeof createAuthServiceStub>;
  let toastService: { success: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    authService = createAuthServiceStub();
    toastService = { success: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [AccountDrawerComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: ToastService, useValue: toastService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountDrawerComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('opens the account drawer', () => {
    component.open();
    fixture.detectChanges();

    const drawer = fixture.debugElement.query(By.css('.account-drawer'));
    expect(drawer.classes['account-drawer--open']).toBe(true);
    expect(drawer.attributes['aria-hidden']).toBe('false');
  });

  it('plays the close transition when the close button is clicked', () => {
    vi.useFakeTimers();
    component.open();
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.account-drawer__close')).nativeElement.click();
    fixture.detectChanges();

    expect(component.isOpen()).toBe(true);
    expect(component.isClosing()).toBe(true);

    vi.advanceTimersByTime(240);
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
    vi.useRealTimers();
  });

  it('does not close the drawer when Escape is pressed while a change modal is open', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    openChangeModal(fixture, 'Change Password');
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(component.isOpen()).toBe(true);
    expect(component.activeChangeModal()).toBe(null);
  });

  it('renders signed-out account actions', () => {
    component.open();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Sign in to view your account details.');
    expect(authService.getCurrentAccount).not.toHaveBeenCalled();
  });

  it('renders loaded account details and change actions for signed-in users', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(authService.getCurrentAccount).toHaveBeenCalled();
    expect(text).toContain('Pat Driver');
    expect(text).toContain('pat@example.com');
    expect(text).toContain('Change Email');
    expect(text).toContain('Change SMS');
    expect(text).toContain('Change Password');
  });

  it('initiates password change and completes verification through the modal', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    fixture.detectChanges();
    openChangeModal(fixture, 'Change Password');
    submitPasswordDetailsStep(fixture, {
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
      confirmPassword: 'NewSecret1!',
    });

    expect(authService.initiateAccountChange).toHaveBeenCalledWith({
      changeType: 'PASSWORD',
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
    });

    const codeInput = fixture.debugElement.query(
      By.css('app-change-password-modal app-email-verification-step input'),
    );
    codeInput.nativeElement.value = '123456';
    codeInput.nativeElement.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    fixture.debugElement
      .query(By.css('app-change-password-modal app-email-verification-step button[type="submit"]'))
      .nativeElement.click();
    fixture.detectChanges();

    expect(authService.verifyAccountChange).toHaveBeenCalledWith('123456');
    expect(toastService.success).toHaveBeenCalledWith('Password updated.');
  });

  it('resumes a pending password change on the verification step', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.getPendingAccountChange.mockReturnValue(
      of({ changeType: 'PASSWORD', expiresInMinutes: 5 }),
    );

    component.open();
    fixture.detectChanges();

    expect(component.activeChangeModal()).toBe('PASSWORD');
    expect(component.resumeChangeOnVerifyStep()).toBe(true);
    expect(fixture.nativeElement.querySelector('app-change-password-modal')).not.toBeNull();
  });

  it('shows registration verification warning for unverified users', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.getCurrentAccount.mockReturnValue(of({ ...accountDetails, emailVerified: false }));

    component.open();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Verify your email to add vehicles.');
  });

  it('logs out, clears the session, navigates home, and closes the drawer', () => {
    vi.useFakeTimers();
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.hc-btn.hc-btn--logout')).nativeElement.click();
    fixture.detectChanges();

    expect(authService.logout).toHaveBeenCalled();
    expect(authService.clearSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);

    vi.advanceTimersByTime(240);
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
    vi.useRealTimers();
  });

  it('shows signed-out actions and clears the session when account lookup is unauthorized', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.getCurrentAccount.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' })),
    );

    component.open();
    fixture.detectChanges();

    expect(authService.clearSession).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Your session has expired.');
  });

  it('opens the existing auth modal in sign-in mode', () => {
    component.open();
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.hc-btn.hc-btn--primary-inverse')).nativeElement.click();
    fixture.detectChanges();

    const modal = fixture.debugElement.query(By.directive(AuthModalComponent));
    expect(modal.componentInstance.mode()).toBe('sign-in');
  });
});
