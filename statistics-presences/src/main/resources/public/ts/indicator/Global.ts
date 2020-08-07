import {Indicator} from "./index";
import {Filter, FILTER_TYPE, FilterType} from "../filter";
import {IndicatorFactory} from "../indicator";

import {Reason} from '@presences/models';

export class Global extends Indicator {
    _filterTypes: FilterType[]
    _filterEnabled: any;

    constructor(reasons: Reason[]) {
        super("Global", reasons);
        this.resetValues();
        this.setFilterTypes([
            this._factoryFilter.getFilter(FILTER_TYPE.UNJUSTIFIED_ABSENCE, null),
            this._factoryFilter.getFilter(FILTER_TYPE.JUSTIFIED_UNREGULARIZED_ABSENCE, (value: boolean) => this._factoryFilter.changeUnProvingReasons(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.REGULARIZED_ABSENCE, (value: boolean) => this._factoryFilter.changeProvingReasons(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.LATENESS, null),
            this._factoryFilter.getFilter(FILTER_TYPE.DEPARTURE, null)
        ]);

        this.enableFilter(Filter.FROM, false);
        this.enableFilter(Filter.TO, false);
        this.enableFilter(Filter.HOUR_DETAIL, false);
    }

    absenceSelected(): boolean {
        for (let type of this._filterTypes) {
            if ((type.name() === FILTER_TYPE.JUSTIFIED_UNREGULARIZED_ABSENCE || type.name() === FILTER_TYPE.UNJUSTIFIED_ABSENCE || type.name() === FILTER_TYPE.REGULARIZED_ABSENCE) && type.selected()) {
                return true;
            }
        }

        return false;
    }

    async search(start: Date, end: Date, users: string[], audiences: string[]): Promise<any> {
        const {count, data} = await super.search(start, end, users, audiences);
        this.values = {
            count,
            students: [...this.values.students, ...data]
        }
    }

    resetValues() {
        this.values = {
            count: {},
            students: []
        }
    }

    isEmpty() {
        return this.values.students.length === 0;
    }
}

IndicatorFactory.register(Global);