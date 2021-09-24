import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Observable } from "rxjs";
import { RestSourceUser } from "../models/rest-source-user.model";

@Injectable()
export class SubjectService {
  constructor(private http: HttpClient) {}

  getSubjectsOfProjects(projectId: string): Observable<RestSourceUser[]> {
    console.log('getAllSubjectsOfProjects');
    const url = encodeURI(
      environment.backendBaseUrl + '/projects/' + projectId + '/users'
    );
    return this.http.get<{users: RestSourceUser[]}>(url).pipe(
      map(result => result.users),
      shareReplay()
    );
  }
}
