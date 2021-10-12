import {Injectable, OnDestroy} from "@angular/core";
import {MatPaginatorIntl} from "@angular/material/paginator";
import {Subject, takeUntil} from "rxjs";
import {TranslateService} from "@ngx-translate/core";

@Injectable()
export class CustomMatPaginatorIntl extends MatPaginatorIntl
  implements OnDestroy {
  unsubscribe: Subject<void> = new Subject<void>();
  OF_LABEL = 'of';

  constructor(private translate: TranslateService) {
    super();

    this.translate.onLangChange.pipe(
      takeUntil(this.unsubscribe)
    ).subscribe(() => {
      this.getAndInitTranslations();
    });

    this.getAndInitTranslations();
  }

  ngOnDestroy() {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  getAndInitTranslations() {
    this.translate
      .get([
        'ADMIN.USERS_LIST.PAGINATOR.ITEMS_PER_PAGE',
        'ADMIN.USERS_LIST.PAGINATOR.NEXT_PAGE',
        'ADMIN.USERS_LIST.PAGINATOR.PREVIOUS_PAGE',
        'ADMIN.USERS_LIST.PAGINATOR.OF_LABEL',
      ]).pipe(
      takeUntil(this.unsubscribe)
    ).subscribe(translation => {
      this.itemsPerPageLabel =
        translation['ADMIN.USERS_LIST.PAGINATOR.ITEMS_PER_PAGE'];
      this.nextPageLabel = translation['ADMIN.USERS_LIST.PAGINATOR.NEXT_PAGE'];
      this.previousPageLabel =
        translation['ADMIN.USERS_LIST.PAGINATOR.PREVIOUS_PAGE'];
      this.OF_LABEL = translation['ADMIN.USERS_LIST.PAGINATOR.OF_LABEL'];
      this.changes.next();
    });
  }

  getRangeLabel = (page: number, pageSize: number, length: number) => {
    if (length === 0 || pageSize === 0) {
      return `0 ${this.OF_LABEL} ${length}`;
    }
    length = Math.max(length, 0);
    const startIndex = page * pageSize;
    const endIndex =
      startIndex < length
        ? Math.min(startIndex + pageSize, length)
        : startIndex + pageSize;
    return `${startIndex + 1} - ${endIndex} ${
      this.OF_LABEL
    } ${length}`;
  };
}
