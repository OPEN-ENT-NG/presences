import {IAudience} from "@common/model/Audience";

export interface ISchoolYearPeriod {
    id: number
    start_date: string;
    end_date: string;
    description: string;
    id_structure: string;
    code: string;
    is_opening: boolean;
}

export interface IStructureSlot {
    _id: string;
    name: string;
    slots: ITimeSlot[];
}

export interface ITimeSlot {
    name: string;
    startHour: string;
    endHour: string;
    id: string;
    _id?: string;
}

export enum TimeSlotHourPeriod {
    START_HOUR = 'startHour',
    END_HOUR = 'endHour'
}

export interface IStructure {
    id: string;
    name: string;
    classes?: Array<IAudience>;
}