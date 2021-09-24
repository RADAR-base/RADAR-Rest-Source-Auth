import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {LoginPageComponent} from './containers/login-page/login-page.component';
import {SharedModule} from '../shared/shared.module';
import {AuthRoutingModule} from './auth-routing.module';
import {AuthService} from './services/auth.service';
import {ManagementPortalAuthService} from "./services/management-portal-auth.service";
import {JwtModule} from "@auth0/angular-jwt";
import {GuestGuard} from "./services/guest.guard";
import {AuthGuard} from "./services/auth.guard";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {TranslateModule} from "@ngx-translate/core";

@NgModule({
  declarations: [LoginPageComponent],
  imports: [
    CommonModule,
    SharedModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    JwtModule.forRoot({config: {tokenGetter: AuthService.getAccessToken}}),
    AuthRoutingModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  providers: [
    { provide: AuthService, useClass: ManagementPortalAuthService },
    AuthGuard,
    GuestGuard,
  ],
})
export class AuthModule {}
