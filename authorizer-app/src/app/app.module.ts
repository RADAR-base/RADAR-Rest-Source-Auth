import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {HTTP_INTERCEPTORS, HttpClient, HttpClientModule} from "@angular/common/http";
import {TranslateLoader, TranslateModule} from "@ngx-translate/core";
import {IModuleTranslationOptions, ModuleTranslateLoader} from "@larscom/ngx-translate-module-loader";

import { AppComponent } from '@app/app.component';
import {AppRoutingModule} from '@app/app-routing.module';
import {AuthModule} from '@app/auth/auth.module';
import {SharedModule} from "@app/shared/shared.module";
import {AuthInterceptor} from "@app/auth/services/auth.interceptor";
import {ErrorInterceptor} from "@app/shared/services/error.interceptor";

export const LANGUAGES = [
    {lang: 'en-GB', locale: 'en-GB', label: 'English (GB)'}, // default language
    {lang: 'nl', locale: 'nl-NL', label: 'Nederlands'},
    {lang: 'en-US', locale: 'en-US', label: 'English (US)'}
  ];

export function ModuleHttpLoaderFactory(http: HttpClient) {
  const baseTranslateUrl = "assets/i18n";

  const options: IModuleTranslationOptions = {
    translateError: (error, path) => {
      console.log("Error on loading translation files: ", { error, path });
    },
    modules: [
      { baseTranslateUrl },
      { baseTranslateUrl, moduleName: "admin" },
      { baseTranslateUrl, moduleName: "auth" },
      { baseTranslateUrl, moduleName: "shared" }
    ]
  };
  return new ModuleTranslateLoader(http, options);
}

@NgModule({
  declarations: [
    AppComponent,
  ],
    imports: [
      BrowserModule,
      BrowserAnimationsModule,
      HttpClientModule,
      TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useFactory: ModuleHttpLoaderFactory,
          deps: [HttpClient]
        }
      }),
      SharedModule,
      AuthModule,
      AppRoutingModule,
    ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
