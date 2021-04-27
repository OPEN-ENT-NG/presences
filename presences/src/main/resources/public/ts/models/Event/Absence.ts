export interface IAbsence {
    id?: number
    counsellor_regularisation?: boolean;
    created?: string;
    end_date?: string;
    followed?: boolean;
    owner?: string;
    reason_id?: number;
    start_date?: string;
    structure_id?: string;
    student_id?: string;
    type?: string;
}

export type AbsenceEventResponse = {
    createdRegisterId: Array<number>;
    id: number;
    updatedRegisterId: Array<number>;
}