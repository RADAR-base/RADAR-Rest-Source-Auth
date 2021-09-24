import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import {AppRoutingModule} from './app-routing.module';
import {AuthModule} from './auth/auth.module';
import {SharedModule} from "./shared/shared.module";
import {HTTP_INTERCEPTORS, HttpClient, HttpClientModule} from "@angular/common/http";
import {AuthInterceptor} from "./auth/services/auth.interceptor";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {TranslateLoader, TranslateModule} from "@ngx-translate/core";
// import {TranslateHttpLoader} from "@ngx-translate/http-loader";
// import {from, Observable} from "rxjs";
import {IModuleTranslationOptions, ModuleTranslateLoader} from "@larscom/ngx-translate-module-loader";
// import {TranslatePoHttpLoader} from "@biesbjerg/ngx-translate-po-http-loader";

// required for AOT compilation
// export function HttpLoaderFactory(http: HttpClient): TranslateHttpLoader {
//   return new TranslateHttpLoader(http); //, 'https://raw.githubusercontent.com/peyman-mohtashami/ngx-translation-test/main/i18n/');
// }

// export function MyTranslateLoader(http: HttpClient) {
//   // let basePath: string = environment.API_BASE_PATH;
//   return new TranslatePoHttpLoader(http, 'assets/i18n/po', '.po');
// }

// export class AppTranslateLoader implements TranslateLoader {
//   getTranslation(lang: string): Observable<any> {
//     return from(import(`../assets/i18n/${lang}.json`));
//   }
// }

export function ModuleHttpLoaderFactory(http: HttpClient) {
  // const baseTranslateUrl = "https://raw.githubusercontent.com/peyman-mohtashami/ngx-translation-test/main/i18n"; // "./assets/i18n";
  const baseTranslateUrl = "./assets/i18n";

  const options: IModuleTranslationOptions = {
    translateError: (error, path) => {
      console.log("Oops! an error occurred: ", { error, path });
    },
    modules: [
      // final url: ./assets/i18n/en.json
      { baseTranslateUrl },
      // final url: ./assets/i18n/feature1/en.json
      { baseTranslateUrl, moduleName: "admin" },
      // final url: ./assets/i18n/feature2/en.json
      { baseTranslateUrl, moduleName: "auth" },
      // final url: ./assets/i18n/feature2/en.json
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
      MatProgressSpinnerModule,
      // TranslateModule.forRoot({
      //   loader: {
      //     provide: TranslateLoader,
      //     useFactory: HttpLoaderFactory,
      //     deps: [HttpClient]
      //   }
      // }),

      // TranslateModule.forRoot({
      //   defaultLanguage: 'ET',
      //   loader: {
      //     provide: TranslateLoader,
      //     useClass: AppTranslateLoader,
      //   }
      // }),
      // TranslateModule.forRoot({
      //   loader: {
      //     provide: TranslateLoader,
      //     useFactory: MyTranslateLoader,
      //     deps: [HttpClient]
      //   },
      //   // missingTranslationHandler: { provide: MissingTranslationHandler, useClass: MyMissingTranslationHandler }
      // }),
      AppRoutingModule,
    ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    // { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
