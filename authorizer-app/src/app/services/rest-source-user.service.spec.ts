import { TestBed } from '@angular/core/testing';

import { RestSourceUserService } from './rest-source-user.service';

describe('DeviceAuthorizationService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: RestSourceUserService = TestBed.get(RestSourceUserService);
    expect(service).toBeTruthy();
  });
});
