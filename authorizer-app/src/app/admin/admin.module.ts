import { NgModule } from '@angular/core';
import {CommonModule, DatePipe} from "@angular/common";
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/nl';
import {DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE} from "@angular/material/core";
import {TranslateModule} from "@ngx-translate/core";
import {MatPaginatorIntl} from "@angular/material/paginator";
import {MatBottomSheet} from "@angular/material/bottom-sheet";
import {
  MAT_MOMENT_DATE_ADAPTER_OPTIONS,
  MAT_MOMENT_DATE_FORMATS,
  MomentDateAdapter
} from "@angular/material-moment-adapter";

import {SharedModule} from '@app/shared/shared.module';
import {AdminRoutingModule} from "@app/admin/admin-routing.module";
import {ProjectService} from "@app/admin/services/project.service";
import {UsersListComponent} from "@app/admin/components/users-list/users-list.component";
import {UserDialogComponent} from "@app/admin/containers/user-dialog/user-dialog.component";
import {UserDeleteDialog} from "@app/admin/containers/user-delete-dialog/user-delete-dialog.component";
import {ProjectsResolver} from "@app/admin/services/projects.resolver";
import {SubjectService} from "@app/admin/services/subject.service";
import {SourceClientService} from "@app/admin/services/source-client.service";
import {SourceClientsResolver} from "@app/admin/services/source-clients.resolver";
import {LocalizedDatePipe} from "@app/admin/pipes/localized-date.pipe";
import {DashboardPageComponent} from "@app/admin/containers/dashboard-page/dashboard-page.component";
import {CustomMatPaginatorIntl} from "@app/admin/components/users-list/custom-mat-paginator-intl";

registerLocaleData(localeDe);
// // required for AOT compilation
// export function HttpLoaderFactory(http: HttpClient): TranslateHttpLoader {
//   return new TranslateHttpLoader(http, 'assets/i18n/admin/'); //, 'https://raw.githubusercontent.com/peyman-mohtashami/ngx-translation-test/main/i18n/');
// }

// export class LazyTranslateLoader implements TranslateLoader {
//   getTranslation(lang: string): Observable<any> {
//     return from(import(`../../assets/i18n/lazyModule/${lang}.json`));
//   }
// }

@NgModule({
  declarations: [
    UsersListComponent,
    DashboardPageComponent,
    UserDialogComponent,
    UserDeleteDialog,
    LocalizedDatePipe,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    AdminRoutingModule,
    TranslateModule,
  ],
  providers: [
    ProjectService,
    ProjectsResolver,
    SubjectService,
    SourceClientService,
    SourceClientsResolver,
    DatePipe,
    // { provide: MAT_DATE_LOCALE, useValue: 'en-GB' },
    // The locale would typically be provided on the root module of your application. We do it at
    // the component level here, due to limitations of our example generation script.
    {provide: MAT_DATE_LOCALE, useValue: 'en-GB'},

    // `MomentDateAdapter` and `MAT_MOMENT_DATE_FORMATS` can be automatically provided by importing
    // `MatMomentDateModule` in your applications root module. We provide it at the component level
    // here, due to limitations of our example generation script.
    {
      provide: DateAdapter,
      useClass: MomentDateAdapter,
      deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS]
    },
    {provide: MAT_DATE_FORMATS, useValue: MAT_MOMENT_DATE_FORMATS},
    MatBottomSheet,
    {provide: MatPaginatorIntl, useClass: CustomMatPaginatorIntl}
  ],
})
export class AdminModule {}
