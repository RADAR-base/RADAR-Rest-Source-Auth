import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import {SharedModule} from '../shared/shared.module';
import {DashboardPageComponent} from "./containers/dashboard-page/dashboard-page.component";
import {AdminRoutingModule} from "./admin-routing.module";
import {ProjectService} from "./services/project.service";
import {CustomMatPaginatorIntl, UsersListComponent} from "./components/users-list/users-list.component";
import {UserDialogComponent} from "./containers/user-dialog/user-dialog.component";
import {UserDeleteDialog} from "./containers/user-delete-dialog/user-delete-dialog.component";
import {CommonModule, DatePipe} from "@angular/common";
import {ProjectsResolver} from "./services/projects.resolver";
import {SubjectService} from "./services/subject.service";
import {SourceService} from "./services/source.service";
import {SourceTypesResolver} from "./services/sourceTypes.resolver";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {MAT_DATE_LOCALE} from "@angular/material/core";
import {MatListModule} from "@angular/material/list";
import {MatBottomSheet} from "@angular/material/bottom-sheet";
import {TranslateModule} from "@ngx-translate/core";
import {MatPaginatorIntl} from "@angular/material/paginator";
import {LocalizedDatePipe} from "./pipes/localized-date.pipe";
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/nl';

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
    LocalizedDatePipe
    // SortAndFiltersComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    AdminRoutingModule,
    MatProgressSpinnerModule,
    MatListModule,
    TranslateModule,
    // TranslateModule.forChild({
    //   // defaultLanguage: 'ET',
    //   isolate: true,
    //   loader: {
    //     provide: TranslateLoader,
    //     useFactory: HttpLoaderFactory,
    //     deps: [HttpClient]
    //   },
    //   //loader: {provide: TranslateLoader, useClass: LazyTranslateLoader}
    // }),
    // TranslateModule.forRoot({
    //   loader: {
    //     provide: TranslateLoader,
    //     useFactory: HttpLoaderFactory,
    //     deps: [HttpClient]
    //   }
    // }),
  ],
  providers: [
    ProjectService,
    ProjectsResolver,
    SubjectService,
    SourceService,
    SourceTypesResolver,
    // LocalizedDatePipe,
    DatePipe,
    { provide: MAT_DATE_LOCALE, useValue: 'en-GB' },
    MatBottomSheet,
    {provide: MatPaginatorIntl, useClass: CustomMatPaginatorIntl}
  ],
})
export class AdminModule {}
