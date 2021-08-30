import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
// import {RestSourceUserDashboardComponent} from './rest-source-authorization/containers/dashboard-page/rest-source-user-dashboard.component';
// import {AuthGuard} from './auth/services/auth.guard';

const routes: Routes = [
  // {
  //   path: '**',
  //   component: RestSourceUserDashboardComponent,
  //   // canActivate: [AuthGuard]
  // }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
