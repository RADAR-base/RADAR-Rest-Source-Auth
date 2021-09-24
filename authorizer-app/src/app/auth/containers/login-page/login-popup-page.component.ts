import {ActivatedRoute, Router} from '@angular/router';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {AuthService} from '../../services/auth.service';
import {Subscription} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {grantType} from '../../enums/grant-type';

@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.scss']
})
export class LoginPageComponent implements OnInit, OnDestroy {

  sessionExpired: boolean = false;
  returnUrl?: string;

  loginLoading = false;

  routerSubscription?: Subscription;
  // authCodeSubscription?: Subscription;
  authCodeSubscription?: Subscription;

  // authorization code and tokens
  authorizationCode?: string | null;
  // popup related
  private windowHandle!: null | Window;   // reference to the window object we will create
  private intervalId: any = null;  // For setting interval time between we check for authorization code or token
  private loopCount = 600;   // the count until which the check will be done, or after window be closed automatically.
  private intervalLength = 100;   // the gap in which the check will be done for code.


  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {
    const state = this.router.getCurrentNavigation()?.extras.state;
    if (state) {
      this.sessionExpired = state.command === 'session-expired';
      this.returnUrl = state.url
    }
  }

  ngOnInit() {
    if (environment.authorizationGrantType === grantType.AUTHORIZATION_CODE) {
      this.loginWithAuthCode();
    }
  }

  loginWithAuthCode() {
    // this.routerSubscription = this.activatedRoute.queryParams.subscribe(params => {
    //
    //   const { code } = params;
    //   // let { returnUrl } = params;
    //
    //   // if (returnUrl) {
    //   //   localStorage.setItem('returnUrl', returnUrl)
    //   // }
    //
    //   if (code) {
    //     this.loading = true;
    //     this.authService
    //       .authenticate(code)
    //       .pipe(first())
    //       .subscribe(() => {
    //         // if (success) {
    //         //   returnUrl = localStorage.getItem('returnUrl');
    //           if (this.returnUrl) {
    //             this.router.navigateByUrl(this.returnUrl);
    //           } else {
    //             this.router.navigateByUrl("/");
    //           }
    //         // }
    //       });
    //
    //     // this.authCodeSubscription = this.authService
    //     //   .authenticate(params.code)
    //     //   .subscribe(() => this.router.navigate(['/']));
    //   }
    // });
  }

  ngOnDestroy() {
    // this.authCodeSubscription?.unsubscribe();
    this.routerSubscription?.unsubscribe();
  }

  loginHandler() {
    if (environment.authorizationGrantType === grantType.AUTHORIZATION_CODE) {
      this.loginLoading = true;
      // this.authService.authorize();
      // this.redirectToAuthRequestLink();
      const authUrl = `${environment.authBaseUrl}/authorize?client_id=${
        environment.appClientId
      }&response_type=code&redirect_uri=${window.location.href.split('?')[0]}`;
      this.doAuthorization(authUrl);
    }
  }

  doAuthorization(url: string) {
    /* isRegisterAction flag i am using to check if the process is for registration or Login */
    /* socialMediaProvider is for name of social media , it is optional*/

    let loopCount = this.loopCount;

    /* Create the window object by passing url and optional window title */
    this.windowHandle = this.createOauthWindow(url, 'OAuth login');
    if(!this.windowHandle) {
      return;
    }

    // this.windowHandle.onbeforeunload = () => {
    //   alert("You are now leaving, are you sure?")
    // }

    // this.windowHandle.addEventListener('beforeunload', (event) => {
    //   event.returnValue = `You have unsaved changes, leave anyway?`;
    //   alert("You are now leaving, are you sure?")
    // });
    // window.addEventListener('beforeunload', function (e) {
    //   // Cancel the event as stated by the standard.
    //   e.preventDefault();
    //   alert("You are now leaving, are you sure?")
    //   // Chrome requires returnValue to be set.
    //   e.returnValue = '';
    // });

    /* Now start the timer for which the window will stay, and after time over window will be closed */
    this.intervalId = window.setInterval(() => {
      console.log('interval', loopCount)
      if (loopCount-- < 0) {
        // console.log('loopCount-- < 0')
        window.clearInterval(this.intervalId);
        this.windowHandle?.close();
        this.loginLoading = false;
      } else {
        // console.log('!! loopCount-- < 0')
        let href: string | null = null;  // For referencing window url
        // let closed: boolean = false;
        try {
          href = this.windowHandle?.location.href || null; // set window location to href string
          //this.windowHandle.op
          console.log('href',href);
          if(this.windowHandle?.closed){
            console.log('closed');
            window.clearInterval(this.intervalId);
            this.loginLoading = false;
            // closed = this.windowHandle?.closed
          }
        } catch (e) {
          // console.log('Error:', e); // Handle any errors here
        }
        if (href != null) {
          console.log('href != null');

          // Method for getting query parameters from query string
          const getQueryString = function(field: any, url0: string) {
            const windowLocationUrl = url0 ? url0 : href;
            const reg = new RegExp('[?&]' + field + '=([^&#]*)', 'i');
            if (typeof windowLocationUrl === "string") {
              const qString = reg.exec(windowLocationUrl);
              return qString ? qString[1] : null;
            }
            return null;
          };
          /* As i was getting code and oauth-token i added for same, you can replace with your expected variables */
          if (href.match('code')) {
            // for google , fb, github, linkedin
            window.clearInterval(this.intervalId);
            this.authorizationCode = getQueryString('code', href);
            this.windowHandle?.close();
            // if (isRegisterAction) {
              /* call signup method */
            // } else {
              /* call login method */
              this.authCodeSubscription = this.authService
                .authenticate(this.authorizationCode)
                .subscribe(() => {
                  if (this.returnUrl) {
                    this.router.navigateByUrl(this.returnUrl);
                  } else {
                    this.router.navigateByUrl("/");
                  }
                  // this.router.navigate(['/'])
                });
            // }
          }
          // else if (href.match('oauth_token')) {
          //   // for twitter
          //   window.clearInterval(this.intervalId);
          //   this.oAuthToken = getQueryString('oauth_token', href);
          //   this.oAuthVerifier = getQueryString('oauth_verifier', href);
          //   this.windowHandle.close();
          //   if (isRegisterAction) {
          //     /* call signup */
          //   } else {
          //     /* call login */
          //   }
          // }
        } else {
          console.log('href == null');

        }
      }
    }, this.intervalLength);
  }

  createOauthWindow(url: string, name = 'Authorization', width = 500, height = 600, left = 0, top = 0) {
    if (url == null) {
      return null;
    }
    const options = `width=${width},height=${height},left=${left},top=${top}`;
    return window.open(url, name, options);
    // loginWindow?.addEventListener('beforeunload', function (e) {
    //   // Cancel the event as stated by the standard.
    //   e.preventDefault();
    //   alert("You are now leaving, are you sure?")
    //   // Chrome requires returnValue to be set.
    //   e.returnValue = '';
    // });
    // return loginWindow; //window.open(url, name, options); //window.open(url, name, options)?.addEventListener('beforeunload', function (e) {
    //   // Cancel the event as stated by the standard.
    //   e.preventDefault();
    //   alert("You are now leaving, are you sure?")
    //   // Chrome requires returnValue to be set.
    //   e.returnValue = '';
    // });
  }

  // redirectToAuthRequestLink() {
  //   window.location.href = `${environment.authBaseUrl}/authorize?client_id=${
  //     environment.appClientId
  //   }&response_type=code&redirect_uri=${window.location.href.split('?')[0]}`;
  //
  //   // window.location.href = `${environment.authAPI}/authorize?client_id=${environment.clientId}&response_type=code&redirect_uri=${environment.authCallback}`;
  // }

  closeLoginWindow() {
    this.windowHandle?.close();
    this.loginLoading = false;
  }
}
