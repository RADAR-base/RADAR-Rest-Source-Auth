import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {TranslateModule} from "@ngx-translate/core";
import {JwtModule} from "@auth0/angular-jwt";

import {SharedModule} from '@app/shared/shared.module';
import {AuthRoutingModule} from '@app/auth/auth-routing.module';
import {AuthService} from '@app/auth/services/auth.service';
import {ManagementPortalAuthService} from "@app/auth/services/management-portal-auth.service";
import {GuestGuard} from "@app/auth/services/guest.guard";
import {AuthGuard} from "@app/auth/services/auth.guard";
import {LoginPageComponent} from '@app/auth/containers/login-page/login-page.component';

@NgModule({
  declarations: [LoginPageComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    SharedModule,
    JwtModule.forRoot({config: {tokenGetter: AuthService.getAccessToken}}),
    TranslateModule,
    AuthRoutingModule,
  ],
  providers: [
    { provide: AuthService, useClass: ManagementPortalAuthService },
    AuthGuard,
    GuestGuard,
  ],
})
export class AuthModule {}
