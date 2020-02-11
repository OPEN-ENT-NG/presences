export interface Reason {
    id: number;
    label: string;
    structure_id: string;
    proving: boolean;
    comment: string;
    default: boolean;
    group: boolean;
    hidden: boolean;
    absence_compliance: boolean;
    isSelected: boolean;
}

export interface ReasonRequest {
    id?: number;
    label: string;
    absenceCompliance: boolean;
    hidden?: boolean;
    structureId?: string;
    proving: boolean
}