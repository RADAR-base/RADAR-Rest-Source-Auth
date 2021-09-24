import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import {SourceService} from "./source.service";

@Injectable()
export class SourceTypesResolver implements Resolve<any> {
  constructor(private service: SourceService) {}

  resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<any> | Promise<any> | any {
    return this.service.getDeviceTypes();
  }
}
