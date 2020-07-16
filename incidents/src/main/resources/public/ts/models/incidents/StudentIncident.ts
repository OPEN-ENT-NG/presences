import {Incident, IPunishment} from "@incidents/models";

export interface IStudentIncidents {
    INCIDENT: Array<Incident>;
    PUNISHMENT: Array<IPunishment>;
}

export interface IStudentIncidentsTotal {
    INCIDENT: number;
    PUNISHMENT: number;
}

export interface IStudentIncidentResponse {
    limit: number;
    offset: number;
    all: IStudentIncidents;
    totals: IStudentIncidentsTotal;
}