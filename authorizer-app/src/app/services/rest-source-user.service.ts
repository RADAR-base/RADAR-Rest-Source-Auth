import { HttpClient, HttpParams } from '@angular/common/http';

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RadarProject } from '../models/rest-source-project.model';
import { RequestTokenPayload } from '../models/auth.model';
import { RestSourceUser } from '../models/rest-source-user.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RestSourceUserService {
  private serviceUrl = environment.backendBaseUrl + '/users';
  AUTH_ENDPOINT_PARAMS_STORAGE_KEY = 'auth_endpoint_params';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<RestSourceUser[]> {
    return this.http.get<RestSourceUser[]>(this.serviceUrl);
  }

  getAllUsersOfProject(projectId: string): Observable<RestSourceUser[]> {
    return this.http.get<RestSourceUser[]>(
      environment.backendBaseUrl + '/users?project-id=' + projectId
    );
  }

  getAllProjects(): Observable<RadarProject[]> {
    return this.http.get<RadarProject[]>(
      environment.backendBaseUrl + '/projects'
    );
  }

  getAllSubjectsOfProjects(projectId: string): Observable<RadarProject[]> {
    return this.http.get<RadarProject[]>(
      environment.backendBaseUrl + '/projects/' + projectId + '/users'
    );
  }

  updateUser(sourceUser: RestSourceUser): Observable<any> {
    return this.http.post(this.serviceUrl + '/' + sourceUser.id, sourceUser);
  }

  addAuthorizedUser(payload: RequestTokenPayload): Observable<any> {
    const sourceType = this.getSourceTypeFromAuthPayload(payload);
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

  getSourceTypeFromAuthPayload(payload: RequestTokenPayload) {
    return payload.code ? 'Fitbit' : 'Garmin';
  }
}
