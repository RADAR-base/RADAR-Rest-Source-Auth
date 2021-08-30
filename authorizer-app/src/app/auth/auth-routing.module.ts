import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {LoginPageComponent} from './containers/login-page/login-page.component';
// import {RestSourceUserDashboardComponent} from './rest-source-authorization/containers/dashboard-page/rest-source-user-dashboard.component';
// import {AuthGuard} from './auth/services/auth.guard';

const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent,
    // canActivate: [AuthGuard]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AuthRoutingModule {}
