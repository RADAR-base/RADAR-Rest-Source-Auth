import { ActivatedRoute, Router } from '@angular/router';
import { ChangeDetectorRef, Component, OnDestroy } from '@angular/core';

import { AuthService } from 'src/app/services/auth.service';
import { Subscription } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { grantType } from 'src/app/enums/grant-type';

@Component({
  selector: 'login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css']
})
export class LoginPageComponent implements OnDestroy {
  authSubscription = new Subscription();
  authType: string;
  authTypes = grantType;
  isLoaded = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.authType = environment.authorizationGrantType;
  }

  ngOnDestroy() {
    this.authSubscription.unsubscribe();
  }

  authenticate(data) {
    this.isLoaded = false;
    this.changeDetectorRef.detectChanges();
    this.authSubscription = this.authService
      .authenticate(data)
      .pipe(delay(1000))
      .subscribe(() => {
        this.router.navigate(['/']);
      });
  }
}
