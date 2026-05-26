import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { VehicleDetailPageComponent } from './vehicle-detail-page';
import { VinService } from '../../core/vin/vin.service';
import { MaintenanceService } from '../../core/maintenance/maintenance.service';
import { RecallService } from '../../core/recall/recall.service';

describe('VehicleDetailPageComponent', () => {
  let fixture: ComponentFixture<VehicleDetailPageComponent>;

  const vehicleDetail = {
    vin: 'JTENU5JR6M5962554',
    vehicleTypeId: 7,
    vehicleMake: 'Toyota',
    vehicleModel: '4RUNNER',
    vehicleTrim: 'SRS Prem',
    vehicleYear: '2021',
    vehicleStyle: 'SUV',
    sourceVin: null,
    origin: null,
    body: null,
    engineDescription: null,
    transmissionStyle: null,
    driveType: null,
    ownersManual: null,
    currentMileage: 45000,
    availableImageUrls: ['https://example.com/photo.jpg'],
    selectedImageUrl: 'https://example.com/photo.jpg',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VehicleDetailPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ vin: 'JTENU5JR6M5962554' })),
          },
        },
        {
          provide: VinService,
          useValue: { getVehicleDetail: vi.fn(() => of(vehicleDetail)) },
        },
        {
          provide: MaintenanceService,
          useValue: {
            getUpcoming: vi.fn(() => of([])),
            getCompleted: vi.fn(() => of([])),
            getMaintenanceCosts: vi.fn(() => of([])),
          },
        },
        {
          provide: RecallService,
          useValue: {
            getUncompleted: vi.fn(() => of([])),
            getCompleted: vi.fn(() => of([])),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VehicleDetailPageComponent);
    fixture.detectChanges();
  });

  it('renders vehicle detail and shows maintenance panel by default', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('2021 Toyota 4RUNNER');
    expect(text).toContain('45,000 mi');
    expect(text).toContain('Upcoming maintenance');
    expect(text).not.toContain('No open recalls.');
  });

  it('switches to recalls panel when recalls toggle is clicked', () => {
    const recallsButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Recalls'));

    expect(recallsButton).toBeDefined();
    recallsButton!.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No open recalls.');
    expect(fixture.nativeElement.textContent).not.toContain('Upcoming maintenance');
  });

  it('does not show owner manual button when ownersManual is null', () => {
    const manualButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes("Owner's manual"));

    expect(manualButton).toBeUndefined();
  });

  it('opens maintenance costs modal when maintenance costs button is clicked', () => {
    const costsButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Maintenance costs'));

    expect(costsButton).toBeDefined();
    costsButton!.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-maintenance-costs-modal')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Search');
  });
});

describe('VehicleDetailPageComponent with owners manual', () => {
  let fixture: ComponentFixture<VehicleDetailPageComponent>;

  const vehicleDetailWithManual = {
    vin: 'JTENU5JR6M5962554',
    vehicleTypeId: 7,
    vehicleMake: 'Toyota',
    vehicleModel: '4RUNNER',
    vehicleTrim: 'SRS Prem',
    vehicleYear: '2021',
    vehicleStyle: 'SUV',
    sourceVin: null,
    origin: null,
    body: null,
    engineDescription: null,
    transmissionStyle: null,
    driveType: null,
    ownersManual: 'https://example.com/manual.pdf',
    currentMileage: 45000,
    availableImageUrls: ['https://example.com/photo.jpg'],
    selectedImageUrl: 'https://example.com/photo.jpg',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VehicleDetailPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ vin: 'JTENU5JR6M5962554' })),
          },
        },
        {
          provide: VinService,
          useValue: { getVehicleDetail: vi.fn(() => of(vehicleDetailWithManual)) },
        },
        {
          provide: MaintenanceService,
          useValue: {
            getUpcoming: vi.fn(() => of([])),
            getCompleted: vi.fn(() => of([])),
            getMaintenanceCosts: vi.fn(() => of([])),
          },
        },
        {
          provide: RecallService,
          useValue: {
            getUncompleted: vi.fn(() => of([])),
            getCompleted: vi.fn(() => of([])),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VehicleDetailPageComponent);
    fixture.detectChanges();
  });

  it('shows owner manual button and opens manual in a new tab on click', () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue({ opener: null } as Window);

    const manualButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes("Owner's manual"));

    expect(manualButton).toBeDefined();
    manualButton!.click();

    expect(openSpy).toHaveBeenCalledWith('https://example.com/manual.pdf', '_blank');
    expect(fixture.nativeElement.querySelector('app-owners-manual-modal')).toBeNull();

    openSpy.mockRestore();
  });
});
