import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { environment } from '../../environments/environment';
import { map, tap } from 'rxjs/operators';
import {
  RestSourceClientDetails,
  RestSourceClientDetailsList
} from "../models/source-client-details.model";
import { AuthEndpointResponse } from "../models/auth.model";

@Injectable({
  providedIn: 'root'
})
export class SourceClientAuthorizationService {
  private serviceUrl = environment.backendBaseUrl;
  AUTH_ENDPOINT_PARAMS_STORAGE_KEY = 'auth_endpoint_params';
  AUTH_SOURCE_TYPE_STORAGE_KEY = 'auth_source_type';

  constructor(private http: HttpClient) {}

  getDeviceTypes(): Observable<RestSourceClientDetailsList> {
    return this.http.get<RestSourceClientDetailsList>(this.serviceUrl + '/source-clients');
  }

  getAuthorizationEndpoint(
    sourceType: RestSourceClientDetails,
    callbackurl: string
  ): Observable<any> {
    const url =
      this.serviceUrl +
      '/source-clients/' +
      sourceType.sourceType +
      '/auth-endpoint?callbackUrl=' +
      callbackurl;
    return this.http.get<AuthEndpointResponse>(url)
      .pipe(
        tap(authEndpoint => {
          this.storeAuthorizationEndpointParams(authEndpoint.url);
          this.storeSourceType(sourceType);
        })
      );
  }

  storeSourceType(type) {
    localStorage.setItem(this.AUTH_SOURCE_TYPE_STORAGE_KEY, type);
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
