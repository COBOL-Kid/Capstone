import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { VehiclePhotoModalComponent } from './vehicle-photo-modal';
import { VinService } from '../../core/vin/vin.service';

describe('VehiclePhotoModalComponent', () => {
  const vin = 'JTENU5JR6M5962554';
  const photo1 = 'https://example.com/photo-1.jpg';
  const photo2 = 'https://example.com/photo-2.jpg';

  function createFixture() {
    const vinService = {
      updateSelectedPhoto: vi.fn(),
    };

    TestBed.configureTestingModule({
      imports: [VehiclePhotoModalComponent],
      providers: [{ provide: VinService, useValue: vinService }],
    });

    const fixture = TestBed.createComponent(VehiclePhotoModalComponent);
    fixture.componentRef.setInput('vin', vin);
    fixture.componentRef.setInput('availableImageUrls', [photo1, photo2]);
    fixture.componentRef.setInput('selectedImageUrl', photo1);
    fixture.detectChanges();

    return { fixture, vinService };
  }

  it('emits updated with the clicked url and closes on success', () => {
    const { fixture, vinService } = createFixture();
    const updatedUrls: string[] = [];
    const closeCount = { value: 0 };

    vinService.updateSelectedPhoto.mockReturnValue(
      of({
        vin,
        currentMileage: 45000,
        vehicleTypeId: 7,
        make: 'Toyota',
        model: '4RUNNER',
        trim: 'SRS Prem',
        year: '2021',
        availableImageUrls: [photo1, photo2],
        selectedImageUrl: photo2,
      }),
    );

    fixture.componentInstance.updated.subscribe((url) => updatedUrls.push(url));
    fixture.componentInstance.close.subscribe(() => closeCount.value++);

    const tiles = fixture.nativeElement.querySelectorAll(
      '.vehicle-photo-modal__tile',
    ) as NodeListOf<HTMLButtonElement>;
    tiles[1].click();
    fixture.detectChanges();

    expect(vinService.updateSelectedPhoto).toHaveBeenCalledWith(vin, {
      selectedImageUrl: photo2,
    });
    expect(updatedUrls).toEqual([photo2]);
    expect(closeCount.value).toBe(1);
  });

  it('renders a scrollable body with one tile per photo when many urls are provided', () => {
    const manyPhotos = Array.from(
      { length: 14 },
      (_, index) => `https://example.com/photo-${index + 1}.jpg`,
    );
    const vinService = { updateSelectedPhoto: vi.fn() };

    TestBed.configureTestingModule({
      imports: [VehiclePhotoModalComponent],
      providers: [{ provide: VinService, useValue: vinService }],
    });

    const fixture = TestBed.createComponent(VehiclePhotoModalComponent);
    fixture.componentRef.setInput('vin', vin);
    fixture.componentRef.setInput('availableImageUrls', manyPhotos);
    fixture.componentRef.setInput('selectedImageUrl', manyPhotos[0]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.vehicle-photo-modal__body')).not.toBeNull();
    expect(fixture.nativeElement.querySelectorAll('.vehicle-photo-modal__tile').length).toBe(
      manyPhotos.length,
    );
    expect(fixture.nativeElement.querySelector('.vehicle-photo-modal__grid')).not.toBeNull();
  });

  it('shows an error and stays open when the update fails', () => {
    const { fixture, vinService } = createFixture();

    vinService.updateSelectedPhoto.mockReturnValue(throwError(() => new Error('network error')));

    const tiles = fixture.nativeElement.querySelectorAll(
      '.vehicle-photo-modal__tile',
    ) as NodeListOf<HTMLButtonElement>;
    tiles[1].click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Unable to update photo');
    expect(fixture.nativeElement.querySelector('.vehicle-photo-modal__tile')).not.toBeNull();
  });
});
