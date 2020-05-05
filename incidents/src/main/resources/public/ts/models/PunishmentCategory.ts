export interface IPunishmentCategory {
    id: number;
    label: string;
    url_img: string;
}

export enum PunishmentCategoryType {
    EXTRA_DUTY = 1,
    DETENTION = 2,
    BLAME = 3,
    EXCLUSION = 4
}