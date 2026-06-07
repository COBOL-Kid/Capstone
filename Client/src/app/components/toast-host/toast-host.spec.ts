import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { ToastHostComponent } from './toast-host';
import { ToastService } from '../../core/toast/toast.service';

describe('ToastHostComponent', () => {
  let fixture: ComponentFixture<ToastHostComponent>;
  let toastService: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ToastHostComponent);
    toastService = TestBed.inject(ToastService);
    fixture.detectChanges();
  });

  afterEach(() => {
    toastService.clear();
    vi.useRealTimers();
  });

  it('does not auto-dismiss a toast that arrives while timers are paused', () => {
    vi.useFakeTimers();

    toastService.success('First toast', 5000);
    fixture.detectChanges();

    const hostEl = fixture.nativeElement.querySelector('.hc-toast-host') as HTMLElement;
    hostEl.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();

    toastService.success('Arrived during hover', 1000);
    fixture.detectChanges();

    vi.advanceTimersByTime(2000);
    fixture.detectChanges();

    expect(toastService.toasts()).toHaveLength(2);

    hostEl.dispatchEvent(new MouseEvent('mouseleave'));
    fixture.detectChanges();

    vi.runAllTimers();
    fixture.detectChanges();

    expect(toastService.toasts()).toHaveLength(0);
  });
});
