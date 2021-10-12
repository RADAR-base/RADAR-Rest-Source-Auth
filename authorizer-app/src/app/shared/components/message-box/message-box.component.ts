import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-message',
  templateUrl: './message-box.component.html',
  styleUrls: ['./message-box.component.scss']
})
export class MessageBoxComponent implements OnInit {

  @Input()
  message: any;

  @Input()
  type: 'error' | 'warn' | 'info' = 'error';

  messageString?: string;
  showSupport: boolean = false;

  ngOnInit() {
    this.messageString = this.getMessageString(this.message);
  }

  getMessageString(message: any): string {
    return message.error?.error_description || message.message || message;
  }
}
