export enum MassmailingStatus {
    JUSTIFIED,
    UNJUSTIFIED,
    LATENESS,
    PUNISHMENT,
    SANCTION
}

export interface MassmailingStatusResponse {
    JUSTIFIED?: number,
    UNJUSTIFIED?: number,
    LATENESS?: number,
    PUNISHMENT?: number,
    SANCTION?: number
}

export interface MassmailingAnomaliesResponse {
    id: string
    displayName: string,
    count: MassmailingStatusResponse,
    bug: {
        SMS?: boolean,
        MAIL?: boolean,
        PDF?: boolean
    }
}