import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, Observable, throwError } from 'rxjs';
import {
  AddVinRequest,
  AddVinResponse,
  UpdateMileageRequest,
  UserVehicleResponse,
  ValidationErrorResponse,
  VehicleDetailResponse,
  VinErrorMessage,
} from './vin.models';
import { apiConfig } from '../api/api.config';

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
    return this.http.get<UserVehicleResponse[]>(this.baseUrl);
  }

  getVehicleDetail(vin: string): Observable<VehicleDetailResponse> {
    return this.http.get<VehicleDetailResponse>(`${this.baseUrl}/${vin}`);
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
    if (this.isValidationErrorResponse(error.error)) {
      return {
        message: error.error.message,
        fieldMessages: error.error.errors.map((fieldError) => fieldError.message),
      };
    }

    if (error.status === 404) {
      return { message: vinNotFoundMessage, fieldMessages: [] };
    }

    if (error.status >= 500) {
      return { message: serverErrorMessage, fieldMessages: [] };
    }

    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return { message: error.error, fieldMessages: [] };
    }

    return { message: genericErrorMessage, fieldMessages: [] };
  }

  private isValidationErrorResponse(value: unknown): value is ValidationErrorResponse {
    return (
      typeof value === 'object' &&
      value !== null &&
      'message' in value &&
      'errors' in value &&
      Array.isArray((value as ValidationErrorResponse).errors)
    );
  }
}
