import {Course} from "@presences/services";
import {Presences} from "@presences/models/Presences";
import {Absence} from "@presences/models/Event";
import {Reason} from "@presences/models/Reason";

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
    absence: any,
    tsStartMoment: any,
    tsEndMoment: any,
    tsStartTimestamp: number,
    tsEndTimestamp: number,
    slotPosition: number
}

export interface ICalendarItems {
    courses: Array<Course>,
    presences: Presences,
    absences: Array<Absence>,
    reasons: Array<Reason>
}