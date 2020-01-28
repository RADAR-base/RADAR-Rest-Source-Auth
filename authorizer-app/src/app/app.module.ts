import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
  MatButtonModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatMenuModule,
  MatPaginatorModule,
  MatSortModule,
  MatTableModule,
  MatToolbarModule
} from '@angular/material';
import { RouterModule, Routes } from '@angular/router';

import { AppComponent } from './app.component';
import { AuthGuard } from './services/auth.guard';
import { AuthService } from './services/auth.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { ErrorReportingComponent } from './components/rest-source-authorization/error.component';
import { HttpClientModule } from '@angular/common/http';
import { JwtModule } from '@auth0/angular-jwt';
import { LoginPageComponent } from './components/auth/login-page.component';
import { NgModule } from '@angular/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { RestSourceUserListComponent } from './components/rest-source-authorization/rest-source-user-list.component';
import { RestSourceUserRegistrationFormComponent } from './components/rest-source-authorization/rest-source-user-registration-form.component';
import { RestSourceUserService } from './services/rest-source-user.service';
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
    ToolbarComponent
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
    JwtModule.forRoot({ config: { tokenGetter: AuthService.getToken } })
  ],
  providers: [
    RestSourceUserService,
    SourceClientAuthorizationService,
    AuthGuard,
    AuthService
  ],
  // entryComponents: [AddDeviceDialogComponent],
  bootstrap: [AppComponent]
})
export class AppModule {}
