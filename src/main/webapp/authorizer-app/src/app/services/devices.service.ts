import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs/internal/Observable";
import {Device} from "../models/device.model";
import {environment} from "../../environments/environment";

export class AuthorizedDeviceDetails {
  externalUserId: string | null;
  authorized: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class DevicesService {

  private serviceUrl = environment.BACKEND_BASE_URL + "/devices";
  constructor(private http: HttpClient) { }

  getDevices(): Observable<Device[]> {
    console.log("get devices")
    return this.http.get<Device[]>(this.serviceUrl);
  }

  addDevice(device: Device): Observable<any> {
    //post device data
    const body = new HttpParams();

    return this.http.post(this.serviceUrl + 'devices', body);
  }


  delete(deviceId: number): Observable<any> {
    return this.http.delete(`${this.serviceUrl}/${deviceId}`);
  }


}
