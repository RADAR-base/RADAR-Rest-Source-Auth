import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {RestSourceUserService} from '../../services/rest-source-user.service';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';

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
    const {state, oauth_token, oauth_verifier, oauth_token_secret} = this.activatedRoute.snapshot.queryParams;
    // todo if state not available get token from storage
    const authorizeRequest = {
      code: '456',
      oauth_token,
      oauth_verifier,
      oauth_token_secret
    };
    this.service.authorizeUser(authorizeRequest, state).subscribe(
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
