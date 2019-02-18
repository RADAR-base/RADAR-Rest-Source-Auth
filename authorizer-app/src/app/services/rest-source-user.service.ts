import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs/internal/Observable";
import {RestSourceUser} from "../models/rest-source-user.model";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class RestSourceUserService {

  private serviceUrl = environment.BACKEND_BASE_URL + "/users";
  constructor(private http: HttpClient) { }

  getAllUsers(): Observable<RestSourceUser[]> {
    return this.http.get<RestSourceUser[]>(this.serviceUrl);
  }

  updateUser(sourceUser: RestSourceUser): Observable<any> {

    return this.http.put(this.serviceUrl + '/' + sourceUser.id, sourceUser);
  }

  addAuthorizedUser(code: string, state: string): Observable<any> {
    const params = new HttpParams()
      .set('code', code)
      .set('state', state);

    return this.http.post(this.serviceUrl ,params);
  }

  getUserById(userId: string): Observable<RestSourceUser> {
    return this.http.get(this.serviceUrl + '/' +userId);
  }

  deleteUser(userId: string): Observable<any> {
    return this.http.delete(this.serviceUrl + '/' + userId);
  }


}
