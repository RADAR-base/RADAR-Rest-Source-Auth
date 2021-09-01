import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {RestSourceUserService} from '../../services/rest-source-user.service';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';
import {storageItems} from '../../enums/storage';

@Component({
  selector: 'app-authorize-rest-source-user',
  templateUrl: './authorize-rest-source-user.component.html',
  styleUrls: ['./authorize-rest-source-user.component.scss'],
})
export class AuthorizeRestSourceUserComponent implements OnInit {
  showThankYou = false;

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private service: RestSourceUserService,
    // private mockService: RestSourceUserMockService,
  ) {
  }

  ngOnInit(): void {
    console.log(this.activatedRoute.snapshot.queryParams);
    const {token, secret} = this.activatedRoute.snapshot.queryParams;
    localStorage.setItem(storageItems.authorizationToken, token);
    this.service.getAuthEndpointUrl({secret}, token).subscribe(
      registrationResp => {
        if (registrationResp.authEndpointUrl) {
          window.location.href = registrationResp.authEndpointUrl;
        }
      }
    );
  }
}
