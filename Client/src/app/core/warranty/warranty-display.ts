import { WarrantyCoverageResponse } from './warranty.models';

export function formatWarrantyCoverageLabel(coverageName: string): string {
  return coverageName
    .replace(/^Warranty\s*-\s*/i, '')
    .replace(/\s*\(months\/miles\)\s*$/i, '')
    .trim();
}

export function formatWarrantyCoverageLabelForSpecs(coverageName: string): string {
  const label = formatWarrantyCoverageLabel(coverageName);
  if (/warranty$/i.test(label)) {
    return label;
  }
  return `${label} warranty`;
}

export function formatWarrantyCoverageStatus(coverage: WarrantyCoverageResponse): string {
  if (!coverage.estimatedExpirationDate) {
    return coverage.coverageValue;
  }

  const expirationYear = coverage.estimatedExpirationDate.slice(0, 4);
  if (coverage.expired) {
    return `Expired · exp. ${expirationYear}`;
  }

  const parts = ['Active'];
  if (coverage.remainingMiles != null) {
    parts.push(`${coverage.remainingMiles.toLocaleString('en-US')} mi`);
  }
  if (coverage.remainingMonths != null) {
    parts.push(`${coverage.remainingMonths} mo left`);
  }
  parts.push(`exp. ${expirationYear}`);
  return parts.join(' · ');
}
