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
    // private service: RestSourceUserMockService,
  ) {
  }

  ngOnInit(): void {
    console.log(this.activatedRoute.snapshot.queryParams);
    const state = this.activatedRoute.snapshot.queryParams.state;
    // todo code, oauth_token, oauth_verifier, oauth_token_secret
    // todo if state not available get token from storage
    const authorizeRequest = {
      code: '456',
      oauth_token: '987',
      oauth_verifier: '654',
      oauth_token_secret: '147'
    };
    this.service.authorizeUser(authorizeRequest, state).subscribe(
      resp => {
        console.log(resp);
        if (resp.persistent) {
          this.router.navigateByUrl('');
        } else {
          this.showThankYou = true;
        }
        },
      err => {
        console.log(err);
      }
    );
  }
}
