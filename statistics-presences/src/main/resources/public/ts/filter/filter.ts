import {Reason} from '@presences/models';

export enum Filter {
    FROM = "FROM",
    TO = "TO",
    HOUR_DETAIL = "HOUR_DETAIL"
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
    reasonsMap: any

    constructor(reasons: Reason[]) {
        this.reasons = reasons;
        this.reasonsMap = {};
        this.reasons.map(reason => this.reasonsMap[reason.id] = true);
    }

    getFilter(name: string, process: Function): FilterType {
        if (process === null) process = () => null;
        return new FilterType(name, process);
    }

    getFilterValue(value: any, selected: boolean): FilterValue {
        return new FilterValue(value, selected);
    }

    private changeReasonsValue(proving: boolean, value: boolean) {
        this.reasons.forEach(reason => {
            if (reason.proving === proving) this.reasonsMap[reason.id] = value;
        });
    }

    changeUnProvingReasons(value: boolean): void {
        this.changeReasonsValue(false, value);
    }

    changeProvingReasons(value: boolean): void {
        this.changeReasonsValue(true, value);
    }

    unselectAllReasons(): void {
        this.reasons.forEach(({id}) => this.reasonsMap[id] = false)
    }
}