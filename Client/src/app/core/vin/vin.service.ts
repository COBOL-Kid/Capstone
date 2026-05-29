import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, Observable, throwError } from 'rxjs';
import {
  AddVinRequest,
  AddVinResponse,
  UpdateMileageRequest,
  UserVehicleResponse,
  VehicleDashboardResponse,
  VehicleDetailResponse,
  VinErrorMessage,
} from './vin.models';
import { apiConfig } from '../api/api.config';
import { normalizeListResponse } from '../http/normalize-list-response';
import { toFieldErrorMessage } from '../http/http-error.util';

const vinNotFoundMessage =
  "We couldn't find a vehicle for that VIN. Check the number and try again.";
const serverErrorMessage = 'Something went wrong on our end. Please try again later.';
const genericErrorMessage = 'Unable to add the vehicle. Please try again.';

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

  updateMileage(vin: string, request: UpdateMileageRequest): Observable<VehicleDetailResponse> {
    return this.http.patch<VehicleDetailResponse>(`${this.baseUrl}/${vin}/mileage`, request);
  }

  addVehicle(request: AddVinRequest): Observable<AddVinResponse> {
    return this.http
      .post<AddVinResponse>(this.baseUrl, request)
      .pipe(catchError((error) => this.handleAddVehicleError(error)));
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
    if (error.status === 404) {
      return { message: vinNotFoundMessage, fieldMessages: [] };
    }

    if (error.status >= 500) {
      return { message: serverErrorMessage, fieldMessages: [] };
    }

    const fieldError = toFieldErrorMessage(error, genericErrorMessage);
    if (fieldError.message !== genericErrorMessage || fieldError.fieldMessages.length > 0) {
      return fieldError;
    }

    return { message: genericErrorMessage, fieldMessages: [] };
  }
}
