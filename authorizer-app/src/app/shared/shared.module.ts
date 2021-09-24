import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {MaterialModule} from './material/material.module';
import {RouterModule} from '@angular/router';
import {ToolbarComponent} from "./components/toolbar/toolbar.component";
import {AuthorizationPageComponent} from "./containers/authorization-page/authorization-page.component";
import {AuthorizationCompletePageComponent} from "./containers/authorization-complete-page/authorization-complete-page.component";
import {ErrorComponent} from "./components/error/error.component";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {TranslateModule} from "@ngx-translate/core";

@NgModule({
  declarations: [ToolbarComponent, AuthorizationPageComponent, AuthorizationCompletePageComponent, ErrorComponent],
    imports: [
        CommonModule,
        RouterModule,
        MaterialModule,
        MatProgressSpinnerModule,
        TranslateModule,
    ],
  exports: [
    MaterialModule,
    ToolbarComponent,
    ErrorComponent,
  ]
})
export class SharedModule { }
