import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import {AuthGuard} from "@app/auth/services/auth.guard";
import {AuthorizationPageComponent} from "@app/shared/containers/authorization-page/authorization-page.component";
import {AuthorizationCompletePageComponent} from "@app/shared/containers/authorization-complete-page/authorization-complete-page.component";

export class APP_ROUTE {
  public static readonly MAIN = '';
  public static readonly AUTHORIZATION = 'users:auth';
  public static readonly AUTHORIZATION_COMPLETE = 'users:new';
}

const routes: Routes = [
  {
    path: APP_ROUTE.MAIN,
    loadChildren: () => import('@app/admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard]
  },
  {
    path: APP_ROUTE.AUTHORIZATION,
    component: AuthorizationPageComponent,
  },
  {
    path: APP_ROUTE.AUTHORIZATION_COMPLETE,
    component: AuthorizationCompletePageComponent,
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
