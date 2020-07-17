import {Student} from "../models";
import {LoadingCollection} from "@common/model";

export interface IStatementsAbsencesResponse {
    page: number;
    page_count: number;
    all: Array<IStatementsAbsences>;
}

export interface IStatementsAbsencesRequest {
    structure_id?: string;
    student_ids?: Array<string>;
    start_at?: string;
    end_at?: string;
    isTreated?: boolean;
    page?: number;
}

export interface IStatementAbsenceBody {
    id?: number;
    structure_id?: string;
    student_id?: string;
    start_at?: string;
    end_at?: string;
    attachment?: string;
    file?: File;
    description?: string;
    treated?: string;
    isTreated?: boolean;
}

export interface IStatementsAbsences {
    id: number;
    student: Student;
    structure_id: string;
    start_at: string;
    end_at: string;
    description: string;
    attachment: string;
    treated: string;
    isTreated?: boolean;
}

export class StatementsAbsences extends LoadingCollection {
    structure_id: string;
    statementAbsenceResponse: IStatementsAbsencesResponse;

    constructor(structure_id: string) {
        super();
        this.structure_id = structure_id;
        this.statementAbsenceResponse = {} as IStatementsAbsencesResponse;
    }

    async build(data: IStatementsAbsencesResponse): Promise<void> {
        this.statementAbsenceResponse.all = [];
        data.all.forEach((punishment: IStatementsAbsences) => {
            this.statementAbsenceResponse.all.push(punishment);
        });
        this.statementAbsenceResponse.all.map((statementAbsence: IStatementsAbsences) =>
            statementAbsence.isTreated = statementAbsence.treated != null);
        this.statementAbsenceResponse.page = data.page;
        this.statementAbsenceResponse.page_count = data.page_count;
    }
}

export const mockupStatement: IStatementsAbsencesResponse = {
    page: 1,
    page_count: 2,
    all: [
        {
            id: 5,
            student: {
                id: 'fzefiuzefkzfe',
                firstName: 'Jin',
                lastName: 'DOUANG',
                name: 'Jin',
                idClasse: 'zefazefpuioazkge',
                displayName: 'DOUANG jin',
                classeName: '2ND2',
                className: '2ND2',
            } as Student,
            structure_id: '5c04e497-cb43-4589-8332-16cc8a873920',
            start_at: '2020-06-24 10:30:00',
            end_at: '2020-06-25 15:30:00',
            description: 'alors là il est en pls mdr',
            attachment: 'fezafzeigoklazgeg',
            treated: '2020-06-25 15:30:00',
        }
    ]
}