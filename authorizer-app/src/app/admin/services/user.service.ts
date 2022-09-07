import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AuthorizeRequest,
  RegistrationCreateRequest, RegistrationRequest, RegistrationResponse,
  RestSourceUser, RestSourceUserRequest, RestSourceUserResponse,
  RestSourceUsers
} from '@app/admin/models/rest-source-user.model';

import {environment} from '@environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  AUTH_ENDPOINT_PARAMS_STORAGE_KEY = 'auth_endpoint_params';

  constructor(private http: HttpClient) {}

  getUsersOfProject(projectId: string): Observable<RestSourceUsers> {
    let params = new HttpParams()
      .set('project-id', projectId);
    return this.http.get<RestSourceUsers>(
      environment.backendBaseUrl + '/users',
      { params }
    );
  }

  createUser(restSourceUserRequest: RestSourceUserRequest): Observable<RestSourceUserResponse> {
    return this.http.post<RestSourceUserResponse>(
      environment.backendBaseUrl + '/users',
      restSourceUserRequest
    );
  }

  registerUser(registrationCreateRequest: RegistrationCreateRequest): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(
      environment.backendBaseUrl + '/registrations',
      registrationCreateRequest
    );
  }

  authorizeUser(authorizeRequest: AuthorizeRequest, state: string): Observable<RegistrationResponse> {
    const url = encodeURI(
      environment.backendBaseUrl + '/registrations/' + state + '/authorize'
    );
    return this.http.post<RegistrationResponse>(url, authorizeRequest);
  }

  getAuthEndpointUrl(registrationRequest: RegistrationRequest, token: string): Observable<RegistrationResponse> {
    const url = encodeURI(
      environment.backendBaseUrl + '/registrations/' + token
    );
    return this.http.post<RegistrationResponse>(url, registrationRequest);
  }

  updateUser(user: RestSourceUser): Observable<any> {
    const url = encodeURI(
      environment.backendBaseUrl + '/users/' + user.id
    );
    return this.http.post(url, user);
  }

  deleteUser(userId: string): Observable<any> {
    const url = encodeURI(
      environment.backendBaseUrl + '/users/' + userId
    );
    return this.http.delete(url);
  }

  storeUserAuthParams(url: string) {
    const params = this.getJsonFromUrl(url);
    localStorage.setItem(
      this.AUTH_ENDPOINT_PARAMS_STORAGE_KEY,
      JSON.stringify(params)
    );
  }

  getUserAuthParams() {
    const params = localStorage.getItem(this.AUTH_ENDPOINT_PARAMS_STORAGE_KEY);
    return params ? JSON.parse(params) : {};
  }


  getJsonFromUrl(url: string) {
    const query = url.split('?')[1];
    let result: any = {};
    query.split('&').forEach(function(part) {
      let item = part.split('=');
      result[item[0]] = decodeURIComponent(item[1]);
    });
    return result;
  }
}
