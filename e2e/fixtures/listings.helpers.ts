import { SEEDED_VINS } from "./test-data";

export interface VehicleListingMock {
  vin: string;
  createdAt: string | null;
  year: string | null;
  make: string | null;
  model: string | null;
  style: string | null;
  price: number | null;
  miles: number | null;
  dealer: string | null;
  city: string | null;
  state: string | null;
  zip: string | null;
  primaryImage: string | null;
  vdp: string | null;
  carfaxUrl: string | null;
  used: boolean | null;
  cpo: boolean | null;
  photoCount: number | null;
  latitude: number | null;
  longitude: number | null;
  history: {
    accidents: boolean | null;
    accidentCount: number | null;
    oneOwner: boolean | null;
    ownerCount: number | null;
    usageType: string | null;
  } | null;
}

export interface VehicleListingsPricingSummaryMock {
  minPrice: number | null;
  maxPrice: number | null;
  averagePrice: number | null;
  pricedListingCount: number;
}

export interface VehicleListingsResponseMock {
  vin: string;
  year: string;
  make: string;
  model: string;
  total: number | null;
  pricingSummary: VehicleListingsPricingSummaryMock;
  listings: VehicleListingMock[];
}

export const SAMPLE_LISTING: VehicleListingMock = {
  vin: "1FA6P8JZ1L5552492",
  createdAt: "2026-05-19 00:31:18",
  year: "2020",
  make: "Ford",
  model: "Mustang",
  style: "GT Premium 2dr Coupe",
  price: 179148,
  miles: 8,
  dealer: "Earth Motorcars",
  city: "Carrollton",
  state: "TX",
  zip: "75006",
  primaryImage: "https://retail.photos.vin/1FA6P8JZ1L5552492-1.jpg",
  vdp: "https://example.com/vdp",
  carfaxUrl:
    "https://www.carfax.com/VehicleHistory/p/Report.cfx?vin=1FA6P8JZ1L5552492",
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
    usageType: "Vehicle Use",
  },
};

export function buildListingsMockResponse(
  overrides: Partial<VehicleListingsResponseMock> = {},
): VehicleListingsResponseMock {
  return {
    vin: SEEDED_VINS.camry,
    year: "2020",
    make: "Toyota",
    model: "Camry",
    total: 661,
    pricingSummary: {
      minPrice: 179148,
      maxPrice: 179148,
      averagePrice: 179148,
      pricedListingCount: 1,
    },
    listings: [SAMPLE_LISTING],
    ...overrides,
  };
}

export function camryListingsUrl(): string {
  return `**/api/vin/${SEEDED_VINS.camry}/listings**`;
}
