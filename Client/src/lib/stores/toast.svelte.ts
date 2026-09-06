export type ToastVariant = 'success' | 'error' | 'info';

export interface Toast {
  readonly id: number;
  readonly message: string;
  readonly variant: ToastVariant;
  readonly durationMs: number;
}

const DEFAULT_DURATION_MS = 4000;
const ERROR_DURATION_MS = 6000;

class ToastStore {
  private toastsState = $state<readonly Toast[]>([]);
  private nextId = 0;

  readonly toasts = $derived(this.toastsState);

  show(message: string, variant: ToastVariant = 'info', durationMs = DEFAULT_DURATION_MS): number {
    const id = this.nextId++;
    this.toastsState = [...this.toastsState, { id, message, variant, durationMs }];
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
    this.toastsState = this.toastsState.filter((toast) => toast.id !== id);
  }

  clear(): void {
    this.toastsState = [];
  }
}

export const toastStore = new ToastStore();
