export interface Action {
    id: number;
    structureId: string;
    label: string;
    abbreviation: string;
    used?: boolean;
}

export interface ActionRequest {
    id?: number;
    structureId?: string;
    label: string;
    abbreviation: string;
}