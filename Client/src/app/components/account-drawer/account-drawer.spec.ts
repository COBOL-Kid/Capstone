import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
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
    updateCurrentAccount: vi.fn().mockReturnValue(of(accountDetails)),
    changePassword: vi.fn().mockReturnValue(of(null)),
    refresh: vi.fn().mockReturnValue(of({ token: 'refreshed-token' })),
    clearSession: vi.fn(),
  };
}

describe('AccountDrawerComponent', () => {
  let fixture: ComponentFixture<AccountDrawerComponent>;
  let component: AccountDrawerComponent;
  let authService: ReturnType<typeof createAuthServiceStub>;

  beforeEach(async () => {
    authService = createAuthServiceStub();

    await TestBed.configureTestingModule({
      imports: [AccountDrawerComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountDrawerComponent);
    component = fixture.componentInstance;
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
  });

  it('renders profile edit controls prefilled from the loaded account', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    fixture.detectChanges();
    fixture.debugElement.query(By.css('.account-drawer__secondary-action')).nativeElement.click();
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

  it('changes the password and clears password fields on success', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    component.passwordForm.setValue({
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
      confirmPassword: 'NewSecret1!',
    });
    component.submitPassword();
    fixture.detectChanges();

    expect(authService.changePassword).toHaveBeenCalledWith({
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
    });
    expect(component.passwordForm.getRawValue()).toEqual({
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    });
    expect(fixture.nativeElement.textContent).toContain('Password updated.');
  });

  it('renders invalid current password errors without closing the drawer', () => {
    authService.isSignedIn.mockReturnValue(true);
    authService.changePassword.mockReturnValue(
      throwError(() => ({ message: 'Current password is incorrect', fieldMessages: [] })),
    );

    component.open();
    component.passwordForm.setValue({
      currentPassword: 'WrongSecret1!',
      newPassword: 'NewSecret1!',
      confirmPassword: 'NewSecret1!',
    });
    component.submitPassword();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Current password is incorrect');
    expect(component.isOpen()).toBe(true);
  });

  it('clears password fields when the drawer closes', () => {
    vi.useFakeTimers();
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    component.passwordForm.setValue({
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
      confirmPassword: 'NewSecret1!',
    });
    component.close();
    vi.advanceTimersByTime(240);
    fixture.detectChanges();

    expect(component.passwordForm.getRawValue()).toEqual({
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    });
    vi.useRealTimers();
  });

  it('clears password fields when the user is no longer signed in', () => {
    authService.isSignedIn.mockReturnValueOnce(true).mockReturnValue(false);

    component.open();
    component.passwordForm.setValue({
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
      confirmPassword: 'NewSecret1!',
    });
    component.open();
    fixture.detectChanges();

    expect(component.passwordForm.getRawValue()).toEqual({
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    });
    expect(component.status()).toBe('signed-out');
  });

  it('prevents password changes when confirmation does not match', () => {
    authService.isSignedIn.mockReturnValue(true);

    component.open();
    component.passwordForm.setValue({
      currentPassword: 'OldSecret1!',
      newPassword: 'NewSecret1!',
      confirmPassword: 'Different1!',
    });
    component.submitPassword();
    fixture.detectChanges();

    expect(authService.changePassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Passwords must match.');
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

    fixture.debugElement.query(By.css('.account-drawer__primary-action')).nativeElement.click();
    fixture.detectChanges();

    const modal = fixture.debugElement.query(By.directive(AuthModalComponent));
    expect(modal.componentInstance.mode()).toBe('sign-in');
  });

  it('opens the existing auth modal in sign-up mode', () => {
    component.open();
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.account-drawer__secondary-action')).nativeElement.click();
    fixture.detectChanges();

    const modal = fixture.debugElement.query(By.directive(AuthModalComponent));
    expect(modal.componentInstance.mode()).toBe('sign-up');
  });

  it('refreshes account details after the auth modal closes', () => {
    authService.isSignedIn.mockReturnValueOnce(false).mockReturnValue(true);

    component.open();
    component.openAuthModal('sign-in');
    fixture.detectChanges();

    component.closeAuthModal();
    fixture.detectChanges();

    expect(authService.getCurrentAccount).toHaveBeenCalled();
  });
});
