import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import { DevicesTableComponent } from './components/devices-table/devices-table.component';
import {HttpClientModule} from "@angular/common/http";
import {
  MatTableModule,
  MatDialogModule,
  MatCardModule,
  MatButtonModule,
  MatFormFieldModule
} from "@angular/material";
import {DevicesService} from "./services/devices.service";
import { DevicesHeaderComponent } from './components/devices-header/devices-header.component';
import { AddDeviceDialogComponent } from './components/add-device-dialog/add-device-dialog.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";

@NgModule({
  declarations: [
    AppComponent,
    DevicesTableComponent,
    DevicesHeaderComponent,
    AddDeviceDialogComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    MatTableModule,
    MatDialogModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    FormsModule,
    ReactiveFormsModule
  ],
  providers: [
    DevicesService
  ],
  entryComponents: [AddDeviceDialogComponent],
  bootstrap: [AppComponent]
})
export class AppModule { }
