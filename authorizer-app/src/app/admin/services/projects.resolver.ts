import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';

import { ProjectService } from '@app/admin/services/project.service';
import { RadarProject } from "@app/admin/models/radar-entities.model";

@Injectable()
export class ProjectsResolver implements Resolve<RadarProject[]> {

  constructor(private service: ProjectService) {}

  resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<RadarProject[]> | Promise<RadarProject[]> | RadarProject[] {
    return this.service.getProjects();
  }
}
