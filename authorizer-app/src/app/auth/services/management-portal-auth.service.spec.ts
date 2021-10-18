import { HttpClient, HttpHandler } from '@angular/common/http';

import { JwtHelperService } from '@auth0/angular-jwt';
import { ManagementPortalAuthService } from './management-portal-auth.service';
import { TestBed } from '@angular/core/testing';

describe('ManagementPortalAuthService', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [
        ManagementPortalAuthService,
        HttpClient,
        HttpHandler,
        { provide: JwtHelperService, useClass: JwtHelperServiceMock }
      ]
    })
  );

  it('should be created', () => {
    const service: ManagementPortalAuthService = TestBed.get(
      ManagementPortalAuthService
    );
    expect(service).toBeTruthy();
  });
});

export class JwtHelperServiceMock {}
