import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs/internal/Observable";
import {Device} from "../models/device.model";
import {environment} from "../../environments/environment";


@Injectable({
  providedIn: 'root'
})
export class DeviceAuthorizationService {
  private serviceUrl = environment.BACKEND_BASE_URL;
  constructor(private http: HttpClient) { }

  getDeviceTypes(): Observable<any> {
    console.log("get device types");
    return this.http.get(this.serviceUrl + '/device-clients/device-type');
  }

  getDeviceClientAuthDetails(deviceType: string): Observable<any> {
    console.log("get device types");
    return this.http.get(this.serviceUrl + '/device-clients/' + deviceType);
  }


  authorize(code: string, state: string): Observable<any> {
    console.log("send authorize request")
    const params = new HttpParams()
      .set('code', code)
      .set('state', state);

    return this.http.get(this.serviceUrl + '/callback',{
      params: params,
    });
  }
}

