import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { RadarProject } from '../models/rest-source-project.model';
import { environment } from '../../../environments/environment';

@Injectable()
export class ProjectService {

  constructor(private http: HttpClient) {}

  getProjects(): Observable<RadarProject[]> {
    return this.http.get<{projects: RadarProject[]}>(
      environment.backendBaseUrl + '/projects'
    ).pipe(
      map(result => result.projects)
    );
  }
}
