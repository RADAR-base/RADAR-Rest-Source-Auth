import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {UserService} from "../../../admin/services/user.service";
import {AuthService} from "../../../auth/services/auth.service";
import {StorageItem} from "../../enums/storage-item";

@Component({
  selector: 'app-authorization-complete-page',
  templateUrl: './authorization-complete-page.component.html',
  styleUrls: ['./authorization-complete-page.component.scss'],
})
export class AuthorizationCompletePageComponent implements OnInit {
  loading = false;
  error?: any;

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private service: UserService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    const {state, oauth_token, oauth_verifier, oauth_token_secret, code} = this.activatedRoute.snapshot.queryParams;

    let stateOrToken = state;
    if (!state) {
      stateOrToken = localStorage.getItem(StorageItem.AUTHORIZATION_TOKEN);
    }
    const authorizeRequest = {
      code,
      oauth_token,
      oauth_verifier,
      oauth_token_secret
    };
    this.service.authorizeUser(authorizeRequest, stateOrToken).subscribe({
      next: (resp) => {
        if (resp.persistent) {
          this.loading = false;
        } else {
          const savedUrl = localStorage.getItem(StorageItem.SAVED_URL) || '';
          this.router.navigateByUrl(savedUrl).finally();
        }
      },
      error: (error) => {
        this.error = error;
        this.loading = false;
      }
    });
  }

  redirect() {
    this.router.navigateByUrl('/login').finally()
  }
}
