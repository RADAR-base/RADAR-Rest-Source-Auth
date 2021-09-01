import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {RestSourceUserService} from '../../services/rest-source-user.service';
import {storageItems} from '../../enums/storage';

@Component({
  selector: 'app-authorized-rest-source-user',
  templateUrl: './authorized-rest-source-user.component.html',
  styleUrls: ['./authorized-rest-source-user.component.scss'],
})
export class AuthorizedRestSourceUserComponent implements OnInit {
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
    const {state, oauth_token, oauth_verifier, oauth_token_secret, code} = this.activatedRoute.snapshot.queryParams;

    let stateOrToken = state;
    if (!state) {
      stateOrToken = localStorage.getItem(storageItems.authorizationToken);
    }
    const authorizeRequest = {
      code,
      oauth_token,
      oauth_verifier,
      oauth_token_secret
    };
    this.service.authorizeUser(authorizeRequest, stateOrToken).subscribe(
      resp => {
        console.log(resp);
        if (resp.persistent) {
          this.showThankYou = true;
        } else {
          this.router.navigateByUrl('');
        }
      },
      err => {
        console.log(err);
      }
    );
  }
}
