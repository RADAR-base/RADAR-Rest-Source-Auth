import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import {ProjectService} from './project.service';

@Injectable()
export class ProjectsResolver implements Resolve<any> {
  constructor(private service: ProjectService) {}

  resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<any> | Promise<any> | any {
    return this.service.getProjects();
  }
}
