import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { AppComponent } from './app.component';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {HttpClientModule} from "@angular/common/http";
import {DevicesService} from "./services/devices.service";
import {RouterModule, Routes} from "@angular/router";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {AddDeviceComponent} from "./components/device-authorization/add-device.component";
import {DevicesListComponent} from "./components/device-authorization/devices-list.component";
import {DeviceAuthorizationService} from "./services/device-authorization.service";
import {DeviceUserRegistrationFormComponent} from "./components/device-authorization/device-user-registration-form.component";
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {ErrorReportingComponent} from "./components/device-authorization/error.component";
const appRoutes: Routes = [
  {
    path: '',
    component: DevicesListComponent,
  },
  {
    path: 'users',
    component: DevicesListComponent,
  },
  {
    path: 'users:new',
    component: AddDeviceComponent,
  },
  {
    path: 'addAuthorizedUser',
    component: DeviceUserRegistrationFormComponent
  },
  {
    path: 'request-failed',
    component: ErrorReportingComponent,
  },

];


@NgModule({
  declarations: [
    AddDeviceComponent,
    AppComponent,
    DeviceUserRegistrationFormComponent,
    ErrorReportingComponent,
    DevicesListComponent
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
    DevicesService,
    DeviceAuthorizationService
  ],
  // entryComponents: [AddDeviceDialogComponent],
  bootstrap: [AppComponent]
})
export class AppModule { }
