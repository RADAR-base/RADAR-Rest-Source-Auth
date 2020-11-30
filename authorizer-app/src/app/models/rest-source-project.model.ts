export class RadarProject {
     id?: string;
     version?: string;
     description ?: string;
     location ?: string;
}

export class RadarSubject {
  id?: string;
  projectId?: string;
  externalId?: string;
  status?: string;
  humanReadableUserId?: string;
}
