import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { AppComponent } from './app.component';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {HttpClientModule} from "@angular/common/http";
import {RouterModule, Routes} from "@angular/router";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {ErrorReportingComponent} from "./components/rest-source-authorization/error.component";
import {RestSourceUserService} from "./services/rest-source-user.service";
import {SourceClientAuthorizationService} from "./services/source-client-authorization.service";
import {RestSourceUserListComponent} from "./components/rest-source-authorization/rest-source-user-list.component";
import {RestSourceUserRegistrationFormComponent} from "./components/rest-source-authorization/rest-source-user-registration-form.component";
import {UpdateRestSourceUserComponent} from "./components/rest-source-authorization/update-rest-source-user.component";
const appRoutes: Routes = [
  {
    path: '',
    component: RestSourceUserListComponent,
  },
  {
    path: 'users',
    component: RestSourceUserListComponent,
  },
  {
    path: 'users:new',
    component: UpdateRestSourceUserComponent,
  },
  {
    path: 'users/:id',
    component: UpdateRestSourceUserComponent,
  },
  {
    path: 'addAuthorizedUser',
    component: RestSourceUserRegistrationFormComponent
  },
  {
    path: 'request-failed',
    component: ErrorReportingComponent,
  },

];


@NgModule({
  declarations: [
    UpdateRestSourceUserComponent,
    AppComponent,
    RestSourceUserRegistrationFormComponent,
    ErrorReportingComponent,
    RestSourceUserListComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forRoot(appRoutes),
    NgbModule.forRoot()
  ],
  providers: [
    RestSourceUserService,
    SourceClientAuthorizationService
  ],
  // entryComponents: [AddDeviceDialogComponent],
  bootstrap: [AppComponent]
})
export class AppModule { }
