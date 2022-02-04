import {LoadingCollection} from "@common/model";
import {IPunishmentType} from "@incidents/models/PunishmentType";
import {User} from "@common/model/User";
import {Student} from "@common/model/Student";

export enum PunishmentsRules {
    PUNISHMENT,
    SANCTION
}

export enum MassmailingsPunishments {
    PUBLISHED,
    NOT_PUBLISHED
}

export enum PunishmentsProcessStates {
    PROCESSED,
    NOT_PROCESSED
}

export interface IPunishmentRequest {
    structure_id: string;
    start_at: string;
    end_at: string;
    students_ids: Array<string>;
    groups_ids: Array<string>;
    type_ids: Array<number>;
    process_state: Array<{ label: string, isSelected: boolean, value?: string }>;
    massmailed?: boolean;
    page: number;
}

export interface IPunishmentAbsence {
    reason_id: number,
    followed: boolean
}

export interface IPunishmentAbsenceRequest {
    studentIds: Array<string>;
    startAt: string;
    endAt: string;
}

export type IPunishmentField = IPBlameField | IPDutyField | IPExcludeField | Array<IPDetentionField>;

export interface IPunishmentBody {
    id?: string;
    grouped_punishment_id?: string;
    structure_id?: string;
    fields?: IPunishmentField;
    student_ids: Array<string>;
    owner_id?: string;
    category_id?: number;
    type_id?: number;
    type?: IPunishmentType;
    incident_id?: number;
    processed?: boolean;
    student_id?: string,
    description?: string,
    absence?: IPunishmentAbsence,
}

export interface IPunishmentResponse {
    page: number;
    page_count: number;
    all: Array<IPunishment>;
}

export interface IPunishment {
    id: string;
    grouped_punishment_id: string;
    structure_id: string;
    created_at?: string;
    updated_at?: string;
    mailing_id?: number;
    fields?: IPunishmentField;
    processed?: boolean;
    description?: string;
    incident?: {};
    owner: User;
    student: Student;
    type: IPunishmentType;
}

export interface IPDutyField {
    delay_at?: string;
    instruction?: string;
}

export interface IPBlameField {
}

export interface IPExcludeField {
    start_at?: string;
    end_at?: string;
    mandatory_presence?: boolean;
}

export interface IPDetentionField {
    start_at?: string;
    end_at?: string;
    place?: string;
    id?: string;
}

export class Punishments extends LoadingCollection {
    structure_id: string;
    punishmentResponse: IPunishmentResponse;

    constructor(structure_id: string) {
        super();
        this.structure_id = structure_id;
        this.punishmentResponse = {} as IPunishmentResponse;
    }

    async build(data: IPunishmentResponse): Promise<void> {
        this.punishmentResponse.all = [];
        data.all.forEach((punishment: IPunishment) => {
            this.punishmentResponse.all.push(punishment);
        });
        this.punishmentResponse.page = data.page;
        this.punishmentResponse.page_count = data.page_count;
    }
}