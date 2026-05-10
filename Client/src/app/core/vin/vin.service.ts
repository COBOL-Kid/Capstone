import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AddVinRequest, AddVinResponse, UserVehicleResponse } from './vin.models';

@Injectable({
  providedIn: 'root',
})
export class VinService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/vin';

  getUserVehicles(): Observable<UserVehicleResponse[]> {
    return this.http.get<UserVehicleResponse[]>(this.baseUrl);
  }

  addVehicle(request: AddVinRequest): Observable<AddVinResponse> {
    return this.http.post<AddVinResponse>(this.baseUrl, request);
  }

  deleteVehicle(vin: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${vin}`);
  }
}
