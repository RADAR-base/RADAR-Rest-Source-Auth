import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-error',
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.scss']
})
export class ErrorComponent implements OnInit {

  @Input()
  error: any;

  errorMessage?: string;

  ngOnInit() {
    this.errorMessage = this.handleError(this.error);
  }

  handleError(error: any): string {
    console.log(error);
    return error.error.error_description || error.message;
  }
}
