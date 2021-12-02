import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {map, shareReplay} from 'rxjs/operators';

import { RadarProject } from '@app/admin/models/radar-entities.model';

import { environment } from '@environments/environment';

@Injectable()
export class ProjectService {

  constructor(private http: HttpClient) {}

  getProjects(): Observable<RadarProject[]> {
    return this.http.get<{projects: RadarProject[]}>(
      environment.backendBaseUrl + '/projects'
    ).pipe(
      map(result => result.projects),
      shareReplay()
    );
  }
}
