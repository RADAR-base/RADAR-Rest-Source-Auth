import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {RestSourceUser} from '../models/rest-source-user.model';
import {environment} from '../../environments/environment';
import {RadarProject} from "../models/rest-source-project.model";

@Injectable({
  providedIn: 'root'
})
export class RestSourceUserService {

  private serviceUrl = environment.backendBaseUrl + '/users';

  constructor(private http: HttpClient) {
  }

  getAllUsers(): Observable<RestSourceUser[]> {
    return this.http.get<RestSourceUser[]>(this.serviceUrl);
  }

  getAllUsersOfProject(projectId: string): Observable<RestSourceUser[]> {
    return this.http.get<RestSourceUser[]>(environment.backendBaseUrl + '/users?project-id='+projectId);
  }

  getAllProjects(): Observable<RadarProject[]> {
    return this.http.get<RadarProject[]>(environment.backendBaseUrl + '/projects');
  }

  getAllSubjectsOfProjects(projectId: string): Observable<RestSourceUser[]> {
    return this.http.get<RestSourceUser[]>(environment.backendBaseUrl + '/projects/' + projectId + '/users');
  }

  updateUser(sourceUser: RestSourceUser): Observable<any> {
    return this.http.post(this.serviceUrl + '/' + sourceUser.id, sourceUser);
  }

  addAuthorizedUser(code: string, state: string): Observable<any> {
    const params = new HttpParams()
    .set('code', code)
    .set('state', state);

    return this.http.post(this.serviceUrl, params);
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
