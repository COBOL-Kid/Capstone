import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  signal,
} from '@angular/core';

import { Toast, ToastService } from '../../core/toast/toast.service';

interface TimerState {
  handle: ReturnType<typeof setTimeout> | null;
  remaining: number;
  startedAt: number;
}

const EXIT_ANIMATION_MS = 200;

@Component({
  selector: 'app-toast-host',
  standalone: true,
  templateUrl: './toast-host.html',
  styleUrl: './toast-host.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastHostComponent {
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly toasts = this.toastService.toasts;
  protected readonly leaving = signal<ReadonlySet<number>>(new Set());

  private readonly timers = new Map<number, TimerState>();
  private timersPaused = false;
  private readonly prefersReducedMotion =
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  constructor() {
    effect(() => {
      const current = this.toasts();
      const currentIds = new Set(current.map((toast) => toast.id));
      const leaving = this.leaving();

      for (const toast of current) {
        if (!this.timers.has(toast.id) && !leaving.has(toast.id) && toast.durationMs > 0) {
          this.startTimer(toast.id, toast.durationMs);
        }
      }

      for (const id of [...this.timers.keys()]) {
        if (!currentIds.has(id)) {
          this.clearTimer(id);
        }
      }
    });

    this.destroyRef.onDestroy(() => {
      for (const id of [...this.timers.keys()]) {
        this.clearTimer(id);
      }
    });
  }

  protected isLeaving(id: number): boolean {
    return this.leaving().has(id);
  }

  protected role(toast: Toast): 'alert' | 'status' {
    return toast.variant === 'error' ? 'alert' : 'status';
  }

  protected dismiss(id: number): void {
    if (this.leaving().has(id)) {
      return;
    }
    this.clearTimer(id);
    this.beginLeave(id);
  }

  protected pauseTimers(): void {
    if (this.timersPaused) {
      return;
    }
    this.timersPaused = true;

    for (const state of this.timers.values()) {
      if (state.handle === null) {
        continue;
      }
      clearTimeout(state.handle);
      state.handle = null;
      state.remaining = Math.max(0, state.remaining - (Date.now() - state.startedAt));
    }
  }

  protected resumeTimers(): void {
    if (!this.timersPaused) {
      return;
    }
    this.timersPaused = false;

    for (const [id, state] of this.timers) {
      if (state.handle !== null) {
        continue;
      }
      if (state.remaining <= 0) {
        this.dismiss(id);
        continue;
      }
      state.startedAt = Date.now();
      state.handle = setTimeout(() => this.dismiss(id), state.remaining);
    }
  }

  private startTimer(id: number, durationMs: number): void {
    if (this.timersPaused) {
      this.timers.set(id, { handle: null, remaining: durationMs, startedAt: Date.now() });
      return;
    }

    const handle = setTimeout(() => this.dismiss(id), durationMs);
    this.timers.set(id, { handle, remaining: durationMs, startedAt: Date.now() });
  }

  private clearTimer(id: number): void {
    const state = this.timers.get(id);
    if (state?.handle != null) {
      clearTimeout(state.handle);
    }
    this.timers.delete(id);
  }

  private beginLeave(id: number): void {
    this.leaving.update((set) => new Set(set).add(id));

    const remove = (): void => {
      this.toastService.dismiss(id);
      this.leaving.update((set) => {
        const next = new Set(set);
        next.delete(id);
        return next;
      });
    };

    if (this.prefersReducedMotion) {
      remove();
      return;
    }

    setTimeout(remove, EXIT_ANIMATION_MS);
  }
}
