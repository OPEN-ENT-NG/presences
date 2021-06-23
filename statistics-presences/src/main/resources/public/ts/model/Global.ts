export type GlobalEventType = {
    type?: string;
    count: number;
    slots?: number;
    max?: boolean;
}

export type GlobalType = {
    DEPARTURE?: number;
    PUNISHMENT?: number;
    LATENESS?: number;
    REGULARIZED?: number;
    NO_REASON?: number;
    UNREGULARIZED?: number;
    SANCTION?: number;
    STUDENTS?: number;
    ABSENCE_TOTAL?: number
};

export type GlobalStatistics = {
    id: string;
    name: string;
    audience: string;
    statistics: { [key: string]: GlobalEventType };
};

export interface IGlobal {
    students?: Array<GlobalStatistics>;
    count?: GlobalType;
}

export type GlobalResponse = {
    data: Array<GlobalStatistics>;
    count: GlobalType;
}