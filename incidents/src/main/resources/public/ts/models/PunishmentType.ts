export interface IPunishmentType {
    id: number;
    structure_id: string;
    label: string;
    type: string;
    punishment_category_id: number;
    hidden: boolean;
    used?: boolean;
}

export interface IPunishmentTypeBody {
    id?: number;
    structure_id?: string,
    label?: string;
    type?: string;
    punishment_category_id?: number;
    hidden?: boolean;
}