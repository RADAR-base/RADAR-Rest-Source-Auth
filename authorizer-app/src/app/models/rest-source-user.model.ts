export class RestSourceUser {
     id?: string;
     version?: string;
     projectId?: string;
     userId?: string;
     humanReadableUserId?: string;
     serviceUserId?: string;
     externalId?: string;
     sourceId?: string;
     startDate?: string;
     endDate?: string;
     externalUserId?: string;
     sourceType?: string;
     isAuthorized?: boolean;
     hasValidToken?: boolean;
     timesReset?: number;
}

export class RestSourceUsers {
     users: RestSourceUser[];
     metadata: Page;
}

export class Page {
     pageNumber: number
     pageSize: number
     totalElements: number
}
