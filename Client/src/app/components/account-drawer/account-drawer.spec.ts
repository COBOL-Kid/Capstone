import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AccountDrawerComponent } from './account-drawer';
import { AuthService } from '../../core/auth/auth.service';
import { AuthModalComponent } from '../auth-modal/auth-modal';

const accountDetails = {
  userId: 1,
  email: 'pat@example.com',
  firstName: 'Pat',
  lastName: 'Driver',
  userSms: null,
  createdAt: '2026-05-07T17:47:00Z',
  updatedAt: '2026-05-07T17:48:00Z',
};

function createAuthServiceStub(isSignedIn = false) {
  return {
    isSignedIn: vi.fn().mockReturnValue(isSignedIn),
    getCurrentAccount: vi.fn().mockReturnValue(of(accountDetails)),
    validateSession: vi.fn().mockReturnValue(of(isSignedIn)),
    updateCurrentAccount: vi.fn().mockReturnValue(of(accountDetails)),
    changePassword: vi.fn().mockReturnValue(of(null)),
    logout: vi.fn().mockReturnValue(of(undefined)),
    refresh: vi.fn().mockReturnValue(of({ token: 'refreshed-token' })),
    clearSession: vi.fn(),
  };
}

function openPasswordModal(fixture: ComponentFixture<AccountDrawerComponent>): void {
  const changePasswordButton = Array.from(
    fixture.nativeElement.querySelectorAll(
      '.hc-btn.hc-btn--secondary',
    ) as NodeListOf<HTMLButtonElement>,
  ).find((button) => button.textContent?.includes('Change Password'));
  changePasswordButton?.click();
  fixture.detectChanges();
}

function submitPasswordModal(
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
  let router: Router;

  beforeEach(async () => {
    authService = createAuthServiceStub();

    await TestBed.configureTestingModule({
      imports: [AccountDrawerComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
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

    const drawer = fixture.debugElement.query(By.css('.account-drawer'));
    expect(component.isOpen()).toBe(true);
    expect(component.isClosing()).toBe(true);
    expect(drawer.classes['account-drawer--closing']).toBe(true);

    vi.advanceTimersByTime(240);
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
    expect(component.isClosing()).toBe(false);
    vi.useRealTimers();
  });

  it('closes when Escape is pressed', () => {
    vi.useFakeTimers();
    component.open();
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(component.isClosing()).toBe(true);

    vi.advanceTimersByTime(240);
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
    vi.useRealTimers();
  });

  it('does not close the drawer when Escape is pressed while the password modal is open', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    component.openPasswordModal();
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(component.isOpen()).toBe(true);
    expect(component.isPasswordModalOpen()).toBe(false);
  });

  it('renders signed-out account actions', () => {
    component.open();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Sign in to view your account details.');
    expect(text).toContain('Sign In');
    expect(text).toContain('Sign Up');
    expect(authService.getCurrentAccount).not.toHaveBeenCalled();
  });

  it('renders loaded account details for signed-in users', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(authService.getCurrentAccount).toHaveBeenCalled();
    expect(text).toContain('Pat Driver');
    expect(text).toContain('pat@example.com');
    expect(text).toContain('Not provided');
    expect(text).toContain('Log out');
    expect(text).toContain('Change Password');
  });

  it('renders profile edit controls prefilled from the loaded account', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    fixture.detectChanges();
    const editButton = Array.from(
      fixture.nativeElement.querySelectorAll(
        '.hc-btn.hc-btn--secondary',
      ) as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Edit Contact Info'));
    editButton?.click();
    fixture.detectChanges();

    const emailInput = fixture.debugElement.query(By.css('#account-email'))
      .nativeElement as HTMLInputElement;
    const smsInput = fixture.debugElement.query(By.css('#account-sms'))
      .nativeElement as HTMLInputElement;
    expect(emailInput.value).toBe('pat@example.com');
    expect(smsInput.value).toBe('');
    expect(fixture.nativeElement.textContent).toContain('Save Changes');
  });

  it('saves profile edits and refreshes the token when the email changes', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.updateCurrentAccount.mockReturnValue(
      of({ ...accountDetails, email: 'new@example.com', userSms: '+15551234567' }),
    );

    component.open();
    component.startProfileEdit();
    component.profileForm.setValue({ email: ' new@example.com ', userSms: ' +15551234567 ' });
    component.submitProfile();
    fixture.detectChanges();

    expect(authService.updateCurrentAccount).toHaveBeenCalledWith({
      firstName: 'Pat',
      lastName: 'Driver',
      email: 'new@example.com',
      userSms: '+15551234567',
    });
    expect(authService.refresh).toHaveBeenCalled();
    expect(component.account()?.email).toBe('new@example.com');
    expect(fixture.nativeElement.textContent).toContain('Account details updated.');
  });

  it('keeps the profile save successful when refreshing after an email change fails', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.updateCurrentAccount.mockReturnValue(
      of({ ...accountDetails, email: 'new@example.com', userSms: '+15551234567' }),
    );
    authService.refresh.mockReturnValue(
      throwError(() => ({ message: 'Could not refresh session', fieldMessages: [] })),
    );

    component.open();
    component.startProfileEdit();
    component.profileForm.setValue({ email: 'new@example.com', userSms: '+15551234567' });
    component.submitProfile();
    fixture.detectChanges();

    expect(authService.refresh).toHaveBeenCalled();
    expect(component.account()?.email).toBe('new@example.com');
    expect(component.profileServerError()).toBeNull();
    expect(component.isEditingProfile()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Account details updated.');
  });

  it('sends null when the saved phone number is cleared', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.getCurrentAccount.mockReturnValue(
      of({ ...accountDetails, userSms: '+15551234567' }),
    );

    component.open();
    component.startProfileEdit();
    component.profileForm.setValue({ email: 'pat@example.com', userSms: '   ' });
    component.submitProfile();

    expect(authService.updateCurrentAccount).toHaveBeenCalledWith({
      firstName: 'Pat',
      lastName: 'Driver',
      email: 'pat@example.com',
      userSms: null,
    });
    expect(authService.refresh).not.toHaveBeenCalled();
  });

  it('renders duplicate email errors from profile saves', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.updateCurrentAccount.mockReturnValue(
      throwError(() => ({ message: 'Email is already in use', fieldMessages: [] })),
    );

    component.open();
    component.startProfileEdit();
    component.profileForm.setValue({ email: 'taken@example.com', userSms: '' });
    component.submitProfile();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Email is already in use');
    expect(component.isEditingProfile()).toBe(true);
  });

  it('marks invalid profile fields touched and avoids profile API calls', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    component.startProfileEdit();
    component.profileForm.setValue({ email: 'not-an-email', userSms: 'abc<script>' });
    component.submitProfile();
    fixture.detectChanges();

    expect(authService.updateCurrentAccount).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Enter a valid email address.');
    expect(fixture.nativeElement.textContent).toContain(
      'Use a phone number with digits and common phone characters.',
    );
  });

  it('changes the password through the modal and shows a success message', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    fixture.detectChanges();

    openPasswordModal(fixture);
    submitPasswordModal(fixture, {
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
      confirmPassword: 'NewSecret1!',
    });

    expect(authService.changePassword).toHaveBeenCalledWith({
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
    });
    expect(component.isPasswordModalOpen()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Password updated.');
  });

  it('shows password requirements at the top of the change password modal', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    fixture.detectChanges();
    openPasswordModal(fixture);

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Password requirements');
    expect(text).toContain('At least 8 characters, at most 72');
    expect(text).toContain('Uppercase, lowercase, number, and special character');
  });

  it('renders invalid current password errors in the modal without closing the drawer', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.changePassword.mockReturnValue(
      throwError(() => ({ message: 'Current password is incorrect', fieldMessages: [] })),
    );

    component.open();
    fixture.detectChanges();

    openPasswordModal(fixture);
    submitPasswordModal(fixture, {
      currentPassword: 'WrongSecret1!',
      newPassword: 'NewSecret1!',
      confirmPassword: 'NewSecret1!',
    });

    expect(fixture.nativeElement.textContent).toContain('Current password is incorrect');
    expect(component.isOpen()).toBe(true);
    expect(component.isPasswordModalOpen()).toBe(true);
  });

  it('closes the password modal when the drawer closes', () => {
    vi.useFakeTimers();
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    component.openPasswordModal();
    fixture.detectChanges();

    component.close();
    vi.advanceTimersByTime(240);
    fixture.detectChanges();

    expect(component.isPasswordModalOpen()).toBe(false);
    vi.useRealTimers();
  });

  it('prevents password changes when confirmation does not match', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    fixture.detectChanges();

    openPasswordModal(fixture);
    submitPasswordModal(fixture, {
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
      confirmPassword: 'Different1!',
    });

    expect(authService.changePassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Passwords must match.');
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
    expect(component.status()).toBe('signed-out');

    vi.advanceTimersByTime(240);
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
    vi.useRealTimers();
  });

  it('clears the session when logout fails and still navigates home', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.logout.mockReturnValue(
      throwError(() => ({ message: 'Logout failed', fieldMessages: [] })),
    );

    component.open();
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.hc-btn.hc-btn--logout')).nativeElement.click();
    fixture.detectChanges();

    expect(authService.logout).toHaveBeenCalled();
    expect(authService.clearSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
    expect(component.status()).toBe('signed-out');
  });

  it('shows signed-out actions and clears the session when account lookup is unauthorized', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.getCurrentAccount.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' })),
    );

    component.open();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(authService.clearSession).toHaveBeenCalled();
    expect(text).toContain('Your session has expired.');
    expect(text).toContain('Sign In');
    expect(text).toContain('Sign Up');
  });

  it('shows an error message when account lookup fails', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.getCurrentAccount.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Server Error' })),
    );

    component.open();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Unable to load account details. Please try again.',
    );
  });

  it('opens the existing auth modal in sign-in mode', () => {
    component.open();
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.hc-btn.hc-btn--primary-inverse')).nativeElement.click();
    fixture.detectChanges();

    const modal = fixture.debugElement.query(By.directive(AuthModalComponent));
    expect(modal.componentInstance.mode()).toBe('sign-in');
  });

  it('opens the existing auth modal in sign-up mode', () => {
    component.open();
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.hc-btn.hc-btn--secondary')).nativeElement.click();
    fixture.detectChanges();

    const modal = fixture.debugElement.query(By.directive(AuthModalComponent));
    expect(modal.componentInstance.mode()).toBe('sign-up');
  });

  it('refreshes account details after the auth modal closes', () => {
    authService.isSignedIn.mockReturnValueOnce(false).mockReturnValue(true);
    authService.validateSession.mockReturnValue(of(false));

    component.open();
    component.openAuthModal('sign-in');
    fixture.detectChanges();

    component.closeAuthModal();
    fixture.detectChanges();

    expect(authService.validateSession).toHaveBeenCalled();
    expect(authService.getCurrentAccount).toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('navigates to home after the auth modal closes with a valid session', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.validateSession.mockReturnValue(of(true));

    component.open();
    component.openAuthModal('sign-in');
    fixture.detectChanges();

    component.closeAuthModal();
    fixture.detectChanges();

    expect(authService.validateSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/home']);
    expect(authService.getCurrentAccount).toHaveBeenCalled();
  });
});
