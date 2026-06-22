import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { VehicleListingsResponse } from '../listings/listings.models';
import {
  AddVehicleResult,
  AddVinRequest,
  AddVinResponse,
  AddVinTrimSelectionRequiredResponse,
  isAddVinTrimSelectionRequiredResponse,
  TrimOptionsResponse,
  UpdateMileageRequest,
  UpdateVehiclePhotoRequest,
  UserVehicleResponse,
  VehicleDashboardResponse,
  VehicleDetailResponse,
  VinErrorMessage,
} from './vin.models';
import { apiConfig } from '../api/api.config';
import { normalizeListResponse } from '../http/normalize-list-response';
import { toFieldErrorMessage } from '../http/http-error.util';

const vinDataUnavailableMessage =
  "We don't have vehicle information for this VIN in our system yet. Try a different VIN or check back later as we add more vehicles.";
const vinNotFoundBackendMessages = new Set([
  'Vehicle not found for VIN',
  'Vehicle data is not available for this VIN',
]);
const onboardingUnavailableMessage =
  "We couldn't load vehicle data for this vehicle right now. Please try again later.";
const genericErrorMessage = 'Unable to add the vehicle. Please try again.';
const emailNotVerifiedMessage =
  'Verify your email before adding vehicles. Use the banner on this page to enter your code.';

@Injectable({
  providedIn: 'root',
})
export class VinService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = apiConfig.vinUrl;

  getUserVehicles(): Observable<UserVehicleResponse[]> {
    return normalizeListResponse(this.http.get<UserVehicleResponse[]>(this.baseUrl));
  }

  getVehicleDetail(vin: string): Observable<VehicleDetailResponse> {
    return this.http.get<VehicleDetailResponse>(`${this.baseUrl}/${vin}`);
  }

  getVehicleDashboard(vin: string): Observable<VehicleDashboardResponse> {
    return this.http.get<VehicleDashboardResponse>(`${this.baseUrl}/${vin}/dashboard`);
  }

  getVehicleListings(vin: string): Observable<VehicleListingsResponse> {
    return this.http.get<VehicleListingsResponse>(`${this.baseUrl}/${vin}/listings`);
  }

  updateMileage(vin: string, request: UpdateMileageRequest): Observable<VehicleDetailResponse> {
    return this.http.patch<VehicleDetailResponse>(`${this.baseUrl}/${vin}/mileage`, request);
  }

  updateSelectedPhoto(
    vin: string,
    request: UpdateVehiclePhotoRequest,
  ): Observable<UserVehicleResponse> {
    return this.http.patch<UserVehicleResponse>(`${this.baseUrl}/${vin}/photo`, request);
  }

  getTrimOptions(year: string, make: string, model: string): Observable<string[]> {
    const params = new HttpParams().set('year', year).set('make', make).set('model', model);
    return this.http
      .get<TrimOptionsResponse>(`${this.baseUrl}/trim-options`, { params })
      .pipe(map((response) => response.trims ?? []));
  }

  addVehicle(request: AddVinRequest): Observable<AddVehicleResult> {
    return this.http
      .post<AddVinResponse | AddVinTrimSelectionRequiredResponse>(this.baseUrl, request)
      .pipe(
        map((body): AddVehicleResult => {
          if (isAddVinTrimSelectionRequiredResponse(body)) {
            return { kind: 'trimSelectionRequired', context: body };
          }
          return { kind: 'completed', response: body };
        }),
        catchError((error) => this.handleAddVehicleError(error)),
      );
  }

  deleteVehicle(vin: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${vin}`);
  }

  private handleAddVehicleError(error: unknown): Observable<never> {
    if (!(error instanceof HttpErrorResponse)) {
      return throwError(() => ({
        message: genericErrorMessage,
        fieldMessages: [],
      }));
    }

    return throwError(() => this.toVinErrorMessage(error));
  }

  private toVinErrorMessage(error: HttpErrorResponse): VinErrorMessage {
    if (error.status === 403 && typeof error.error === 'string') {
      const message = error.error.trim();
      if (message.includes('Email address must be verified')) {
        return { message: emailNotVerifiedMessage, fieldMessages: [] };
      }
    }

    if (this.isVinDataUnavailableError(error)) {
      return { message: vinDataUnavailableMessage, fieldMessages: [] };
    }

    if (error.status >= 500) {
      return { message: onboardingUnavailableMessage, fieldMessages: [] };
    }

    const fieldError = toFieldErrorMessage(error, genericErrorMessage);
    if (fieldError.message !== genericErrorMessage || fieldError.fieldMessages.length > 0) {
      return fieldError;
    }

    return { message: genericErrorMessage, fieldMessages: [] };
  }

  private isVinDataUnavailableError(error: HttpErrorResponse): boolean {
    if (error.status === 404) {
      return true;
    }

    if (typeof error.error !== 'string') {
      return false;
    }

    return vinNotFoundBackendMessages.has(error.error.trim());
  }
}
