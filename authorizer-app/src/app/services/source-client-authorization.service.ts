import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SourceClientAuthorizationService {
  private serviceUrl = environment.backendBaseUrl;
  constructor(private http: HttpClient) {}

  getDeviceTypes(): Observable<any> {
    return this.http.get(this.serviceUrl + '/source-clients/type');
  }

  getSourceClientAuthDetails(sourceType: string): Observable<any> {
    return this.http.get(this.serviceUrl + '/source-clients/' + sourceType);
  }

  getAuthorizationEndpoint(
    sourceType: string,
    callbackurl: string
  ): Observable<any> {
    const url =
      this.serviceUrl +
      '/source-clients/' +
      sourceType +
      '/auth-endpoint?callbackUrl=' +
      callbackurl;
    return this.http.get(url, { responseType: 'text' });
  }
}
