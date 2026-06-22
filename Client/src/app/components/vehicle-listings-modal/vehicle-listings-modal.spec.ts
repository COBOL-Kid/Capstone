import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';

import { VehicleListingsModalComponent } from './vehicle-listings-modal';
import { apiConfig } from '../../core/api/api.config';

describe('VehicleListingsModalComponent', () => {
  let fixture: ComponentFixture<VehicleListingsModalComponent>;
  let httpTesting: HttpTestingController;

  const sampleListing = {
    vin: '1FA6P8JZ1L5552492',
    createdAt: '2026-05-19 00:31:18',
    year: '2020',
    make: 'Ford',
    model: 'Mustang',
    style: 'GT Premium 2dr Coupe',
    price: 179148,
    miles: 8,
    dealer: 'Earth Motorcars',
    city: 'Carrollton',
    state: 'TX',
    zip: '75006',
    primaryImage: 'https://retail.photos.vin/1FA6P8JZ1L5552492-1.jpg',
    vdp: 'https://example.com/vdp',
    carfaxUrl: 'https://www.carfax.com/VehicleHistory/p/Report.cfx?vin=1FA6P8JZ1L5552492',
    used: true,
    cpo: false,
    photoCount: 105,
    latitude: 32.971378,
    longitude: -96.844514,
    history: {
      accidents: false,
      accidentCount: 0,
      oneOwner: false,
      ownerCount: 0,
      usageType: 'Vehicle Use',
    },
  };

  const pageOneResponse = {
    vin: 'JTENU5JR6M5962554',
    year: '2021',
    make: 'Toyota',
    model: '4RUNNER',
    page: 1,
    total: 661,
    listings: [sampleListing],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VehicleListingsModalComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(VehicleListingsModalComponent);
    fixture.componentRef.setInput('vin', 'JTENU5JR6M5962554');
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('shows loading then renders listings', async () => {
    expect(fixture.nativeElement.textContent).toContain('Loading listings…');

    const request = httpTesting.expectOne(
      (req) =>
        req.url === `${apiConfig.vinUrl}/JTENU5JR6M5962554/listings` &&
        req.params.get('page') === '1',
    );
    request.flush(pageOneResponse);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Current market value');
    expect(fixture.nativeElement.textContent).toContain('2021 Toyota 4RUNNER');
    expect(fixture.nativeElement.textContent).toContain('661 comparable listings found');
    expect(fixture.nativeElement.textContent).toContain('2020 Ford Mustang');
    expect(fixture.nativeElement.textContent).toContain('Earth Motorcars');
    expect(fixture.nativeElement.textContent).toContain('View listing');
  });

  it('shows empty state when no listings are returned', async () => {
    const request = httpTesting.expectOne(`${apiConfig.vinUrl}/JTENU5JR6M5962554/listings?page=1`);
    request.flush({
      ...pageOneResponse,
      total: 0,
      listings: [],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'No comparable vehicles are listed right now.',
    );
  });

  it('shows rate limit message for 429 responses', async () => {
    const request = httpTesting.expectOne(`${apiConfig.vinUrl}/JTENU5JR6M5962554/listings?page=1`);
    request.flush('Vehicle listings are temporarily unavailable. Please try again shortly.', {
      status: 429,
      statusText: 'Too Many Requests',
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Vehicle listings are temporarily unavailable. Please try again shortly.',
    );
  });

  it('loads the next page when Next is clicked', async () => {
    const firstRequest = httpTesting.expectOne(
      `${apiConfig.vinUrl}/JTENU5JR6M5962554/listings?page=1`,
    );
    firstRequest.flush(pageOneResponse);
    await fixture.whenStable();
    fixture.detectChanges();

    const nextButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.trim() === 'Next');
    expect(nextButton).toBeDefined();
    nextButton!.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Loading listings…');

    const secondRequest = httpTesting.expectOne(
      (req) =>
        req.url === `${apiConfig.vinUrl}/JTENU5JR6M5962554/listings` &&
        req.params.get('page') === '2',
    );
    secondRequest.flush({
      ...pageOneResponse,
      page: 2,
      listings: [
        {
          ...sampleListing,
          vin: 'SECONDLISTINGVIN12',
          year: '2019',
          make: 'Honda',
          model: 'Civic',
        },
      ],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Page 2');
    expect(fixture.nativeElement.textContent).toContain('2019 Honda Civic');
  });

  it('does not close while loading', async () => {
    const closeSpy = vi.fn();
    fixture.componentInstance.close.subscribe(closeSpy);

    fixture.componentInstance['requestClose']();
    expect(closeSpy).not.toHaveBeenCalled();

    const request = httpTesting.expectOne(`${apiConfig.vinUrl}/JTENU5JR6M5962554/listings?page=1`);
    request.flush(pageOneResponse);
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance['requestClose']();
    expect(closeSpy).toHaveBeenCalledTimes(1);
  });
});
