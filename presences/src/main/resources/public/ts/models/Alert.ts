export interface Alert {
    absence?: number
    lateness?: number
    incident?: number
    forgotten_notebook?: number
    alertType?: string
    students: any;
    classes: any;
    userId: string;
}