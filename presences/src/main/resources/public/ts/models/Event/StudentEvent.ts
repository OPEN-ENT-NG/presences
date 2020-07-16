import {Event} from "./Event";

export interface IStudentEventRequest {
    structure_id: string;
    student_id: string;
    start_at: string;
    end_at: string;
    type?: Array<string>;
    limit: number;
    offset: number;
}

export interface IStudentEvents {
    JUSTIFIED: Array<Event>;
    UNJUSTIFIED: Array<Event>;
    LATENESS: Array<Event>;
    DEPARTURE: Array<Event>;
}

export interface IStudentEventsTotal {
    JUSTIFIED: number;
    UNJUSTIFIED: number;
    LATENESS: number;
    DEPARTURE: number;
}

export interface IStudentEventResponse {
    limit: number;
    offset: number;
    all: IStudentEvents;
    totals: IStudentEventsTotal;
    recovery_method: string;
}