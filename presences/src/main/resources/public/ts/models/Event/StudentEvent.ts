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
    NO_REASON: Array<Event>;
    UNREGULARIZED: Array<Event>;
    REGULARIZED: Array<Event>;
    LATENESS: Array<Event>;
    DEPARTURE: Array<Event>;
}

export interface IStudentEventsTotal {
    NO_REASON: number;
    UNREGULARIZED: number;
    REGULARIZED: number;
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