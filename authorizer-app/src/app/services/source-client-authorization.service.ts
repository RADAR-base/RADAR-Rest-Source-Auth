import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { environment } from '../../environments/environment';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class SourceClientAuthorizationService {
  private serviceUrl = environment.backendBaseUrl;
  AUTH_ENDPOINT_PARAMS_STORAGE_KEY = 'auth_endpoint_params';

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
    return this.http.get(url, { responseType: 'text' }).pipe(
      map(url => {
        this.storeAuthorizationEndpointParams(url);
        return url;
      })
    );
  }

  storeAuthorizationEndpointParams(url) {
    const params = this.getJsonFromUrl(url);
    localStorage.setItem(
      this.AUTH_ENDPOINT_PARAMS_STORAGE_KEY,
      JSON.stringify(params)
    );
  }

  getJsonFromUrl(url) {
    const query = url.split('?')[1];
    const result = {};
    query.split('&').forEach(function(part) {
      let item = part.split('=');
      result[item[0]] = decodeURIComponent(item[1]);
    });
    return result;
  }
}
