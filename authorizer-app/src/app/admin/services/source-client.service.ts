import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from "rxjs";
import {map, shareReplay} from 'rxjs/operators';

import {RadarSourceClient} from "@app/admin/models/radar-entities.model";

import { environment } from '@environments/environment';

@Injectable()
export class SourceClientService {

  constructor(private http: HttpClient) {}

  getSourceClients(): Observable<RadarSourceClient[]> {
    return this.http.get<{sourceClients: RadarSourceClient[]}>(
      environment.backendBaseUrl + '/source-clients'
    ).pipe(
      map(result => result.sourceClients),
      shareReplay()
    );
  }
}
