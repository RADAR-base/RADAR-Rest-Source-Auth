import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import {
  MatButtonModule,
  MatFormFieldModule,
  MatInputModule,
  MatPaginatorModule,
  MatSortModule,
  MatTableModule
} from "@angular/material";
import { RouterModule, Routes } from "@angular/router";

import { AppComponent } from "./app.component";
import { AuthorizePageComponent } from "./pages/authorize/containers/authorize-page.component";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { BrowserModule } from "@angular/platform-browser";
import { ErrorReportingComponent } from "./core/components/error/error.component";
import { HttpClientModule } from "@angular/common/http";
import { NgModule } from "@angular/core";
import { NgbModule } from "@ng-bootstrap/ng-bootstrap";
import { RestSourceUserListComponent } from "./pages/users/components/users-list/rest-source-user-list.component";
import { RestSourceUserRegistrationFormComponent } from "./pages/authorize/components/registration-form/rest-source-user-registration-form.component";
import { RestSourceUserService } from "./services/rest-source-user.service";
import { SourceClientAuthorizationService } from "./services/source-client-authorization.service";
import { UpdateRestSourceUserComponent } from "./pages/user/components/update-user/update-rest-source-user.component";
import { UserPageComponent } from "./pages/user/containers/user-page.component";
import { UsersPageComponent } from "./pages/users/containers/users-page.component";

const appRoutes: Routes = [
  {
    path: "",
    component: UsersPageComponent
  },
  {
    path: "users",
    component: UsersPageComponent
  },
  {
    path: "user:new",
    component: UserPageComponent
  },
  {
    path: "user/:id",
    component: UserPageComponent
  },
  {
    path: "authorize",
    component: AuthorizePageComponent
  },
  {
    path: "request-failed",
    component: ErrorReportingComponent
  }
];

@NgModule({
  declarations: [
    AuthorizePageComponent,
    UsersPageComponent,
    UserPageComponent,
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
    MatPaginatorModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSortModule,
    MatButtonModule,
    RouterModule.forRoot(appRoutes),
    NgbModule.forRoot()
  ],
  providers: [RestSourceUserService, SourceClientAuthorizationService],
  // entryComponents: [AddDeviceDialogComponent],
  bootstrap: [AppComponent]
})
export class AppModule {}
