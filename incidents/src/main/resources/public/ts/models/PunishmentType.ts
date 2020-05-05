import {IPunishmentCategory} from "@incidents/models/PunishmentCategory";

export interface IPunishmentType {
    id: number;
    structure_id: string;
    label: string;
    type: string;
    punishment_category_id: number;
    punishment_category?: IPunishmentCategory;
    hidden: boolean;
    used?: boolean;
    isSelected?: boolean;
}

export interface IPunishmentTypeBody {
    id?: number;
    structure_id?: string,
    label?: string;
    type?: string;
    punishment_category_id?: number;
    hidden?: boolean;
}