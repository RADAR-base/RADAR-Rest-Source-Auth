import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';

import { SourceClientService } from "@app/admin/services/source-client.service";
import { RadarSourceClient } from "@app/admin/models/radar-entities.model";

@Injectable()
export class SourceClientsResolver implements Resolve<RadarSourceClient[]> {

  constructor(private service: SourceClientService) {}

  resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<RadarSourceClient[]> | Promise<RadarSourceClient[]> | RadarSourceClient[] {
    return this.service.getSourceClients();
  }
}
