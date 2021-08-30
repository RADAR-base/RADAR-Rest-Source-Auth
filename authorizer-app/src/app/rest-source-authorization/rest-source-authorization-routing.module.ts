import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {RestSourceUserDashboardComponent} from './containers/dashboard-page/rest-source-user-dashboard.component';
// import {LinkRestSourceUserComponent} from './containers/add-subject-page/link-rest-source-user.component';
// import {AuthorizedRestSourceUserComponent} from './containers/subject-authorized-page/authorized-rest-source-user.component';
// import {AuthorizeRestSourceUserComponent} from './containers/subject-authorization-page/authorize-rest-source-user.component';
// import {AuthGuard} from '../auth/services/auth.guard';
// import {UpdateRestSourceUserComponent} from './containers/update-subject-dialog/update-rest-source-user.component';
// // import {RestSourceUserRegistrationFormComponent} from './rest-source-user-registration-form.component';
// import {ErrorReportingComponent} from './components/error/error.component';
// import {RestSourceUserRegistrationFormComponent} from './components/register-subject/rest-source-user-registration-form.component';
// // import {PageNotFoundComponent} from "./shared/components/page-not-found/page-not-found.component";
// // import {GuestGuard} from "./auth/guards/guest.guard";
// // import {AuthGuard} from "./auth/guards/auth.guard";

const routes: Routes = [
  {
    path: '',
    component: RestSourceUserDashboardComponent,
    // canActivate: [AuthGuard]
  },
  // // {
  // //   path: 'login',
  // //   component: LoginPageComponent
  // // },
  // {
  //   path: 'users',
  //   component: RestSourceUserDashboardComponent,
  //   // canActivate: [AuthGuard]
  // },
  // // {
  // //   path: 'users:new',
  // //   component: UpdateRestSourceUserComponent,
  // //   canActivate: [AuthGuard]
  // // },
  // {
  //   path: 'users:link',
  //   component: LinkRestSourceUserComponent,
  //   // canActivate: [AuthGuard]
  // },
  // {
  //   path: 'users:new',
  //   component: AuthorizedRestSourceUserComponent,
  //   // canActivate: [AuthGuard]
  // },
  // {
  //   path: 'users:auth',
  //   component: AuthorizeRestSourceUserComponent,
  //   // canActivate: [AuthGuard]
  // },
  // {
  //   path: 'users/:id',
  //   component: UpdateRestSourceUserComponent,
  //   canActivate: [AuthGuard]
  // },
  // {
  //   path: 'addAuthorizedUser',
  //   component: RestSourceUserRegistrationFormComponent,
  //   canActivate: [AuthGuard]
  // },
  // {
  //   path: 'request-failed',
  //   component: ErrorReportingComponent,
  //   canActivate: [AuthGuard]
  // },
  // {
  //   path: '**',
  //   component: RestSourceUserDashboardComponent,
  //   canActivate: [AuthGuard]
  // }
];


// const routes: Routes = [
//   {
//     path: 'admin',
//     loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule),
//     canActivate: [AuthGuard]
//   },
//   {
//     path: '**',
//     component: PageNotFoundComponent
//   }
// ];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class RestSourceAuthorizationRoutingModule { }
