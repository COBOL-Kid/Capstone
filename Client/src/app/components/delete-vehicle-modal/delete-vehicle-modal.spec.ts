import { TestBed } from '@angular/core/testing';
import { NEVER } from 'rxjs';
import { vi } from 'vitest';

import { DeleteVehicleModalComponent } from './delete-vehicle-modal';
import { VinService } from '../../core/vin/vin.service';

describe('DeleteVehicleModalComponent', () => {
  it('does not close while delete is in flight', () => {
    const vinService = {
      deleteVehicle: vi.fn().mockReturnValue(NEVER),
    };

    TestBed.configureTestingModule({
      imports: [DeleteVehicleModalComponent],
      providers: [{ provide: VinService, useValue: vinService }],
    });

    const fixture = TestBed.createComponent(DeleteVehicleModalComponent);
    fixture.componentRef.setInput('vin', 'JTENU5JR6M5962554');
    fixture.detectChanges();

    const close = vi.fn();
    fixture.componentInstance.close.subscribe(close);

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.hc-overlay-backdrop') as HTMLDivElement).click();
    (fixture.nativeElement.querySelector('.hc-dialog__close') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(close).not.toHaveBeenCalled();
  });
});
