/**
 * Usage: dateString | localDate:'format'
 **/

import { Pipe, PipeTransform } from '@angular/core';
import { formatDate } from '@angular/common';
import { LocaleService } from '@app/admin/services/locale.service';

@Pipe({
  name: 'localDate',
  pure: false
})
export class LocalDatePipe implements PipeTransform {

  constructor(private session: LocaleService) { }

  transform(value: any, format?: string) {

    if (!value) { return ''; }
    if (!format) { format = 'shortDate'; }

    return formatDate(value, format, this.session.locale);
  }
}
