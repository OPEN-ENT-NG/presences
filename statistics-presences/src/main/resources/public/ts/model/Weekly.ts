import {Moment} from "moment";

export type WeeklyStatisticsResponse = {
    dayOfWeek: number,
    slot_id: string,
    total_group_occurrences: any,
    total_students_expected: WeeklyStats,
    total_events: number,
    rate: number,
    max: boolean
}

export type WeeklyStats = {
    [monthLabel: string]: number;
}

export interface IWeeklyResponse {
    data: Array<WeeklyStatisticsResponse>;
}

export interface IWeekly {
    slots: Array<WeeklyStatistics>;
}

export interface WeeklyStatistics {
    dayOfWeek: number,
    slot_id: string,
    startMoment: Moment;
    endMoment: Moment;
    is_periodic: boolean;
    locked: true;
    rate: number;
    max: boolean
}