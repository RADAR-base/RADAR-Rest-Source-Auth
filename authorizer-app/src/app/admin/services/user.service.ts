import { HttpClient } from '@angular/common/http';
import {
  AuthorizeRequest,
  RegistrationCreateRequest, RegistrationRequest, RegistrationResponse,
  RestSourceUser, RestSourceUserRequest, RestSourceUserResponse,
  RestSourceUsers
} from '../models/rest-source-user.model';

import { Injectable } from '@angular/core';
import {Observable} from 'rxjs';
import {environment} from '../../../environments/environment';
import {createRequestOption} from '../../shared/utilities/request.util';
import {RequestTokenPayload} from '../../auth/models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private serviceUrl = environment.backendBaseUrl;
  AUTH_ENDPOINT_PARAMS_STORAGE_KEY = 'auth_endpoint_params';
  AUTH_SOURCE_TYPE_STORAGE_KEY = 'auth_source_type';

  constructor(private http: HttpClient) {}

  getUsersOfProject(
    projectId: string,
    req?: any
  ): Observable<RestSourceUsers> {
    let params = createRequestOption(req);
    params = params.set('project-id', projectId);
    return this.http.get<RestSourceUsers>(environment.backendBaseUrl + '/users', { params });
  }

  updateUser(sourceUser: RestSourceUser): Observable<any> {
    return this.http.post(this.serviceUrl + '/users/' + sourceUser.id, sourceUser);
  }

  addAuthorizedUser(payload: RequestTokenPayload): Observable<any> {
    const sourceType = localStorage.getItem(this.AUTH_SOURCE_TYPE_STORAGE_KEY);
    const redirectParams = JSON.parse(
      <string>localStorage.getItem(this.AUTH_ENDPOINT_PARAMS_STORAGE_KEY)
    );
    const newPayload = Object.assign(
      {},
      payload,
      { sourceType },
      redirectParams
    );
    return this.http.post(this.serviceUrl, newPayload, {
      responseType: 'json'
    });
  }

  getUserById(userId: string): Observable<RestSourceUser> {
    return this.http.get<RestSourceUser>(this.serviceUrl + '/users/' + userId);
  }

  deleteUser(userId: string): Observable<any> {
    return this.http.delete(this.serviceUrl + '/users/' + userId);
  }

  resetUser(user: RestSourceUser): Observable<any> {
    // return this.http.post(this.serviceUrl + '/users/' + user.id + '/reset', user);
    return this.http.post(this.serviceUrl + '/users/' + user.id, user);
  }

  // --
  createUser(restSourceUserRequest: RestSourceUserRequest): Observable<RestSourceUserResponse> {
    return this.http.post<RestSourceUserResponse>(this.serviceUrl + '/users', restSourceUserRequest);
  }

  registerUser(registrationCreateRequest: RegistrationCreateRequest): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(this.serviceUrl + '/registrations', registrationCreateRequest);
  }

  authorizeUser(authorizeRequest: AuthorizeRequest, state: string): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(this.serviceUrl + '/registrations/' + state + '/authorize', authorizeRequest);
  }

  getAuthEndpointUrl(registrationRequest: RegistrationRequest, token: string): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(this.serviceUrl + '/registrations/' + token, registrationRequest);
  }
}
