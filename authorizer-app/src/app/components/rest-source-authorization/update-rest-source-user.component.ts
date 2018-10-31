import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {RestSourceUserService} from "../../services/rest-source-user.service";
import {RestSourceUser} from "../../models/rest-source-user.model";
import {SourceClientAuthorizationService} from "../../services/source-client-authorization.service";

@Component({
  selector: 'update-rest-source-user',
  templateUrl: './update-rest-source-user.component.html',
})
export class UpdateRestSourceUserComponent implements OnInit {
  errorMessage: string;
  restSourceUser: RestSourceUser;
  startDate;
  endDate;

  constructor(private restSourceUserService: RestSourceUserService,
              private sourceClientAuthorizationService: SourceClientAuthorizationService,
              private router: Router,
              private activatedRoute: ActivatedRoute) {
  }

  ngOnInit() {
    this.activatedRoute.queryParams.subscribe((params: Params) => {
      if(params.hasOwnProperty('error')) {
        this.errorMessage = params['error_description'];
      } else {
        this.errorMessage = null;
        this.addRestSourceUser(params['code'], params['state']);
      }
    });
  }

  private updateRestSourceUser() {

    this.restSourceUser.startDate = new Date(Date.UTC(this.startDate.year, this.startDate.month-1, this.startDate.day)).toISOString();
    this.restSourceUser.endDate = new Date(Date.UTC(this.endDate.year, this.endDate.month-1, this.endDate.day)).toISOString();
    this.restSourceUserService.updateUser(this.restSourceUser).subscribe(() => {
        return this.router.navigate(['/users']);
      },
      err => {
        this.errorMessage = err.json._body;

      });
  }

  private addRestSourceUser(code: string, state: string) {
    this.restSourceUserService.addAuthorizedUser(code, state).subscribe(data => {
        this.restSourceUser = data;

      },
      (err: Response) => {
        this.errorMessage = 'Cannot retrieve current user details'
        window.setTimeout(() => this.router.navigate(['']), 5000);
      });
  }
}
