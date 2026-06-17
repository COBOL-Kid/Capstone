import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
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
import { finalize } from 'rxjs';

import { VinService } from '../../core/vin/vin.service';

@Component({
  selector: 'app-vehicle-photo-modal',
  standalone: true,
  templateUrl: './vehicle-photo-modal.html',
  styleUrl: './vehicle-photo-modal.css',
  host: {
    class: 'hc-modal-host',
    style: '--hc-dialog-max-width: 36rem',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VehiclePhotoModalComponent implements AfterViewInit {
  readonly vin = input.required<string>();
  readonly availableImageUrls = input.required<string[]>();
  readonly selectedImageUrl = input.required<string>();
  readonly close = output<void>();
  readonly updated = output<string>();

  protected readonly savingUrl = signal<string | null>(null);
  protected readonly serverError = signal<string | null>(null);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly vinService = inject(VinService);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.requestClose();
  }

  protected requestClose(): void {
    if (this.savingUrl()) {
      return;
    }
    this.close.emit();
  }

  protected selectPhoto(url: string): void {
    if (this.savingUrl()) {
      return;
    }

    if (url === this.selectedImageUrl()) {
      this.close.emit();
      return;
    }

    this.serverError.set(null);
    this.savingUrl.set(url);

    this.vinService
      .updateSelectedPhoto(this.vin(), { selectedImageUrl: url })
      .pipe(
        finalize(() => this.savingUrl.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.updated.emit(url);
          this.close.emit();
        },
        error: () => {
          this.serverError.set('Unable to update photo. Please try again.');
        },
      });
  }

  protected isSelected(url: string): boolean {
    return url === this.selectedImageUrl();
  }

  protected isSaving(url: string): boolean {
    return this.savingUrl() === url;
  }
}
