import {Reason} from '@presences/models';
import {IPunishmentType} from '@incidents/models/PunishmentType';
import {PunishmentsUtils} from "@incidents/utilities/punishments";
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";

export enum Filter {
    FROM = "FROM",
    TO = "TO",
    HOUR_DETAIL = "HOUR_DETAIL",
    PUNISHMENT_SANCTION_TYPES = "PUNISHMENT_SANCTION_TYPES"
}

export class FilterValue {
    _selected: boolean
    _value: any

    constructor(value: any, selected: boolean) {
        this._selected = selected;
        this._value = value;
    }

    get selected(): boolean {
        return this._selected;
    }

    set selected(value) {
        this._selected = value;
    }

    get value() {
        return this._value;
    }

    set value(value: any) {
        this._value = value;
    }
}

export class FilterType {
    private _name: string
    private _process: Function
    private _selected: boolean

    constructor(name: string, process: Function) {
        this._name = name;
        this._process = process;
        this._selected = true;
    }

    name(): string {
        return this._name;
    }

    get process() {
        return this._process;
    }

    set process(process) {
        this._process = process;
    }

    selected() {
        return this._selected;
    }

    select(value: boolean) {
        this._selected = value;
    }
}

export class FilterTypeFactory {
    reasons: Reason[]
    punishmentTypes: IPunishmentType[];
    reasonsMap: { [key: number]: boolean };
    punishmentTypesMap: { [key: number]: boolean };

    constructor(reasons: Reason[], punishmentTypes: IPunishmentType[]) {
        this.reasons = reasons;
        this.punishmentTypes = punishmentTypes;
        this.reasonsMap = {};
        this.punishmentTypesMap = {};
        this.reasons.forEach((reason: Reason) => this.reasonsMap[reason.id] = true);
        this.punishmentTypes.forEach((type: IPunishmentType) => this.punishmentTypesMap[type.id] = true);
    }

    public getFilter(name: string, process: Function): FilterType {
        if (process === null) process = () => null;
        return new FilterType(name, process);
    }

    public getFilterValue(value: any, selected: boolean): FilterValue {
        return new FilterValue(value, selected);
    }

    public changeUnProvingAbsences(value: boolean): void {
        this.reasons.filter((reason: Reason) => reason.reason_type_id == REASON_TYPE_ID.ABSENCE && reason.proving === false)
            .forEach((reason: Reason) => this.reasonsMap[reason.id] = value);
    }

    public changeProvingAbsences(value: boolean): void {
        this.reasons.filter((reason: Reason) => reason.reason_type_id == REASON_TYPE_ID.ABSENCE)
            .forEach((reason: Reason) => this.reasonsMap[reason.id] = value);
    }

    public changeLateness(value: boolean): void {
        this.reasons.filter((reason: Reason) => reason.reason_type_id == REASON_TYPE_ID.LATENESS || reason.id == 0)
            .forEach((reason: Reason) => {
                this.reasonsMap[reason.id] = value;
            });
    }

    private changePunishmentTypesValue(type: string, value: boolean): void {
        this.punishmentTypes
            .filter((punishmentType: IPunishmentType) => punishmentType.type === type)
            .forEach((punishmentType: IPunishmentType) => this.punishmentTypesMap[punishmentType.id] = value)
    }

    public changePunishmentFilter(value: boolean): void {
        this.changePunishmentTypesValue(PunishmentsUtils.RULES.punishment, value);
    }

    public changeSanctionFilter(value: boolean): void {
        this.changePunishmentTypesValue(PunishmentsUtils.RULES.sanction, value);
    }

    public unselectAllReasons(): void {
        this.reasons.forEach(({id}) => this.reasonsMap[id] = false);
    }
}