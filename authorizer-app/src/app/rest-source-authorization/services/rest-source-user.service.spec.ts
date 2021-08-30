import { HttpClientModule } from '@angular/common/http';
import { JwtHelperService } from '@auth0/angular-jwt';
import { RestSourceUserService } from './rest-source-user.service';
import { TestBed } from '@angular/core/testing';

describe('DeviceAuthorizationService', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [HttpClientModule],
      providers: [
        RestSourceUserService,
        { provide: JwtHelperService, useClass: JwtHelperServiceMock }
      ]
    })
  );

  it('should be created', () => {
    const service: RestSourceUserService = TestBed.get(RestSourceUserService);
    expect(service).toBeTruthy();
  });
});

export class JwtHelperServiceMock {}
