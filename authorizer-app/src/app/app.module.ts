import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
  MAT_MOMENT_DATE_ADAPTER_OPTIONS,
  MatMomentDateModule
} from '@angular/material-moment-adapter';
import {
  MatButtonModule,
  MatCheckboxModule,
  MatDatepickerModule,
  MatDialogModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatMenuModule,
  MatNativeDateModule,
  MatPaginatorModule,
  MatSortModule,
  MatTableModule,
  MatToolbarModule,
  MatTooltipModule
} from '@angular/material';
import {
  RestSourceUserListComponent,
} from './components/rest-source-authorization/rest-source-user-list.component';
import {
  RestSourceUserDashboardComponent,
} from './components/rest-source-authorization/rest-source-user-dashboard.component';

import { RouterModule, Routes } from '@angular/router';

import { AppComponent } from './app.component';
import { AuthGuard } from './services/auth.guard';
import { AuthService } from './services/auth.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { ErrorReportingComponent } from './components/rest-source-authorization/error.component';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { JwtModule } from '@auth0/angular-jwt';
import { LoginPageComponent } from './components/auth/login-page.component';
import { ManagementPortalAuthService } from './services/management-portal-auth.service';
import { NgModule } from '@angular/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { RestSourceUserRegistrationFormComponent } from './components/rest-source-authorization/rest-source-user-registration-form.component';
import { RestSourceUserService } from './services/rest-source-user.service';
import { SourceClientAuthorizationService } from './services/source-client-authorization.service';
import { ToolbarComponent } from './components/shared/toolbar/toolbar.component';
import { UpdateRestSourceUserComponent } from './components/rest-source-authorization/update-rest-source-user.component';
import { AuthInterceptor } from './auth.interceptor';
import { ErrorInterceptor } from './error.interceptor';
import {RestSourceUserListDeleteDialog} from './components/rest-source-authorization/rest-source-user-list-delete-dialog.component';
import {RestSourceUserListResetDialog} from './components/rest-source-authorization/rest-source-user-list-reset-dialog.component';

const appRoutes: Routes = [
  {
    path: '',
    component: RestSourceUserDashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'login',
    component: LoginPageComponent
  },
  {
    path: 'users',
    component: RestSourceUserDashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'users:new',
    component: UpdateRestSourceUserComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'users/:id',
    component: UpdateRestSourceUserComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'addAuthorizedUser',
    component: RestSourceUserRegistrationFormComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'request-failed',
    component: ErrorReportingComponent,
    canActivate: [AuthGuard]
  },
  {
    path: '**',
    component: RestSourceUserDashboardComponent,
    canActivate: [AuthGuard]
  }
];

@NgModule({
  declarations: [
    UpdateRestSourceUserComponent,
    AppComponent,
    RestSourceUserRegistrationFormComponent,
    ErrorReportingComponent,
    RestSourceUserListComponent,
    RestSourceUserDashboardComponent,
    LoginPageComponent,
    ToolbarComponent,
    RestSourceUserListResetDialog,
    RestSourceUserListDeleteDialog
  ],
  entryComponents: [
    RestSourceUserListDeleteDialog,
    RestSourceUserListResetDialog
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    MatPaginatorModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSortModule,
    MatButtonModule,
    MatMenuModule,
    MatToolbarModule,
    MatIconModule,
    RouterModule.forRoot(appRoutes),
    NgbModule.forRoot(),
    JwtModule.forRoot({config: {tokenGetter: AuthService.getAccessToken}}),
    MatTooltipModule,
    MatDialogModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatMomentDateModule,
    MatCheckboxModule
  ],
  providers: [
    RestSourceUserService,
    SourceClientAuthorizationService,
    AuthGuard,
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    { provide: AuthService, useClass: ManagementPortalAuthService },
    { provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS, useValue: { useUtc: true } }
  ],
  // entryComponents: [AddDeviceDialogComponent],
  bootstrap: [AppComponent]
})
export class AppModule {}
