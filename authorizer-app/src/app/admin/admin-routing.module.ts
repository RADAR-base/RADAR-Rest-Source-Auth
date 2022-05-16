import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import {AuthGuard} from "@app/auth/services/auth.guard";
import {DashboardPageComponent} from "@app/admin/containers/dashboard-page/dashboard-page.component";
import {ProjectsResolver} from "@app/admin/services/projects.resolver";
import {SourceClientsResolver} from "@app/admin/services/source-clients.resolver";

export class ADMIN_ROUTE {
  public static readonly MAIN = '';
}

const routes: Routes = [
  {
    path: ADMIN_ROUTE.MAIN,
    component: DashboardPageComponent,
    resolve: {projects: ProjectsResolver, sourceClients: SourceClientsResolver},
    canActivate: [AuthGuard]
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
