import {Component} from '@angular/core';

@Component({
  selector: 'app-error',
  template: `
  <style>
    </style>
   <div style="text-align:center;background-color: #CA7070; padding: 8px !important; height: 30px;border:0;">
   Something went wrong in the server. Please contact your system administrator</div>
  `
})
export class ErrorReportingComponent {

}
