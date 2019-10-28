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