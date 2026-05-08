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
