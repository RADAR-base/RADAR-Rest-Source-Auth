import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import { DevicesTableComponent } from './components/devices-table/devices-table.component';
import {HttpClientModule} from "@angular/common/http";
import {MatTableModule} from "@angular/material";
import {DevicesService} from "./services/devices.service";

@NgModule({
  declarations: [
    AppComponent,
    DevicesTableComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    MatTableModule
  ],
  providers: [
    DevicesService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
