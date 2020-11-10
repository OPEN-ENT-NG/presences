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