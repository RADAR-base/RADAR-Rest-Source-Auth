import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from "rxjs";
import { map, shareReplay } from 'rxjs/operators';

import { RadarSubject } from "@app/admin/models/radar-entities.model";

import { environment } from '@environments/environment';

@Injectable()
export class SubjectService {

  constructor(private http: HttpClient) {}

  getSubjectsOfProject(projectId: string): Observable<RadarSubject[]> {
    const url = encodeURI(
      environment.backendBaseUrl + '/projects/' + projectId + '/users'
    );
    return this.http.get<{users: RadarSubject[]}>(url).pipe(
      map(result => result.users),
      shareReplay()
    );
  }
}
