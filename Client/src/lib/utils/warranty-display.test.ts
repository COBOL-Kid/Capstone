import { describe, expect, it } from 'vitest';

import type { WarrantyCoverageResponse } from '$lib/models/warranty';
import {
  formatWarrantyCoverageLabel,
  formatWarrantyCoverageLabelForSpecs,
  formatWarrantyCoverageStatus,
} from './warranty-display';

function coverage(overrides: Partial<WarrantyCoverageResponse> = {}): WarrantyCoverageResponse {
  return {
    coverageName: 'Warranty - Powertrain (months/miles)',
    coverageValue: '5 years / 60,000 miles',
    estimatedExpirationDate: null,
    expired: false,
    remainingMonths: null,
    remainingMiles: null,
    ...overrides,
  };
}

describe('formatWarrantyCoverageLabel', () => {
  it('strips the Warranty prefix and months/miles suffix', () => {
    expect(formatWarrantyCoverageLabel('Warranty - Powertrain (months/miles)')).toBe('Powertrain');
  });
});

describe('formatWarrantyCoverageLabelForSpecs', () => {
  it('appends warranty when missing', () => {
    expect(formatWarrantyCoverageLabelForSpecs('Warranty - Powertrain (months/miles)')).toBe(
      'Powertrain warranty',
    );
  });

  it('keeps labels already ending in warranty', () => {
    expect(formatWarrantyCoverageLabelForSpecs('Basic warranty')).toBe('Basic warranty');
  });
});

describe('formatWarrantyCoverageStatus', () => {
  it('returns the coverage value without an expiration date', () => {
    expect(formatWarrantyCoverageStatus(coverage())).toBe('5 years / 60,000 miles');
  });

  it('marks expired coverage', () => {
    expect(
      formatWarrantyCoverageStatus(
        coverage({ expired: true, estimatedExpirationDate: '2023-04-01' }),
      ),
    ).toBe('Expired · exp. 2023');
  });

  it('summarizes active coverage with remaining time and miles', () => {
    expect(
      formatWarrantyCoverageStatus(
        coverage({
          estimatedExpirationDate: '2028-04-01',
          remainingMonths: 20,
          remainingMiles: 12345,
        }),
      ),
    ).toBe('Active · 12,345 mi · 20 mo left · exp. 2028');
  });
});
