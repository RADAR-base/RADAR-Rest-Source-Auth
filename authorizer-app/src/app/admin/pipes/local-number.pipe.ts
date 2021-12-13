/**
 * Usage: number | localNumber:'format'
 * If no format is provided 2 decimals will be used.
 **/

import { Pipe, PipeTransform } from '@angular/core';
import { formatNumber } from '@angular/common';
import { LocaleService } from '@app/admin/services/locale.service';

@Pipe({
  name: 'localNumber',
  pure: false
})
export class LocalNumberPipe implements PipeTransform {

  constructor(private session: LocaleService) { }

  transform(value: any, format?: string) {
    if (value == null) { return ''; } // !value would also react to zeros.
    if (!format) { format = '.2-2'; }

    return formatNumber(value, this.session.locale, format);
  }
}
