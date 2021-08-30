import { HttpParams } from '@angular/common/http';

export const createRequestOption = (req?: any): HttpParams => {
  let options: HttpParams = new HttpParams();
  if (req) {
    Object.keys(req).forEach(key => {
      if (key !== 'sort' && req[key]) {
        options = options.set(key, req[key]);
      }
    });
    if (req.sort) {
      req.sort.forEach((val: string | number | boolean) => {
        options = options.append('sort', val);
      });
    }
  }
  return options;
};

interface RequestOptions {
  [key: string]: string | [string];
}
