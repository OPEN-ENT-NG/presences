import {Course} from "@presences/services";
import {Presences} from "@presences/models/Presences";
import {Absence} from "@presences/models/Event/Event";
import {Reason} from "@presences/models/Reason";
import {Moment} from "moment";

export interface TimeSlotData {
    index: number;
    div: Element;
    divTimeSlots?: Element;
    timeslot: any;
    startDate: string;
    start: number;
    startMinutes: number;
    endDate: string;
    end: number;
    endMinutes: number;
    check?: boolean;
    type?: { event: string, id: number };
    isCourse?: boolean;
    isMatchingSlot?: boolean;
}

export interface ITimeSlotWithAbsence {
    slotElement: HTMLElement,
    absence: ITimeSlotAbsence,
    tsStartMoment: Moment,
    tsEndMoment: Moment,
    tsStartTimestamp: number,
    tsEndTimestamp: number,
    slotPosition: number
}

export interface ITimeSlotAbsence {
    is_periodic: boolean,
    absence: boolean,
    counsellor_regularisation?: boolean,
    followed: boolean,
    locked: boolean,
    absenceId: number,
    absenceReason: number,
    structureId: string,
    event: any[],
    startDate: string,
    startMomentDate: string,
    startMomentTime: string,
    startTimestamp: number,
    endDate: string,
    endMomentTime: string,
    endTimestamp: number
    type_id?: number
}


export interface ICalendarItems {
    courses: Array<Course>,
    presences: Presences,
    absences: Array<Absence>,
    reasons: Array<Reason>
}