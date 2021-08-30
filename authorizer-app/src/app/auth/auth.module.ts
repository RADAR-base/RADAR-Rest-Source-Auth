import { ModuleWithProviders, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {LoginPageComponent} from './containers/login-page/login-page.component';
import {SharedModule} from '../shared/shared.module';
import {HttpClientModule} from '@angular/common/http';
import {AuthRoutingModule} from './auth-routing.module';
import {AuthService} from './services/auth.service';
// import { AuthRoutingModule } from './auth-routing.module';
// import { SigninComponent } from './containers/signin/signin.component';
// import { AuthComponent } from './auth.component';
// import {SignupComponent} from "./containers/signup/signup.component";
// import {ForgotPasswordComponent} from "./containers/forgot-password/forgot-password.component";
// import {SharedModule} from "../shared/shared.module";
// import {AuthService} from "./services/auth.service";
// import {HttpClientModule} from "@angular/common/http";
// import {StoreModule} from "@ngrx/store";
// import {authReducer} from "./store/reducers";
// import {EffectsModule} from "@ngrx/effects";
// import {AuthEffects} from "./store/auth.effects";


@NgModule({
  declarations: [LoginPageComponent],
  imports: [
    CommonModule,
    HttpClientModule,
    SharedModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    AuthRoutingModule,
  ],
  providers: [AuthService],
  // bootstrap: [AuthComponent]
})
// export class AuthModule {}
export class AuthModule {
  static forRoot(): ModuleWithProviders<AuthModule> {
      return {
          ngModule: AuthModule,
          providers: [
            // ActivatedRouteSnapshot
            // AuthService,
              // AuthGuard
          ]
      };
  }
}
