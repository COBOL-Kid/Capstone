import { TestBed } from '@angular/core/testing';

import { MaintenanceCostsModalComponent } from './maintenance-costs-modal';
import { MiscMaintenanceCostResponse } from '../../core/maintenance/maintenance.models';

describe('MaintenanceCostsModalComponent', () => {
  const costs: MiscMaintenanceCostResponse[] = [
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
    {
      miscMaintCostId: 2,
      maintTitle: 'Brake Pad Replacement',
      maintDesc: 'Replace front brake pads and resurface rotors',
      independentAvg: 275,
      independentHigh: 380,
      independentLow: 210,
      dealerAvg: 425,
      dealerHigh: 550,
      dealerLow: 340,
    },
  ];

  function configure(inputCosts: MiscMaintenanceCostResponse[] = costs) {
    TestBed.configureTestingModule({
      imports: [MaintenanceCostsModalComponent],
    });

    const fixture = TestBed.createComponent(MaintenanceCostsModalComponent);
    fixture.componentRef.setInput('costs', inputCosts);
    fixture.detectChanges();

    return { fixture };
  }

  it('renders maintenance cost items from input', () => {
    const { fixture } = configure();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Oil Change');
    expect(text).toContain('Brake Pad Replacement');
    expect(text).toContain('$45 – $95');
    expect(text).toContain('$85 – $145');
  });

  it('shows all items when search is empty', () => {
    const { fixture } = configure();
    const items = fixture.nativeElement.querySelectorAll('.maintenance-costs-modal__item');
    expect(items.length).toBe(2);
  });

  it('filters items by title and description', () => {
    const { fixture } = configure();
    const searchInput = fixture.nativeElement.querySelector(
      'input[type="search"]',
    ) as HTMLInputElement;
    searchInput.value = 'brake';
    searchInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Brake Pad Replacement');
    expect(text).not.toContain('Oil Change');

    searchInput.value = 'engine oil';
    searchInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Oil Change');
    expect(fixture.nativeElement.textContent).not.toContain('Brake Pad Replacement');
  });

  it('shows no-match message when search has no results', () => {
    const { fixture } = configure();
    const searchInput = fixture.nativeElement.querySelector(
      'input[type="search"]',
    ) as HTMLInputElement;
    searchInput.value = 'transmission';
    searchInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No items match your search.');
  });
});
