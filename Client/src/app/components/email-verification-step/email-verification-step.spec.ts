import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { EmailVerificationStepComponent } from './email-verification-step';

describe('EmailVerificationStepComponent', () => {
  it('disables resend while submit is in progress', () => {
    const fixture = TestBed.createComponent(EmailVerificationStepComponent);
    fixture.componentRef.setInput('isSubmitting', true);
    fixture.componentRef.setInput('isResending', false);
    fixture.detectChanges();

    const resendButton = fixture.nativeElement.querySelector(
      'button.hc-btn--secondary',
    ) as HTMLButtonElement;
    expect(resendButton.disabled).toBe(true);
  });

  it('disables submit while resend is in progress', () => {
    const fixture = TestBed.createComponent(EmailVerificationStepComponent);
    fixture.componentRef.setInput('isSubmitting', false);
    fixture.componentRef.setInput('isResending', true);
    fixture.detectChanges();

    const submitButton = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;
    expect(submitButton.disabled).toBe(true);
  });

  it('does not emit resend while submit is in progress', () => {
    const fixture = TestBed.createComponent(EmailVerificationStepComponent);
    fixture.componentRef.setInput('isSubmitting', true);
    fixture.detectChanges();

    const resend = vi.fn();
    fixture.componentInstance.resend.subscribe(resend);
    fixture.componentInstance['onResend']();

    expect(resend).not.toHaveBeenCalled();
  });
});
