// import { FormsModule, ReactiveFormsModule } from '@angular/forms';
// import {
//   MAT_MOMENT_DATE_ADAPTER_OPTIONS,
//   MatMomentDateModule
// } from '@angular/material-moment-adapter';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
// import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import {AppRoutingModule} from './app-routing.module';
// import {SharedModule} from './shared/shared.module';
import {AuthModule} from './auth/auth.module';
import {RestSourceAuthorizationModule} from './rest-source-authorization/rest-source-authorization.module';
// import {RestSourceAuthorizationModule} from './rest-source-authorization/rest-source-authorization.module';
// import {SharedModule} from './shared/shared.module';
// import {AuthModule} from './auth/auth.module';
// import {RestSourceAuthorizationModule} from './rest-source-authorization/rest-source-authorization.module';
// import {AuthInterceptor} from './auth/services/auth.interceptor';
// import {ErrorInterceptor} from './shared/services/error.interceptor';
// import {ManagementPortalAuthService} from './auth/services/management-portal-auth.service';
// import {AuthService} from './auth/services/auth.service';

@NgModule({
  declarations: [
    AppComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    // HttpClientModule,
    // FormsModule,
    // ReactiveFormsModule,
    // SharedModule,
    AuthModule.forRoot(),
    RestSourceAuthorizationModule,
    AppRoutingModule,
  ],
  providers: [
    // { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    // { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    // { provide: AuthService, useClass: ManagementPortalAuthService },
    // { provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS, useValue: { useUtc: true } }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
