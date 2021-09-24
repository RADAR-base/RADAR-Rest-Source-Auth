import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {UserService} from "../../../admin/services/user.service";
import {RegistrationResponse} from "../../../admin/models/rest-source-user.model";
import {StorageItem} from "../../enums/storage-item";

@Component({
  selector: 'app-authorization-page',
  templateUrl: './authorization-page.component.html',
  styleUrls: ['./authorization-page.component.scss'],
})
export class AuthorizationPageComponent implements OnInit {
  loading = false;
  error?: any;

  authEndpointUrl?: string;
  registrationResponse?: RegistrationResponse;

  constructor(
    private activatedRoute: ActivatedRoute,
    private userService: UserService,
  ) {}

  ngOnInit(): void {
    this.loading = true;
    const {token, secret} = this.activatedRoute.snapshot.queryParams;
    localStorage.setItem(StorageItem.AUTHORIZATION_TOKEN, token);
    this.userService.getAuthEndpointUrl({secret}, token).subscribe({
      next: (registrationResp) => {
        if (registrationResp.authEndpointUrl) {
          this.authEndpointUrl = registrationResp.authEndpointUrl;
          this.registrationResponse = registrationResp;
          this.loading = false;
        }
      },
      error: (error) => {
        this.error = error;
        this.loading = false;
      },
    });
  }

  authorize(): void {
    if(this.authEndpointUrl) {
      window.location.href = this.authEndpointUrl;
    }
  }
}
