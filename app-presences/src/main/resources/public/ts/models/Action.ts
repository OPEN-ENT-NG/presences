export interface Action {
    id: number;
    structureId: string;
    label: string;
    abbreviation: string;
    used?: boolean;
    hidden: boolean;
}

export interface ActionRequest {
    id?: number;
    structureId?: string;
    label: string;
    abbreviation: string;
    hidden?: boolean;
}

export interface ActionBody {
    eventId: Array<number>;
    actionId: number;
    owner: string;
    comment?: string;
}