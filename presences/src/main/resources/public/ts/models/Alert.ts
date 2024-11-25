export interface Alert {
    ABSENCE?: number,
    LATENESS?: number,
    INCIDENT?: number,
    FORGOTTEN_NOTEBOOK?: number
    absence?: number
    lateness?: number
    incident?: number
    forgotten_notebook?: number
    alertType?: string
    students: any;
    classes: any;
    userId: string;
}

export interface StudentAlert {
    "student_id": string,
    "type": string,
    "count": number,
    "name": string,
    "audience": string,
    selected?: boolean
}

export interface DeleteAlertRequest {
    start_at: string,
    end_at: string,
    deleted_alert: Array<StudentAlert>,
}

export interface InfiniteScrollAlert {
    page_count:number,
    page:number,
    all: Array<StudentAlert>,
}