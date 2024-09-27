export interface Discipline {
    id: number;
    structureId: string;
    label: string;
    isUsed?: boolean;
    hidden?: boolean;
}

export interface DisciplineRequest {
    id?: number;
    structureId?: string;
    label: string;
    hidden?: boolean;
}