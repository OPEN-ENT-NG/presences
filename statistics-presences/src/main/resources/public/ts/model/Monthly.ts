import {FILTER_TYPE} from '@statistics/filter';

export type MonthlyStats = {
    [monthLabel: string]: MonthlyStat;
}

export type MonthlyStat = {
    count: number;
    slots?: number;
    max?: boolean;
}

export type MonthlyStudent = {
    name: string;
    months: Array<MonthlyStats>;
    monthsMap?: Map<string, MonthlyStat>;
    total?: number;
}

export type MonthlyStatistics = {
    audience: string;
    months: Array<MonthlyStats>;
    monthsMap?: Map<string, MonthlyStat>;
    students: Array<MonthlyStudent>;
    total?: number;
    isClicked?: boolean;
}

export interface IMonthly {
    data: Array<MonthlyStatistics>;
    months?: Array<string>;
}

export type MonthlyGraphStatistics = {
    [key in FILTER_TYPE]?: MonthlyStats;
}

export interface IMonthlyGraph {
    data: MonthlyGraphStatistics;
    months: Array<string>;
}

export type IMonthlyGraphSeries = {
    name: string;
    data: Array<number>;
}
