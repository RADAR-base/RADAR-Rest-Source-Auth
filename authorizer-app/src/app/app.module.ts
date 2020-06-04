import { ActivatedRoute, Router, RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { JwtHelperService, JwtModule } from '@auth0/angular-jwt';
import {
  MAT_MOMENT_DATE_ADAPTER_OPTIONS,
  MatMomentDateModule
} from '@angular/material-moment-adapter';
import {
  MatButtonModule,
  MatDatepickerModule,
  MatDialogModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatMenuModule,
  MatNativeDateModule,
  MatPaginatorModule,
  MatProgressSpinnerModule,
  MatSortModule,
  MatTableModule,
  MatToolbarModule,
  MatTooltipModule
} from '@angular/material';
import {
  RestSourceUserListComponent,
  RestSourceUserListDeleteDialog,
  RestSourceUserListResetDialog
} from './components/rest-source-authorization/rest-source-user-list.component';

import { AppComponent } from './app.component';
import { AuthGuard } from './services/auth.guard';
import { AuthService } from './services/auth.service';
import { AuthServiceFactory } from './services/auth.service.factory';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { ErrorReportingComponent } from './components/rest-source-authorization/error.component';
import { LoginPageComponent } from './components/auth/login-page.component';
import { MpLoginComponent } from './components/auth/mp-login.component';
import { NgModule } from '@angular/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { RestSourceUserRegistrationFormComponent } from './components/rest-source-authorization/rest-source-user-registration-form.component';
import { RestSourceUserService } from './services/rest-source-user.service';
import { SimpleLoginComponent } from './components/auth/simple-login.component';
import { SourceClientAuthorizationService } from './services/source-client-authorization.service';
import { ToolbarComponent } from './components/shared/toolbar/toolbar.component';
import { UpdateRestSourceUserComponent } from './components/rest-source-authorization/update-rest-source-user.component';

const appRoutes: Routes = [
  {
    path: '',
    component: RestSourceUserListComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'login',
    component: LoginPageComponent
  },
  {
    path: 'users',
    component: RestSourceUserListComponent,
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
    component: RestSourceUserListComponent,
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
    LoginPageComponent,
    SimpleLoginComponent,
    MpLoginComponent,
    ToolbarComponent,
    RestSourceUserListDeleteDialog,
    RestSourceUserListResetDialog
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
    JwtModule.forRoot({ config: { tokenGetter: AuthService.getAccessToken } }),
    MatTooltipModule,
    MatDialogModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatMomentDateModule,
    MatProgressSpinnerModule
  ],
  providers: [
    RestSourceUserService,
    SourceClientAuthorizationService,
    AuthGuard,
    {
      provide: AuthService,
      useFactory: AuthServiceFactory,
      deps: [HttpClient, JwtHelperService]
    },
    { provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS, useValue: { useUtc: true } }
  ],
  // entryComponents: [AddDeviceDialogComponent],
  bootstrap: [AppComponent]
})
export class AppModule {}
