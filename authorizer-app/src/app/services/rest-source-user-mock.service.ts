import { HttpClient } from '@angular/common/http';
import {
  AuthorizeRequest,
  RegistrationCreateRequest, RegistrationRequest, RegistrationResponse,
  RestSourceUser, RestSourceUserRequest, RestSourceUserResponse,
  RestSourceUsers
} from '../models/rest-source-user.model';

import { Injectable } from '@angular/core';
import {Observable, throwError} from 'rxjs';
import { RadarProject } from '../models/rest-source-project.model';
import { RequestTokenPayload } from '../models/auth.model';
import { createRequestOption } from '../utilities/request.util';
import { environment } from '../../environments/environment';
import {delay, map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class RestSourceUserMockService {
  private serviceUrl = environment.backendBaseUrl + '/users';
  AUTH_ENDPOINT_PARAMS_STORAGE_KEY = 'auth_endpoint_params';
  AUTH_SOURCE_TYPE_STORAGE_KEY = 'auth_source_type';

  constructor(private http: HttpClient) {}

  getAllProjects(): Observable<RadarProject[]> {
    const projects: RadarProject[] = [
      {
        id: 'radar',
        version: '1',
        description: 'radar',
        location: 'Utrecht',
      },
      {
        id: 'playground',
        version: '1',
        description: 'playground',
        location: 'Amsterdam',
      },
    ];
    // return throwError({message: 'Server error. Couldn\'t create user'}).pipe(
    //   delay(5000)
    // );
    return new Observable((observer: { next: (arg0: RadarProject[]) => void; }) => {
      observer.next(projects);
    }).pipe(
      delay(1000)
    );
  }

  getAllUsersOfProject(
    projectId: string,
    req?: any
  ): Observable<RestSourceUsers> {
    const users: any = {
      users: [
        {
          endDate: "2021-01-20T11:00:00Z",
          externalId: "Subject 006",
          hasValidToken: false,
          id: "63",
          isAuthorized: true,
          projectId: "CHDR-TEST-001",
          serviceUserId: "20965595",
          sourceId: "7eebcee4-7fea-41fd-bbe8-4b7bc9d536e8",
          sourceType: "Withings",
          startDate: "2021-01-16T11:00:00Z",
          timesReset: 0,
          userId: "ab8b020c-206c-49ef-b526-30cd13ac31b2"
        },
        {
          endDate: "2021-01-18T11:00:00Z",
          externalId: "Subject 005",
          hasValidToken: false,
          id: "62",
          isAuthorized: false,
          projectId: "CHDR-TEST-001",
          serviceUserId: "20730926",
          sourceId: "3a440d1f-5220-4d23-8a0f-134a6f7d0c20",
          sourceType: "Withings",
          startDate: "2021-01-15T11:00:00Z",
          timesReset: 0,
          userId: "0e640a3d-8a30-4d36-acb4-a58baf6873b7"
        }
      ],
      metadata: {
        pageNumber: 1,
        pageSize: 50,
        totalElements: 0,
        offset: 0
      }
    };
    // return throwError({message: 'Server error. Couldn\'t create user'}).pipe(
    //   delay(5000)
    // );
    return new Observable((observer: { next: (arg0: any) => void; }) => {
      observer.next(users);
    }).pipe(
      // map(resp => resp.users),
      delay(1000)
    );

    // let params = createRequestOption(req);
    // params = params.set('project-id', projectId);
    // return this.http.get<RestSourceUsers>(environment.backendBaseUrl + '/users', { params });
  }


  getAllSubjectsOfProjects(projectId: string): Observable<RestSourceUser[]> {
    // const url = encodeURI(
    //   environment.backendBaseUrl + '/projects/' + projectId + '/users'
    // );
    // return this.http.get<RestSourceUser[]>(url);
    const users: any = {
      users: [
        {
          id: 'b5baaf18-e707-401b-b088-aeaba77ea5e9',
          projectId: 'playground',
          externalId: 'joris',
          status: 'ACTIVATED'
        },
        {
          id: '17e719de-26e3-4d3b-bf53-f981f9f63faa',
          projectId: 'playground',
          externalId: 'joris-demo',
          status: 'ACTIVATED'
        }
      ]
    };
    // return throwError({message: 'Server error. Couldn\'t create user'}).pipe(
    //   delay(5000)
    // );
    return new Observable((observer: { next: (arg0: any) => void; }) => {
      observer.next(users);
    }).pipe(
      map(resp => resp.users),
      delay(1000)
    );
  }

  updateUser(sourceUser: RestSourceUser): Observable<any> {
    return this.http.post(this.serviceUrl + '/' + sourceUser.id, sourceUser);
  }

  addAuthorizedUser(payload: RequestTokenPayload): Observable<any> {
    const sourceType = localStorage.getItem(this.AUTH_SOURCE_TYPE_STORAGE_KEY);
    const redirectParams = JSON.parse(
      localStorage.getItem(this.AUTH_ENDPOINT_PARAMS_STORAGE_KEY)
    );
    const newPayload = Object.assign(
      {},
      payload,
      { sourceType },
      redirectParams
    );
    return this.http.post(this.serviceUrl, newPayload, {
      responseType: 'json'
    });
  }

  getUserById(userId: string): Observable<RestSourceUser> {
    return this.http.get(this.serviceUrl + '/' + userId);
  }

  deleteUser(userId: string): Observable<any> {
    return this.http.delete(this.serviceUrl + '/' + userId);
  }

  resetUser(user: RestSourceUser): Observable<any> {
    return this.http.post(this.serviceUrl + '/' + user.id + '/reset', user);
  }

  // --
  createUser(restSourceUserRequest: RestSourceUserRequest): Observable<RestSourceUserResponse> {
    const restSourceUserResponse: RestSourceUserResponse = {
      id: '555',
      projectId: restSourceUserRequest.projectId,
      userId: restSourceUserRequest.userId,
      sourceId: '556',
      startDate: restSourceUserRequest.startDate,
      endDate: restSourceUserRequest.endDate,
      sourceType: restSourceUserRequest.sourceType,
      createdAt: '111',
      humanReadableUserId: '',
      externalId: 'peyman',
      serviceUserId: '123',
      isAuthorized: false,
      timesReset: 0,
      version: 'v1.0'
    };
    // return throwError({message: 'Server error. Couldn\'t create user'}).pipe(
    //   delay(5000)
    // );
    return new Observable((observer: { next: (arg0: RestSourceUserResponse) => void; }) => {
      observer.next(restSourceUserResponse);
    }).pipe(
      delay(1000)
    );
  }

  registerUser(registrationCreateRequest: RegistrationCreateRequest): Observable<RegistrationResponse> {
    const registrationResponse = {
      token: '9876543210',
      secret: registrationCreateRequest.persistent ? '123456789' : null,  // only defined if the registration is persistent
      userId: registrationCreateRequest.userId,
      authEndpointUrl: registrationCreateRequest.persistent ? null : 'https://www.google.com',  // only defined if the registration is not persistent
      expiresAt: '123456789',
      persistent: registrationCreateRequest.persistent
    };
    return new Observable((observer: { next: (arg0: RegistrationResponse) => void; }) => {
      observer.next(registrationResponse);
    }).pipe(
      delay(1000)
    );
  }

  authorizeUser(authorizeRequest: AuthorizeRequest, state: string): Observable<RegistrationResponse> {
    const registrationResponse = {
      token: '9876543210',
      userId: 'registrationCreateRequest.userId',
      expiresAt: '123456789',
      persistent: false, // registrationCreateRequest.persistent
    };
    return new Observable((observer: { next: (arg0: RegistrationResponse) => void; }) => {
      observer.next(registrationResponse);
    }).pipe(
      delay(1000)
    );
  }

  getAuthEndpointUrl(registrationRequest: RegistrationRequest, token: string): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(this.serviceUrl + '/registrations/' + token, registrationRequest);
  }
}
