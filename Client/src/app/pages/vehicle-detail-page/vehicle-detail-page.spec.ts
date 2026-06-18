import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { VehicleDetailPageComponent } from './vehicle-detail-page';
import { VehiclePhotoModalComponent } from '../../components/vehicle-photo-modal/vehicle-photo-modal';
import { VehiclePageDataService } from '../../core/vin/vehicle-page.data';
import { UserVehiclesStore } from '../../core/vin/user-vehicles.store';
import { VinService } from '../../core/vin/vin.service';

describe('VehicleDetailPageComponent', () => {
  let fixture: ComponentFixture<VehicleDetailPageComponent>;
  let vehiclesStore: UserVehiclesStore;
  let loadVehiclePage: ReturnType<typeof vi.fn>;

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
    availableImageUrls: ['https://example.com/photo-1.jpg', 'https://example.com/photo-2.jpg'],
    selectedImageUrl: 'https://example.com/photo-1.jpg',
  };

  const informationUnavailableMessage = 'information not yet available for this vehicle';

  const sampleMiscMaintenanceCosts = [
    {
      miscMaintCostId: 1,
      maintTitle: 'Oil Change',
      maintDesc: 'Replace engine oil and filter',
      independentAvg: 65,
      independentHigh: 95,
      independentLow: 45,
      dealerAvg: 110,
      dealerHigh: 145,
      dealerLow: 85,
    },
  ];

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
    loadVehiclePage = vi.fn(() => of(emptyPageData));

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
            loadVehiclePage,
          },
        },
        {
          provide: VinService,
          useValue: {
            updateSelectedPhoto: vi.fn(() =>
              of({
                vin: 'JTENU5JR6M5962554',
                currentMileage: 45000,
                vehicleTypeId: 7,
                make: 'Toyota',
                model: '4RUNNER',
                trim: 'SRS Prem',
                year: '2021',
                availableImageUrls: [
                  'https://example.com/photo-1.jpg',
                  'https://example.com/photo-2.jpg',
                ],
                selectedImageUrl: 'https://example.com/photo-2.jpg',
              }),
            ),
          },
        },
      ],
    }).compileComponents();

    vehiclesStore = TestBed.inject(UserVehiclesStore);
    vehiclesStore.reset();
    vehiclesStore.setVehicles([
      {
        vin: 'JTENU5JR6M5962554',
        currentMileage: 45000,
        vehicleTypeId: 7,
        make: 'Toyota',
        model: '4RUNNER',
        trim: 'SRS Prem',
        year: '2021',
        availableImageUrls: ['https://example.com/photo-1.jpg', 'https://example.com/photo-2.jpg'],
        selectedImageUrl: 'https://example.com/photo-1.jpg',
      },
    ]);

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

    expect(fixture.nativeElement.textContent).toContain('No open recalls');
    expect(fixture.nativeElement.textContent).not.toContain('Upcoming maintenance');
  });

  it('disables owner manual button with tooltip when ownersManual is null', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const openSpy = vi.spyOn(window, 'open');
    const manualButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes("Owner's manual"));

    expect(manualButton).toBeDefined();
    expect(manualButton!.disabled).toBe(true);
    expect(manualButton!.closest('.vehicle-detail__manual-btn-wrap')?.getAttribute('title')).toBe(
      informationUnavailableMessage,
    );

    manualButton!.click();
    fixture.detectChanges();

    expect(openSpy).not.toHaveBeenCalled();
    openSpy.mockRestore();
  });

  it('disables warranty button with tooltip when vehicleWarranty is null', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const warrantyButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Warranty information'));

    expect(warrantyButton).toBeDefined();
    expect(warrantyButton!.disabled).toBe(true);
    expect(warrantyButton!.closest('.vehicle-detail__costs-btn-wrap')?.getAttribute('title')).toBe(
      informationUnavailableMessage,
    );

    warrantyButton!.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-warranty-modal')).toBeNull();
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
    const loadVehiclePage = vi.fn(() =>
      of({ ...emptyPageData, miscMaintenanceCosts: sampleMiscMaintenanceCosts }),
    );
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

    const costsButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Maintenance costs'));

    expect(costsButton).toBeDefined();
    expect(costsButton!.disabled).toBe(false);
    costsButton!.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-maintenance-costs-modal')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Search');
  });

  it('disables maintenance costs button with tooltip when miscMaintenanceCosts is empty', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const costsButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Maintenance costs'));

    expect(costsButton).toBeDefined();
    expect(costsButton!.disabled).toBe(true);
    expect(costsButton!.closest('.vehicle-detail__costs-btn-wrap')?.getAttribute('title')).toBe(
      informationUnavailableMessage,
    );

    costsButton!.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-maintenance-costs-modal')).toBeNull();
  });

  it('shows camera button when vehicle photos are available', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const photoButton = fixture.nativeElement.querySelector(
      '.vehicle-detail__photo-btn',
    ) as HTMLButtonElement;

    expect(photoButton).not.toBeNull();
    expect(photoButton.getAttribute('aria-label')).toBe('Change vehicle photo');
  });

  it('does not show camera button when no photos are available', async () => {
    const loadVehiclePage = vi.fn(() =>
      of({
        ...emptyPageData,
        detail: {
          ...vehicleDetail,
          availableImageUrls: [],
          selectedImageUrl: '',
        },
      }),
    );
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

    expect(fixture.nativeElement.querySelector('.vehicle-detail__photo-btn')).toBeNull();
  });

  it('opens vehicle photo modal when camera button is clicked', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const photoButton = fixture.nativeElement.querySelector(
      '.vehicle-detail__photo-btn',
    ) as HTMLButtonElement;
    photoButton.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-vehicle-photo-modal')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Vehicle photos');
  });

  it('updates hero image when photo selection changes', async () => {
    loadVehiclePage.mockReturnValue(
      of({
        ...emptyPageData,
        detail: {
          ...vehicleDetail,
          selectedImageUrl: 'https://example.com/photo-2.jpg',
        },
      }),
    );

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance['onPhotoUpdated']('https://example.com/photo-2.jpg');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const heroImage = fixture.nativeElement.querySelector(
      '.vehicle-detail__image',
    ) as HTMLImageElement;
    expect(heroImage.src).toBe('https://example.com/photo-2.jpg');
    expect(fixture.nativeElement.querySelector('app-vehicle-photo-modal')).toBeNull();
    expect(vehiclesStore.vehicles()[0].selectedImageUrl).toBe('https://example.com/photo-2.jpg');
  });

  it('updates hero image when a photo is selected in the modal', async () => {
    const vinService = TestBed.inject(VinService);
    const updateSelectedPhoto = vi.mocked(vinService.updateSelectedPhoto);
    updateSelectedPhoto.mockReturnValue(
      of({
        vin: 'JTENU5JR6M5962554',
        currentMileage: 45000,
        vehicleTypeId: 7,
        make: 'Toyota',
        model: '4RUNNER',
        trim: 'SRS Prem',
        year: '2021',
        availableImageUrls: ['https://example.com/photo-1.jpg', 'https://example.com/photo-2.jpg'],
        selectedImageUrl: 'https://example.com/photo-2.jpg',
      }),
    );
    loadVehiclePage.mockReturnValue(of(emptyPageData));

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const heroBefore = fixture.nativeElement.querySelector(
      '.vehicle-detail__image',
    ) as HTMLImageElement;
    expect(heroBefore.src).toBe('https://example.com/photo-1.jpg');

    const photoButton = fixture.nativeElement.querySelector(
      '.vehicle-detail__photo-btn',
    ) as HTMLButtonElement;
    photoButton.click();
    fixture.detectChanges();

    loadVehiclePage.mockReturnValue(
      of({
        ...emptyPageData,
        detail: {
          ...vehicleDetail,
          selectedImageUrl: 'https://example.com/photo-2.jpg',
        },
      }),
    );

    const modal = fixture.debugElement.query(By.directive(VehiclePhotoModalComponent))
      .componentInstance as VehiclePhotoModalComponent;
    modal['selectPhoto']('https://example.com/photo-2.jpg');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const heroImage = fixture.nativeElement.querySelector(
      '.vehicle-detail__image',
    ) as HTMLImageElement;
    expect(heroImage.src).toBe('https://example.com/photo-2.jpg');
    expect(updateSelectedPhoto).toHaveBeenCalledWith('JTENU5JR6M5962554', {
      selectedImageUrl: 'https://example.com/photo-2.jpg',
    });
    expect(fixture.nativeElement.querySelector('app-vehicle-photo-modal')).toBeNull();
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
    availableImageUrls: ['https://example.com/photo-1.jpg', 'https://example.com/photo-2.jpg'],
    selectedImageUrl: 'https://example.com/photo-1.jpg',
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
    expect(text).toContain('Inspect - Battery');
    expect(text).toContain('Inspection');
    expect(text).toContain('Parts$49.59');
    expect(text).toContain('Labor$48.40');
    expect(text).toContain('Total$97.99');

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
    availableImageUrls: ['https://example.com/photo-1.jpg', 'https://example.com/photo-2.jpg'],
    selectedImageUrl: 'https://example.com/photo-1.jpg',
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
    expect(manualButton!.disabled).toBe(false);
    manualButton!.click();

    expect(openSpy).toHaveBeenCalledWith('https://example.com/manual.pdf', '_blank');
    expect(fixture.nativeElement.querySelector('app-owners-manual-modal')).toBeNull();

    openSpy.mockRestore();
  });
});

describe('VehicleDetailPageComponent with unsafe owners manual url', () => {
  it('disables owner manual button for javascript urls', async () => {
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
                detail: {
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
                  ownersManual: 'javascript:alert(1)',
                  currentMileage: 45000,
                  availableImageUrls: ['https://example.com/photo-1.jpg'],
                  selectedImageUrl: 'https://example.com/photo-1.jpg',
                },
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

    const fixture = TestBed.createComponent(VehicleDetailPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const manualButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes("Owner's manual"));

    expect(manualButton).toBeDefined();
    expect(manualButton!.disabled).toBe(true);
  });
});

describe('VehicleDetailPageComponent error states', () => {
  const vin = 'JTENU5JR6M5962554';

  async function createFixture(loadVehiclePage: ReturnType<typeof vi.fn>) {
    await TestBed.configureTestingModule({
      imports: [VehicleDetailPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ vin })),
          },
        },
        {
          provide: VehiclePageDataService,
          useValue: { loadVehiclePage },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(VehicleDetailPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('shows not-found message on 404', async () => {
    const loadVehiclePage = vi.fn(() =>
      throwError(() => new HttpErrorResponse({ status: 404, statusText: 'Not Found' })),
    );
    const fixture = await createFixture(loadVehiclePage);

    const errorEl = fixture.nativeElement.querySelector('.vehicle-detail__status--error');
    expect(errorEl?.textContent).toContain('Vehicle not found.');
    expect(fixture.nativeElement.querySelector('.vehicle-detail__skeleton')).toBeNull();
  });

  it('shows load failure message on 500', async () => {
    const loadVehiclePage = vi.fn(() =>
      throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' })),
    );
    const fixture = await createFixture(loadVehiclePage);

    const errorEl = fixture.nativeElement.querySelector('.vehicle-detail__status--error');
    expect(errorEl?.textContent).toContain('Failed to load vehicle.');
    expect(fixture.nativeElement.querySelector('.vehicle-detail__skeleton')).toBeNull();
  });
});
