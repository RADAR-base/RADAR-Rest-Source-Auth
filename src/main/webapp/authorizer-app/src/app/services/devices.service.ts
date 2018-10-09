import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/internal/Observable";
import {Device} from "../models/device.model";

@Injectable({
  providedIn: 'root'
})
export class DevicesService {

  private serviceUrl = "http://localhost:8080/devices";
  constructor(private http: HttpClient) { }

  getDevices(): Observable<Device[]> {
    return this.http.get<Device[]>(this.serviceUrl);
  }
}
