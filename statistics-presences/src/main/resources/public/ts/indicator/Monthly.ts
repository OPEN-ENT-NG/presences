import {Indicator} from './index';
import {Filter, FILTER_TYPE, FilterType} from '../filter';
import {IndicatorFactory} from '../indicator';
import {IPunishmentType} from '@incidents/models/PunishmentType';
import {ISchoolYearPeriod, Reason} from '@presences/models';
import {INDICATOR_TYPE} from "../core/constants/IndicatorType";
import {IMonthly, IMonthlyGraph, MonthlyStat, MonthlyStatistics, MonthlyStats, MonthlyStudent} from '../model/Monthly';
import {AxiosError} from 'axios';
import {DISPLAY_TYPE} from "../core/constants/DisplayMode";
import {DateUtils} from "@common/utils";
import {ViescolaireService} from "@common/services";

declare let window: any;

export class Monthly extends Indicator {
    _filterTypes: FilterType[]
    _filterEnabled: any;

    constructor(reasons: Reason[], punishmentTypes: IPunishmentType[]) {
        super(INDICATOR_TYPE.monthly, reasons, punishmentTypes);
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

    async search(start: Date, end: Date, users: string[], audiences: string[]): Promise<void> {
        if (this._display === DISPLAY_TYPE.TABLE) {
            await this.searchTable(start, end, users, audiences);
        } else {
            await this.searchGraph(start, end, users, audiences);
        }
    }

    async searchTable(start: Date, end: Date, users: string[], audiences: string[]): Promise<void> {
        await new Promise((resolve, reject) => {
            super.fetchIndicator(start, end, users, audiences)
                .then((res: IMonthly) => {
                    this._mapResults(res);
                    resolve();
                })
                .catch((error: AxiosError) => {
                    reject(error);
                });
        });
    }


    async searchGraph(start: Date, end: Date, users: string[], audiences: string[]): Promise<void> {
        await new Promise((resolve, reject) => {
            super.fetchGraphIndicator(start, end, users, audiences)
                .then((res: IMonthlyGraph) => {
                    this.graphValues = res;
                    resolve();
                })
                .catch((error: AxiosError) => {
                    reject(error);
                });
        });
    }

    private _mapResults(res: IMonthly): void {

        const fetchedMonthsFromData: MonthlyStatistics = res.data.find((m: MonthlyStatistics) => m.months && m.months.length !== 0);

        let months: string[] = fetchedMonthsFromData ? fetchedMonthsFromData.months.map((t: MonthlyStats) => Object.keys(t).reduce((acc: string) => acc)) : [];

        res.data.forEach((monthlyStatistic: MonthlyStatistics) => {

            if (monthlyStatistic.monthsMap === null || monthlyStatistic.monthsMap === undefined) {
                monthlyStatistic.monthsMap = new Map<string, MonthlyStat>();
            }

            this._mapMonths(months, monthlyStatistic.months,monthlyStatistic.monthsMap);

            monthlyStatistic.students.forEach((student: MonthlyStudent) => {
                if (student.monthsMap === null || student.monthsMap === undefined) {
                    student.monthsMap = new Map<string, MonthlyStat>();
                }
                this._mapMonths(months, student.months, student.monthsMap);
            });
        });

        let data: Array<MonthlyStatistics> = (this.values as IMonthly).data.concat(res.data);
        data = data.filter((item: MonthlyStatistics, index: number) => { return data.indexOf(item) === index});

        let monthsValue: Array<string> = (this.values as IMonthly).months.concat(months);
        monthsValue = monthsValue.filter((item: string, index: number) => { return monthsValue.indexOf(item) === index});

        this.values = { data: data, months: monthsValue };
    }


    private _mapMonths(monthLabels: string[], months: MonthlyStats[], monthsMap: Map<string, MonthlyStat>): void {

        monthLabels.forEach((month: string) => {
            if (!monthsMap.has(month)) {
                if (months.find((statsMonth: MonthlyStats) => statsMonth[month] !== undefined) !== undefined) {
                    monthsMap.set(month,
                        months.find((statsMonth: MonthlyStats) => statsMonth[month] !== undefined)[month]);
                } else {
                    monthsMap.set(month, null);
                }
            }
        });
    }

    resetValues(): void {
        this.values = {
            data: [],
            months: []
        };

        this.graphValues = {
            data: null,
            months: []
        };
    }

    resetDisplayMode(): void {
        this._display = DISPLAY_TYPE.TABLE;
    }

    async resetDates(): Promise<void> {
        const schoolYear: ISchoolYearPeriod = await ViescolaireService.getSchoolYearDates(window.structure.id);
        this._from = DateUtils.setFirstTime(schoolYear.start_date);
        this._to = DateUtils.setLastTime(new Date());
    }

    isEmpty(): boolean {
        return (this.values as IMonthly).data.every((item: MonthlyStatistics) => item.students.length === 0)
            && (this.graphValues as IMonthly).data === null;
    }
}

IndicatorFactory.register(Monthly);