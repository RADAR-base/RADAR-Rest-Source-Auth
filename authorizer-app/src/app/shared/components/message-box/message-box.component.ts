import {Component, Input} from '@angular/core';

export enum MessageBoxType {
  ERROR = 'error',
  WARNING = 'warn',
  INFO = 'info'
}

@Component({
  selector: 'app-message',
  templateUrl: './message-box.component.html',
  styleUrls: ['./message-box.component.scss']
})
export class MessageBoxComponent {
  @Input() type: MessageBoxType = MessageBoxType.ERROR;
}
