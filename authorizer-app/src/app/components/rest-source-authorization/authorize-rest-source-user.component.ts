import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {RestSourceUserService} from '../../services/rest-source-user.service';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';
import {storageItems} from '../../enums/storage';
import {RegistrationResponse} from "../../models/rest-source-user.model";

@Component({
  selector: 'app-authorize-rest-source-user',
  templateUrl: './authorize-rest-source-user.component.html',
  styleUrls: ['./authorize-rest-source-user.component.scss'],
})
export class AuthorizeRestSourceUserComponent implements OnInit {
  loading = false;
  errorMessage?: string;

  authEndpointUrl?: string;
  registrationResponse?: RegistrationResponse;

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
    this.service.getAuthEndpointUrl({secret}, token).subscribe({
      next: (registrationResp) => {
        if (registrationResp.authEndpointUrl) {
          this.authEndpointUrl = registrationResp.authEndpointUrl;
          this.registrationResponse = registrationResp;
          // this.loading = false;
        }
      },
      error: (error) => {
        this.errorMessage = error.error.error_description;
        // this.loading = false;
      },
    });
    // this.service.getAuthEndpointUrl({secret}, token).subscribe(
    //   registrationResp => {
    //     if (registrationResp.authEndpointUrl) {
    //       window.location.href = registrationResp.authEndpointUrl;
    //     }
    //   },
    //   error => {
    //     console.log(error);
    //     this.errorMessage = error.error.error_description;
    //   }
    // );
  }

  authorize(): void {
    if(this.authEndpointUrl) {
      window.location.href = this.authEndpointUrl;
    }
  }
}
