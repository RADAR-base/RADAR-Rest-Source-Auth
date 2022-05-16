import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import {LoginPageComponent} from '@app/auth/containers/login-page/login-page.component';
import {GuestGuard} from "@app/auth/services/guest.guard";

export class AUTH_ROUTE {
  public static readonly LOGIN = 'login';
}

const routes: Routes = [
  {
    path: AUTH_ROUTE.LOGIN,
    component: LoginPageComponent,
    canActivate: [GuestGuard]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AuthRoutingModule {}
