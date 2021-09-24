import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {AuthGuard} from "../auth/services/auth.guard";
import {DashboardPageComponent} from "./containers/dashboard-page/dashboard-page.component";
import {ProjectsResolver} from "./services/projects.resolver";
import {SourceTypesResolver} from "./services/sourceTypes.resolver";

const routes: Routes = [
  {
    path: '',
    component: DashboardPageComponent,
    resolve: {projects: ProjectsResolver, sourceTypes: SourceTypesResolver},
    canActivate: [AuthGuard]
  },
  // {
  //   path: 'users',
  //   component: DashboardPageComponent,
  //   canActivate: [AuthGuard]
  // },
  // {
  //   path: '**',
  //   component: DashboardPageComponent,
  //   canActivate: [AuthGuard]
  // }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
