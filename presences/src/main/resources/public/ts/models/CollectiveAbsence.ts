export interface ICollectiveAbsence {
    id?: number;
    startDate: string;
    endDate: string;
    createdAt?: string;
    owner?: {
        id: string;
        displayName: string;
    };
    counsellorRegularisation?: boolean;
    audiences: Array<ICollectiveAbsenceAudience>;
    countStudent?: number;
    reason?: { id: number, label?: string };
    reasonId?: number;
    comment: string;
}

export interface ICollectiveAbsenceStudent {
    id: string;
    studentId?: string;
    displayName?: string;
    isUpdated?: boolean;
    isAbsent?: boolean;
}

export interface ICollectiveAbsenceAudience {
    id: string;
    name?: string;
    countStudents?: number;
    students?: Array<ICollectiveAbsenceStudent>;
    studentIds?: Array<string>;
    isDisplayed?: boolean;
}

export interface ICollectiveAbsencesResponse {
    page?: number;
    page_count?: number;
    all: Array<ICollectiveAbsence>;
}

export interface ICollectiveAbsenceBody {
    startDate: string;
    endDate: string;
    groups?: Array<string>;
    studentIds?: Array<string>;
    collectiveId?: number;
    page?: number;
}
