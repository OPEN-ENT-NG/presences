import {Indicator} from './index';
import {Filter, FILTER_TYPE, FilterType} from '../filter';
import {IndicatorFactory} from '../indicator';
import {IPunishmentType} from '@incidents/models/PunishmentType';
import {Reason} from '@presences/models';
import {INDICATOR_TYPE} from "../core/constants/IndicatorType";

export class Global extends Indicator {
    _filterTypes: FilterType[]
    _filterEnabled: any;

    constructor(reasons: Reason[], punishmentTypes: IPunishmentType[]) {
        super(INDICATOR_TYPE.global, reasons, punishmentTypes);
        this.resetValues();
        this.setFilterTypes([
            this._factoryFilter.getFilter(FILTER_TYPE.NO_REASON, null),
            this._factoryFilter.getFilter(FILTER_TYPE.UNREGULARIZED, (value: boolean) => this._factoryFilter.changeUnProvingReasons(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.REGULARIZED, (value: boolean) => this._factoryFilter.changeProvingReasons(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.LATENESS, null),
            this._factoryFilter.getFilter(FILTER_TYPE.DEPARTURE, null),
            this._factoryFilter.getFilter(FILTER_TYPE.PUNISHMENT, (value: boolean) => this._factoryFilter.changePunishmentFilter(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.SANCTION, (value: boolean) => this._factoryFilter.changeSanctionFilter(value))
        ]);

        this.enableFilter(Filter.FROM, false);
        this.enableFilter(Filter.TO, false);
        this.enableFilter(Filter.HOUR_DETAIL, false);
    }

    absenceSelected(): boolean {
        for (let type of this._filterTypes) {
            if ((type.name() === FILTER_TYPE.UNREGULARIZED || type.name() === FILTER_TYPE.NO_REASON || type.name() === FILTER_TYPE.REGULARIZED) && type.selected()) {
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