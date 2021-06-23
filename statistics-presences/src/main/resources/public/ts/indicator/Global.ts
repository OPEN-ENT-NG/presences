import {Indicator} from './index';
import {Filter, FILTER_TYPE, FilterType} from '../filter';
import {IndicatorFactory} from '../indicator';
import {IPunishmentType} from '@incidents/models/PunishmentType';
import {Reason} from '@presences/models';
import {INDICATOR_TYPE} from "../core/constants/IndicatorType";
import {GlobalResponse, IGlobal} from "../model/Global";
import {AxiosError} from "axios";
import {DISPLAY_TYPE} from "../core/constants/DisplayMode";
import {DateUtils} from "@common/utils";

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

    async search(start: Date, end: Date, users: string[], audiences: string[]): Promise<void> {
        await new Promise((resolve, reject) => {
            super.fetchIndicator(start, end, users, audiences)
                .then((res: GlobalResponse) => {
                    this.values = {
                        count: res.count,
                        students: [...(this.values as IGlobal).students, ...res.data]
                    };
                    resolve();
                })
                .catch((error: AxiosError) => {
                    reject(error);
                });
        });
    }

    resetValues() {
        this.values = {
            count: {},
            students: []
        }
    }

    isEmpty() {
        return (this.values as IGlobal).students.length === 0;
    }

    resetDisplayMode() {
        this._display = DISPLAY_TYPE.TABLE;
    }

    resetDates(): void {
        this._from = DateUtils.setFirstTime(new Date());
        this._to = DateUtils.setLastTime(new Date());
    }
}

IndicatorFactory.register(Global);