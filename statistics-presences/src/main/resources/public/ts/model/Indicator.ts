export type IndicatorBody = {
    start: string;
    end: string;
    types: Array<string>;
    filters: {};
    reasons: Array<number>;
    punishmentTypes: Array<number>;
    sanctionTypes: Array<number>;
    users: Array<string>;
    audiences: Array<string>;
    noLatenessReason?: boolean;
}