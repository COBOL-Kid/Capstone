import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  input,
  output,
  viewChild,
} from '@angular/core';

import {
  formatWarrantyCoverageLabel,
  formatWarrantyCoverageStatus,
} from '../../core/warranty/warranty-display';
import { VehicleWarrantyResponse } from '../../core/warranty/warranty.models';

@Component({
  selector: 'app-warranty-modal',
  standalone: true,
  templateUrl: './warranty-modal.html',
  styleUrl: './warranty-modal.css',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WarrantyModalComponent implements AfterViewInit {
  readonly warranty = input.required<VehicleWarrantyResponse>();
  readonly close = output<void>();

  protected readonly formatWarrantyCoverageLabel = formatWarrantyCoverageLabel;
  protected readonly formatWarrantyCoverageStatus = formatWarrantyCoverageStatus;

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');

  ngAfterViewInit(): void {
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.close.emit();
  }
}
