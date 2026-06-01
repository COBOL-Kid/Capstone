import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { VehicleDetailPageComponent } from './vehicle-detail-page';
import { VehiclePageDataService } from '../../core/vin/vehicle-page.data';

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

  const emptyPageData = {
    detail: vehicleDetail,
    upcomingIntervals: [] as never[],
    completedMaintenance: [] as never[],
    uncompletedRecalls: [] as never[],
    completedRecalls: [] as never[],
    miscMaintenanceCosts: [] as never[],
    vehicleWarranty: null,
  };

  const sampleWarranty = {
    vehicleYear: '2021',
    vehicleMake: 'Toyota',
    vehicleModel: '4RUNNER',
    coverages: [
      {
        coverageName: 'Warranty - Basic (months/miles)',
        coverageValue: '36/36,000',
        estimatedExpirationDate: '2024-01-01',
        expired: false,
        remainingMonths: 6,
        remainingMiles: 8_432,
      },
    ],
  };

  const expiredWarranty = {
    ...sampleWarranty,
    coverages: [
      {
        ...sampleWarranty.coverages[0],
        expired: true,
        remainingMonths: null,
        remainingMiles: null,
      },
    ],
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
          provide: VehiclePageDataService,
          useValue: {
            loadVehiclePage: vi.fn(() => of(emptyPageData)),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VehicleDetailPageComponent);
  });

  it('renders vehicle detail and shows maintenance panel by default', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('2021 Toyota 4RUNNER');
    expect(text).toContain('45,000 mi');
    expect(text).toContain('Upcoming maintenance');
    expect(text).not.toContain('No open recalls.');
  });

  it('switches to recalls panel when recalls toggle is clicked', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const recallsButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Recalls'));

    expect(recallsButton).toBeDefined();
    recallsButton!.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No open recalls.');
    expect(fixture.nativeElement.textContent).not.toContain('Upcoming maintenance');
  });

  it('does not show owner manual button when ownersManual is null', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const manualButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes("Owner's manual"));

    expect(manualButton).toBeUndefined();
  });

  it('does not show warranty button when vehicleWarranty is null', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const warrantyButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Warranty information'));

    expect(warrantyButton).toBeUndefined();
  });

  it('opens warranty modal when warranty button is clicked', async () => {
    const loadVehiclePage = vi.fn(() => of({ ...emptyPageData, vehicleWarranty: sampleWarranty }));
    TestBed.resetTestingModule();
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
          provide: VehiclePageDataService,
          useValue: { loadVehiclePage },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VehicleDetailPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const warrantyButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Warranty information'));

    expect(warrantyButton).toBeDefined();
    warrantyButton!.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-warranty-modal')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Basic');
    expect(fixture.nativeElement.textContent).toContain('36/36,000');
    expect(fixture.nativeElement.textContent).toContain('Active');
  });

  it('shows warranty status in specs when coverage has computed fields', async () => {
    const loadVehiclePage = vi.fn(() => of({ ...emptyPageData, vehicleWarranty: sampleWarranty }));
    TestBed.resetTestingModule();
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
          provide: VehiclePageDataService,
          useValue: { loadVehiclePage },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VehicleDetailPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const specs = fixture.nativeElement.querySelector('.vehicle-detail__specs');
    expect(specs?.textContent).toContain('Basic warranty');
    expect(specs?.textContent).toContain('Active');
    expect(specs?.textContent).toContain('8,432 mi');
    expect(specs?.textContent).toContain('exp. 2024');
  });

  it('shows expired warranty status in specs', async () => {
    const loadVehiclePage = vi.fn(() => of({ ...emptyPageData, vehicleWarranty: expiredWarranty }));
    TestBed.resetTestingModule();
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
          provide: VehiclePageDataService,
          useValue: { loadVehiclePage },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VehicleDetailPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const specs = fixture.nativeElement.querySelector('.vehicle-detail__specs');
    expect(specs?.textContent).toContain('Expired');
    expect(specs?.textContent).toContain('exp. 2024');
  });

  it('reloads dashboard after mileage update', async () => {
    const loadVehiclePage = vi.fn(() => of(emptyPageData));
    TestBed.resetTestingModule();
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
          provide: VehiclePageDataService,
          useValue: { loadVehiclePage },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VehicleDetailPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(loadVehiclePage).toHaveBeenCalledTimes(1);

    fixture.componentInstance['onMileageUpdated']({
      ...vehicleDetail,
      currentMileage: 46_000,
    });
    await fixture.whenStable();

    expect(loadVehiclePage).toHaveBeenCalledTimes(2);
  });

  it('opens maintenance costs modal when maintenance costs button is clicked', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

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

describe('VehicleDetailPageComponent with upcoming maintenance', () => {
  let fixture: ComponentFixture<VehicleDetailPageComponent>;

  const upcomingIntervals = [
    {
      mileageDue: 50000,
      summary: {
        totalPartsCost: 49.59,
        totalLaborCost: 48.4,
        totalCost: 97.99,
        currency: 'USD',
      },
      items: [
        {
          maintMileageId: 11,
          maintDesc: 'Replace engine oil and filter',
          isInspect: false,
          parts: [{ partDesc: 'Engine oil and filter', totalCost: 49.59, currency: 'USD' }],
          labor: {
            timeRequiredHours: 1,
            hourlyRate: 48.4,
            totalCost: 48.4,
            currency: 'USD',
          },
        },
        {
          maintMileageId: 12,
          maintDesc: 'Inspect - Battery',
          isInspect: true,
          parts: [],
          labor: {
            timeRequiredHours: 0.05,
            hourlyRate: 55,
            totalCost: 2.75,
            currency: 'USD',
          },
        },
      ],
    },
  ];

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
          provide: VehiclePageDataService,
          useValue: {
            loadVehiclePage: vi.fn(() =>
              of({
                detail: vehicleDetail,
                upcomingIntervals,
                completedMaintenance: [],
                uncompletedRecalls: [],
                completedRecalls: [],
                miscMaintenanceCosts: [],
                vehicleWarranty: null,
              }),
            ),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VehicleDetailPageComponent);
  });

  it('renders grouped upcoming maintenance with labor, expandable parts, and mark complete', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('50,000 mi service');
    expect(text).toContain('Replace engine oil and filter');
    expect(text).toContain('Engine oil and filter');
    expect(text).toContain('Inspect - Battery');
    expect(text).toContain('Inspection');
    expect(text).toContain('Total parts');
    expect(text).toContain('Total labor');
    expect(text).toContain('Total cost');

    const markCompleteButtons = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).filter((button) => button.textContent?.trim() === 'Mark complete');

    expect(markCompleteButtons.length).toBe(2);
  });

  it('hides inspection items when show inspections is unchecked', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const inspectCheckbox = fixture.nativeElement.querySelector(
      '.vehicle-detail__inspect-filter input',
    ) as HTMLInputElement;

    inspectCheckbox.checked = false;
    inspectCheckbox.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Replace engine oil and filter');
    expect(text).not.toContain('Inspect - Battery');
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
          provide: VehiclePageDataService,
          useValue: {
            loadVehiclePage: vi.fn(() =>
              of({
                detail: vehicleDetailWithManual,
                upcomingIntervals: [],
                completedMaintenance: [],
                uncompletedRecalls: [],
                completedRecalls: [],
                miscMaintenanceCosts: [],
                vehicleWarranty: null,
              }),
            ),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VehicleDetailPageComponent);
  });

  it('shows owner manual button and opens manual in a new tab on click', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
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
