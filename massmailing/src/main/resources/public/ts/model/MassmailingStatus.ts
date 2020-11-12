export enum MassmailingStatus {
    REGULARIZED,
    UNREGULARIZED,
    LATENESS,
    PUNISHMENT,
    SANCTION,
    NO_REASON
}

export interface MassmailingStatusResponse {
    REGULARIZED?: number;
    UNREGULARIZED?: number;
    NO_REASON?: number;
    LATENESS?: number;
    PUNISHMENT?: number;
    SANCTION?: number;
}

export interface MassmailingAnomaliesResponse {
    id: string;
    displayName: string;
    count: MassmailingStatusResponse;
    bug: {
        SMS?: boolean,
        MAIL?: boolean,
        PDF?: boolean
    };
}