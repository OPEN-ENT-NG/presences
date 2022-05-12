import {IAudience} from "@common/model/Audience";

export interface IAbsence {
    id?: number;
    counsellor_regularisation?: boolean;
    created?: string;
    end_date?: string;
    followed?: boolean;
    owner?: string;
    reason_id?: number;
    reason?: AbsenceReason;
    start_date?: string;
    structure_id?: string;
    student_id?: string;
    student?: StudentAbsence;
    type?: string;
    actionAbbreviation?: string;
}

export type AbsenceEventResponse = {
    createdRegisterId: Array<number>;
    id: number;
    updatedRegisterId: Array<number>;
};

export type StudentAbsence = {
    id?: string;
    className?: string;
    name: string;
};

export type AbsenceReason = {
    id?: number;
    label?: string;
};