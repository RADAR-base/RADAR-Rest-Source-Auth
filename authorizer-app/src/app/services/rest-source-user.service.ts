import { HttpClient } from '@angular/common/http';
import {
  AuthorizeRequest,
  RegistrationCreateRequest, RegistrationRequest, RegistrationResponse,
  RestSourceUser, RestSourceUserRequest, RestSourceUserResponse,
  RestSourceUsers
} from '../models/rest-source-user.model';

import { Injectable } from '@angular/core';
import {Observable, throwError} from 'rxjs';
import { RadarProject } from '../models/rest-source-project.model';
import { RequestTokenPayload } from '../models/auth.model';
import { createRequestOption } from '../utilities/request.util';
import { environment } from '../../environments/environment';
import {delay, map, shareReplay} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class RestSourceUserService {
  private serviceUrl = environment.backendBaseUrl;
  AUTH_ENDPOINT_PARAMS_STORAGE_KEY = 'auth_endpoint_params';
  AUTH_SOURCE_TYPE_STORAGE_KEY = 'auth_source_type';

  constructor(private http: HttpClient) {}

  getAllProjects(): Observable<RadarProject[]> {
    return this.http.get<{projects: RadarProject[]}>(
      environment.backendBaseUrl + '/projects'
    ).pipe(
      map(result => result.projects)
    );
  }

  getAllUsersOfProject(
    projectId: string,
    req?: any
  ): Observable<RestSourceUsers> {
    let params = createRequestOption(req);
    params = params.set('project-id', projectId);
    return this.http.get<RestSourceUsers>(environment.backendBaseUrl + '/users', { params });
  }


  getAllSubjectsOfProjects(projectId: string): Observable<RestSourceUser[]> {
    console.log('getAllSubjectsOfProjects');
    const url = encodeURI(
      environment.backendBaseUrl + '/projects/' + projectId + '/users'
    );
    return this.http.get<{users: RestSourceUser[]}>(url).pipe(
      map(result => result.users),
      shareReplay()
    );
  }

  updateUser(sourceUser: RestSourceUser): Observable<any> {
    return this.http.post(this.serviceUrl + '/users/' + sourceUser.id, sourceUser);
  }

  addAuthorizedUser(payload: RequestTokenPayload): Observable<any> {
    const sourceType = localStorage.getItem(this.AUTH_SOURCE_TYPE_STORAGE_KEY);
    const redirectParams = JSON.parse(
      localStorage.getItem(this.AUTH_ENDPOINT_PARAMS_STORAGE_KEY)
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
    return this.http.get(this.serviceUrl + '/users/' + userId);
  }

  deleteUser(userId: string): Observable<any> {
    return this.http.delete(this.serviceUrl + '/users/' + userId);
  }

  resetUser(user: RestSourceUser): Observable<any> {
    return this.http.post(this.serviceUrl + '/users/' + user.id + '/reset', user);
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
