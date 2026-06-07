import { Injectable, signal } from '@angular/core';

export type ToastVariant = 'success' | 'error' | 'info';

export interface Toast {
  readonly id: number;
  readonly message: string;
  readonly variant: ToastVariant;
  readonly durationMs: number;
}

const DEFAULT_DURATION_MS = 4000;
const ERROR_DURATION_MS = 6000;

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly toastsState = signal<readonly Toast[]>([]);
  readonly toasts = this.toastsState.asReadonly();

  private nextId = 0;

  show(message: string, variant: ToastVariant = 'info', durationMs = DEFAULT_DURATION_MS): number {
    const id = this.nextId++;
    this.toastsState.update((toasts) => [...toasts, { id, message, variant, durationMs }]);
    return id;
  }

  success(message: string, durationMs = DEFAULT_DURATION_MS): number {
    return this.show(message, 'success', durationMs);
  }

  error(message: string, durationMs = ERROR_DURATION_MS): number {
    return this.show(message, 'error', durationMs);
  }

  info(message: string, durationMs = DEFAULT_DURATION_MS): number {
    return this.show(message, 'info', durationMs);
  }

  dismiss(id: number): void {
    this.toastsState.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  clear(): void {
    this.toastsState.set([]);
  }
}
