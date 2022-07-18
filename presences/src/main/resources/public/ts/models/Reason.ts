import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";
import {ALERT_RULE} from "@common/core/enum/alert-rule";

export interface Reason {
    id: number;
    label: string;
    structure_id: string;
    proving: boolean;
    comment: string;
    default: boolean;
    group: boolean;
    hidden: boolean;
    absence_compliance: boolean;
    isSelected: boolean;
    reason_type_id?: REASON_TYPE_ID;
    reason_alert_rules?: Array<ALERT_RULE>;
}

export interface ReasonRequest {
    id?: number;
    label: string;
    absenceCompliance: boolean;
    hidden?: boolean;
    structureId?: string;
    proving: boolean;
    reasonTypeId?: REASON_TYPE_ID;
    excludeAlertRegularised: boolean;
    excludeAlertNoRegularised: boolean;
    excludeAlertLateness: boolean;
}