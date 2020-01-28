import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';

import { AuthService } from '../../services/auth.service';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css']
})
export class LoginPageComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params.code) {
        this.authService
          .login(params.code)
          .subscribe(() => this.router.navigate(['/']));
      }
    });
  }

  loginHandler() {
    window.location.href = `${environment.AUTH_URI}/authorize?client_id=${
      environment.AUTH.client_id
    }&response_type=code&redirect_uri=${window.location.href.split('?')[0]}`;
  }
}
