import {Indicator} from './index';
import {Filter, FILTER_TYPE, FilterType} from '../filter';
import {IndicatorFactory} from '../indicator';
import {IPunishmentType} from '@incidents/models/PunishmentType';
import {Reason} from '@presences/models';
import {INDICATOR_TYPE} from "../core/constants/IndicatorType";
import {GlobalResponse, GlobalStatistics, IGlobal} from "../model/Global";
import {AxiosError} from "axios";
import {DISPLAY_TYPE} from "../core/constants/DisplayMode";
import {DateUtils} from "@common/utils";
import {model} from "entcore";
import rights from "../rights";

export class Global extends Indicator {
    _filterTypes: FilterType[]
    _filterEnabled: any;

    constructor(reasons: Reason[], punishmentTypes: IPunishmentType[]) {
        super(INDICATOR_TYPE.global, reasons, punishmentTypes);
        this.resetValues();
        let filterTypes: FilterType[] = [
            this._factoryFilter.getFilter(FILTER_TYPE.NO_REASON, null),
            this._factoryFilter.getFilter(FILTER_TYPE.UNREGULARIZED, (value: boolean) => this._factoryFilter.changeUnProvingAbsences(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.REGULARIZED, (value: boolean) => this._factoryFilter.changeProvingAbsences(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.LATENESS, (value: boolean) => this._factoryFilter.changeLateness(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.DEPARTURE, null)
        ];

        if (!model.me.hasWorkflow(rights.workflow['statisticsPresences1d'])) {
            filterTypes.push(this._factoryFilter.getFilter(FILTER_TYPE.PUNISHMENT, (value: boolean) => this._factoryFilter.changePunishmentFilter(value)));
            filterTypes.push(this._factoryFilter.getFilter(FILTER_TYPE.SANCTION, (value: boolean) => this._factoryFilter.changeSanctionFilter(value)))
        }

        this.setFilterTypes(filterTypes);

        this.enableFilter(Filter.FROM, false);
        this.enableFilter(Filter.TO, false);
        this.enableFilter(Filter.HOUR_DETAIL, false);
        this.enableFilter(Filter.PUNISHMENT_SANCTION_TYPES, false);
    }

    absenceSelected(): boolean {
        for (let type of this._filterTypes) {
            if ((type.name() === FILTER_TYPE.UNREGULARIZED || type.name() === FILTER_TYPE.NO_REASON || type.name() === FILTER_TYPE.REGULARIZED) && type.selected()) {
                return true;
            }
        }

        return false;
    }

    async search(start: Date, end: Date, users: Array<string>, audiences: Array<string>): Promise<void> {
        await new Promise<void>((resolve, reject) => {
            super.fetchIndicator(start, end, users, audiences)
                .then((res: GlobalResponse): void => {
                    this.values = {
                        count: res.count,
                        rate: res.rate,
                        slots: res.slots,
                        students: [...(this.values as IGlobal).students, ...res.data]
                    };
                    resolve();
                })
                .catch((error: AxiosError) => {
                    reject(error);
                });
        });
    }

    resetValues(): void {
        this.values = {
            count: {},
            rate: {},
            slots: {},
            students: []
        };
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

    displayGlobalValue(type: string): string {
        if (this.isAbsenceType(type) && this._isRateDisplay) {
            return ((this.values as IGlobal).rate[type] ? (this.values as IGlobal).rate[type] : "0") + "%";
        }
        return (this.values as IGlobal).count[type];
    }

    displayStudentValue(studentStats: GlobalStatistics, type: string): string {
        if (this.isAbsenceType(type) && this._isRateDisplay) {
            return (studentStats.statistics[type].rate ? studentStats.statistics[type].rate : "0") + "%";
        }
        return studentStats.statistics[type].count.toString();
    }
}

IndicatorFactory.register(Global);
