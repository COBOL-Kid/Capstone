import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  HostListener,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { AuthErrorMessage, AuthModalMode } from '../../core/auth/auth.models';

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).+$/;

@Component({
  selector: 'app-auth-modal',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './auth-modal.html',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthModalComponent implements AfterViewInit {
  readonly mode = input.required<AuthModalMode>();
  readonly useRoutingLinks = input(true);
  readonly close = output<void>();
  readonly modeChange = output<AuthModalMode>();
  protected readonly isSubmitting = signal(false);
  protected readonly serverError = signal<AuthErrorMessage | null>(null);
  protected readonly title = computed(() =>
    this.mode() === 'sign-up' ? 'Create your account' : 'Welcome back',
  );
  protected readonly submitLabel = computed(() =>
    this.mode() === 'sign-up' ? 'Sign Up' : 'Sign In',
  );
  protected readonly alternateMode = computed<AuthModalMode>(() =>
    this.mode() === 'sign-up' ? 'sign-in' : 'sign-up',
  );
  protected readonly alternatePath = computed(() =>
    this.alternateMode() === 'sign-up' ? '/sign-up' : '/sign-in',
  );
  protected readonly alternateLabel = computed(() =>
    this.alternateMode() === 'sign-up' ? 'Sign up' : 'Sign in',
  );
  protected readonly alternatePrompt = computed(() =>
    this.mode() === 'sign-up' ? 'Already have an account?' : 'Need an account?',
  );
  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    firstname: ['', [Validators.required, Validators.maxLength(100)]],
    lastname: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.maxLength(72)]],
  });
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.close.emit();
  }

  protected isSignup(): boolean {
    return this.mode() === 'sign-up';
  }

  protected passwordErrors(): string[] {
    const control = this.form.controls.password;

    if (!control.touched && !control.dirty) {
      return [];
    }

    const value = control.value;
    const errors: string[] = [];

    if (control.hasError('required')) {
      errors.push('Password is required.');
    }

    if (this.isSignup() && value.length > 0 && value.length < 8) {
      errors.push('Password must be at least 8 characters.');
    }

    if (control.hasError('maxlength')) {
      errors.push('Password must be 72 characters or fewer.');
    }

    if (this.isSignup() && value.length >= 8 && !passwordPattern.test(value)) {
      errors.push('Use uppercase, lowercase, number, and special characters.');
    }

    return errors;
  }

  protected submit(): void {
    if (this.isSubmitting()) {
      return;
    }

    this.applyModeValidators();
    this.serverError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();

      return;
    }

    this.isSubmitting.set(true);

    const value = this.form.getRawValue();
    const request = this.isSignup()
      ? this.authService.register({
          firstname: value.firstname.trim(),
          lastname: value.lastname.trim(),
          email: value.email.trim(),
          password: value.password,
        })
      : this.authService.login({
          email: value.email.trim(),
          password: value.password,
        });

    request
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => this.close.emit(),
        error: (error: AuthErrorMessage) => this.serverError.set(error),
      });
  }

  protected applyModeValidators(): void {
    const passwordValidators = [Validators.required, Validators.maxLength(72)];
    const nameValidators = this.isSignup() ? [Validators.required, Validators.maxLength(100)] : [];

    if (this.isSignup()) {
      passwordValidators.push(Validators.minLength(8), Validators.pattern(passwordPattern));
    }

    this.form.controls.firstname.setValidators(nameValidators);
    this.form.controls.lastname.setValidators(nameValidators);
    this.form.controls.password.setValidators(passwordValidators);

    this.form.controls.firstname.updateValueAndValidity({ emitEvent: false });
    this.form.controls.lastname.updateValueAndValidity({ emitEvent: false });
    this.form.controls.password.updateValueAndValidity({ emitEvent: false });
  }

  protected switchMode(): void {
    this.serverError.set(null);
    this.form.reset();
    this.modeChange.emit(this.alternateMode());
  }
}
