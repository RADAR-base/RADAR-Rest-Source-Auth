import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {AuthGuard} from "./auth/services/auth.guard";
import {AuthorizationPageComponent} from "./shared/containers/authorization-page/authorization-page.component";
import {AuthorizationCompletePageComponent} from "./shared/containers/authorization-complete-page/authorization-complete-page.component";

const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'users:auth',
    component: AuthorizationPageComponent,
  },
  {
    path: 'users:new',
    component: AuthorizationCompletePageComponent,
  },
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
