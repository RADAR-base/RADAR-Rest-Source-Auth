import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs/internal/Observable';
import {environment} from '../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class SourceClientAuthorizationService {
  private serviceUrl = environment.BACKEND_BASE_URL;
  constructor(private http: HttpClient) { }

  getDeviceTypes(): Observable<any> {
    return this.http.get(this.serviceUrl + '/source-clients/type');
  }

  getSourceClientAuthDetails(sourceType: string): Observable<any> {
    return this.http.get(this.serviceUrl + '/source-clients/' + sourceType);
  }
}

