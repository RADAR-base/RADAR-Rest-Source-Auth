import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {LoginPageComponent} from './containers/login-page/login-page.component';
import {GuestGuard} from "./services/guest.guard";

const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent,
    canActivate: [GuestGuard]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AuthRoutingModule {}
