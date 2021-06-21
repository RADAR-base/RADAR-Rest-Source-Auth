import { HttpClient } from '@angular/common/http';
import {
  RestSourceUser,
  RestSourceUsers
} from '../models/rest-source-user.model';

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RadarProject } from '../models/rest-source-project.model';
import { RequestTokenPayload } from '../models/auth.model';
import { createRequestOption } from '../utilities/request.util';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RestSourceUserService {
  private serviceUrl = environment.backendBaseUrl + '/users';
  AUTH_ENDPOINT_PARAMS_STORAGE_KEY = 'auth_endpoint_params';
  AUTH_SOURCE_TYPE_STORAGE_KEY = 'auth_source_type';

  constructor(private http: HttpClient) {}

  getAllUsersOfProject(
    projectId: string,
    req?: any
  ): Observable<RestSourceUsers> {
    let params = createRequestOption(req);
    params = params.set('project-id', projectId);
    return this.http.get<RestSourceUsers>(environment.backendBaseUrl + '/users', { params });
  }

  getAllProjects(): Observable<RadarProject[]> {
    return this.http.get<RadarProject[]>(
      environment.backendBaseUrl + '/projects'
    );
  }

  getAllSubjectsOfProjects(projectId: string): Observable<RestSourceUser[]> {
    const url = encodeURI(
      environment.backendBaseUrl + '/projects/' + projectId + '/users'
    );
    return this.http.get<RestSourceUser[]>(url);
  }

  updateUser(sourceUser: RestSourceUser): Observable<any> {
    return this.http.post(this.serviceUrl + '/' + sourceUser.id, sourceUser);
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
    return this.http.get(this.serviceUrl + '/' + userId);
  }

  deleteUser(userId: string): Observable<any> {
    return this.http.delete(this.serviceUrl + '/' + userId);
  }

  resetUser(user: RestSourceUser): Observable<any> {
    return this.http.post(this.serviceUrl + '/' + user.id + '/reset', user);
  }
}
