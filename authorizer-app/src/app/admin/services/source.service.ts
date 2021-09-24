import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { map } from 'rxjs/operators';
import {environment} from '../../../environments/environment';

@Injectable()
export class SourceService {
  private serviceUrl = environment.backendBaseUrl;
  AUTH_ENDPOINT_PARAMS_STORAGE_KEY = 'auth_endpoint_params';
  AUTH_SOURCE_TYPE_STORAGE_KEY = 'auth_source_type';

  constructor(private http: HttpClient) {}

  getDeviceTypes(): Observable<any[]> {
    return this.http.get<{sourceClients: any[]}>(this.serviceUrl + '/source-clients').pipe(
      map(result => result.sourceClients)
    );
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
        this.storeSourceType(sourceType);
        return url;
      })
    );
  }

  storeSourceType(type: string) {
    localStorage.setItem(this.AUTH_SOURCE_TYPE_STORAGE_KEY, type);
  }

  storeAuthorizationEndpointParams(url: string) {
    const params = this.getJsonFromUrl(url);
    localStorage.setItem(
      this.AUTH_ENDPOINT_PARAMS_STORAGE_KEY,
      JSON.stringify(params)
    );
  }

  getJsonFromUrl(url: string) {
    const query = url.split('?')[1];
    const result: any = {};
    query.split('&').forEach(function(part) {
      let item = part.split('=');
      result[item[0]] = decodeURIComponent(item[1]);
    });
    return result;
  }
}
