import { NgModule } from '@angular/core';
import {CommonModule} from "@angular/common";
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE} from "@angular/material/core";
import {MatLegacyPaginatorIntl as MatPaginatorIntl} from "@angular/material/legacy-paginator";
import {MatBottomSheet} from "@angular/material/bottom-sheet";
import {
  MAT_MOMENT_DATE_ADAPTER_OPTIONS,
  MAT_MOMENT_DATE_FORMATS,
  MomentDateAdapter
} from "@angular/material-moment-adapter";
import {TranslateModule} from "@ngx-translate/core";

import {SharedModule} from '@app/shared/shared.module';
import {AdminRoutingModule} from "@app/admin/admin-routing.module";
import {ProjectService} from "@app/admin/services/project.service";
import {UsersListComponent} from "@app/admin/components/users-list/users-list.component";
import {UserDialogComponent} from "@app/admin/containers/user-dialog/user-dialog.component";
import {ProjectsResolver} from "@app/admin/services/projects.resolver";
import {SubjectService} from "@app/admin/services/subject.service";
import {SourceClientService} from "@app/admin/services/source-client.service";
import {SourceClientsResolver} from "@app/admin/services/source-clients.resolver";
import {DashboardPageComponent} from "@app/admin/containers/dashboard-page/dashboard-page.component";
import {CustomMatPaginatorIntl} from "@app/admin/components/users-list/custom-mat-paginator-intl";
import {LANGUAGES} from "@app/app.module";

import {LocalDatePipe} from "@app/admin/pipes/local-date.pipe";
import {LocalNumberPipe} from "@app/admin/pipes/local-number.pipe";

@NgModule({
  declarations: [
    UsersListComponent,
    DashboardPageComponent,
    UserDialogComponent,
    LocalDatePipe,
    LocalNumberPipe,
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
    {provide: MAT_DATE_LOCALE, useValue: LANGUAGES[0].locale},
    {
      provide: DateAdapter,
      useClass: MomentDateAdapter,
      deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS]
    },
    {provide: MAT_DATE_FORMATS, useValue: MAT_MOMENT_DATE_FORMATS},
    {provide: MatPaginatorIntl, useClass: CustomMatPaginatorIntl},
    MatBottomSheet,
    LocalDatePipe,
    LocalNumberPipe,
  ],
})
export class AdminModule {}
