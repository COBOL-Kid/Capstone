export interface VehicleListingHistoryResponse {
  accidents: boolean | null;
  accidentCount: number | null;
  oneOwner: boolean | null;
  ownerCount: number | null;
  usageType: string | null;
}

export interface VehicleListingResponse {
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
  history: VehicleListingHistoryResponse | null;
}

export interface VehicleListingsResponse {
  vin: string;
  year: string;
  make: string;
  model: string;
  page: number;
  total: number | null;
  listings: VehicleListingResponse[];
}
