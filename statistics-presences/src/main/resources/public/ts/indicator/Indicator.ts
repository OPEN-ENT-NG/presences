import {idiom, moment} from "entcore";
import {Mix} from "entcore-toolkit";
import {Reason} from '@presences/models';
import {Filter, FILTER_TYPE, FilterType, FilterTypeFactory, FilterValue} from "../filter";
import {DateUtils} from "@common/utils";
import {IPunishmentType} from '@incidents/models/PunishmentType';
import {PunishmentsUtils} from "@incidents/utilities/punishments";
import {DISPLAY_TYPE} from "../core/constants/DisplayMode";
import {GlobalResponse, IGlobal} from "../model/Global";
import {IMonthly, IMonthlyGraph} from "../model/Monthly";
import {IndicatorBody} from "../model/Indicator";
import {indicatorService} from "../services";
import {IWeekly, IWeeklyResponse} from "@statistics/model/Weekly";

declare const window: any;

export interface IIndicator {
    name(): string

    i18nName(): string

    filterTypes(): FilterType[]

    filterEnabled(filter: string): boolean;

    cloneFilterTypes();

    cloneFilterTypes(): FilterType[];

    getEmptyMessage(): string
}

export abstract class Indicator implements IIndicator {
    values: IGlobal | IMonthly | IWeekly;
    graphValues: IMonthlyGraph;
    _factoryFilter: FilterTypeFactory;
    _filtersEnabled: Map<Filter, FilterValue>;
    _from: Date;
    _to: Date;
    _filterTypes: FilterType[];
    _display: string;
    _isRateDisplay: boolean;
    _name: string;
    _page: number;

    constructor(name: string, reasons: Reason[], punishmentTypes: IPunishmentType[]) {
        this.values = {};
        this.graphValues = {data: null, months: []};
        this._factoryFilter = new FilterTypeFactory(reasons, punishmentTypes);
        this._filterTypes = new Array<FilterType>();
        this._filtersEnabled = new Map([
            [Filter.FROM, this._factoryFilter.getFilterValue(0, null)],
            [Filter.TO, this._factoryFilter.getFilterValue(0, null)],
            [Filter.HOUR_DETAIL, this._factoryFilter.getFilterValue(null, null)]
        ]);
        this._from = DateUtils.setFirstTime(new Date());
        this._to = DateUtils.setLastTime(new Date());
        this._display = DISPLAY_TYPE.TABLE;
        this._isRateDisplay = false;
        this._name = name;
        this._page = 0;
    }

    filterEnabled(filter: string): boolean {
        return this._filtersEnabled.get(Filter[filter]).selected;
    }

    filter(filter: string): FilterValue {
        return this._filtersEnabled.get(Filter[filter]);
    }

    filterTypes(): FilterType[] {
        return this._filterTypes;
    }

    name(): string {
        return this._name;
    }

    i18nName(): string {
        return idiom.translate(`statistics-presences.indicator.${this._name}`);
    }

    cloneFilterTypes() {
        const filters: FilterType[] = [];
        this.filterTypes().forEach(filter => {
            const newFilter = Mix.castAs(FilterType, JSON.parse(JSON.stringify(filter)));
            newFilter.process = filter.process;
            filters.push(newFilter);
        });

        return filters;
    }

    public setFilterTypes(types: FilterType[]) {
        this._filterTypes = types;
    }

    protected enableFilter(filter: Filter, value: boolean) {
        if (this._filtersEnabled.has(filter)) {
            this._filtersEnabled.get(filter)._selected = value;
        }
    }

    protected isAbsenceType(type: string): boolean {
        return (
            type === FILTER_TYPE.ABSENCE_TOTAL ||
            type === FILTER_TYPE.UNREGULARIZED ||
            type === FILTER_TYPE.NO_REASON ||
            type === FILTER_TYPE.REGULARIZED
        );
    }

    set from(from: Date) {
        this._from = from;
    }

    get from() {
        return this._from;
    }

    set to(to: Date) {
        this._to = to;
    }

    get to() {
        return this._to;
    }

    set page(value: number) {
        this._page = value;
    }

    get page() {
        return this._page;
    }

    set display(display: string) {
        this._display = display;
    }

    get display() {
        return this._display;
    }

    set rateDisplay(_isRateDisplay: boolean) {
        this._isRateDisplay = _isRateDisplay;
    }

    get rateDisplay(): boolean {
        return this._isRateDisplay;
    }

    abstract resetValues();

    abstract resetDisplayMode();

    abstract resetDates();

    abstract isEmpty();

    getEmptyMessage(): string {
        return idiom.translate('statistics-presences.indicator.empty.state');
    }

    async fetchIndicator(start: Date, end: Date, users: string[], audiences: string[]): Promise<GlobalResponse | IMonthly | IWeeklyResponse> {
        const body = this.prepareIndicator(start, end, users, audiences);
        return indicatorService.fetchIndicator(window.structure.id, this.name(), this._page, body);
    }

    async fetchGraphIndicator(start: Date, end: Date, users: string[], audiences: string[]): Promise<IMonthlyGraph> {
        const body = this.prepareIndicator(start, end, users, audiences);
        return indicatorService.fetchGraphIndicator(window.structure.id, this.name(), body);
    }

    private prepareIndicator(start: Date, end: Date, users: string[], audiences: string[]) {
        const body: IndicatorBody = {
            start: moment(start).format(DateUtils.FORMAT['YEAR-MONTH-DAY-T-HOUR-MIN-SEC']),
            end: moment(end).format(DateUtils.FORMAT['YEAR-MONTH-DAY-T-HOUR-MIN-SEC']),
            types: [],
            filters: {},
            reasons: this.getSelectedReasons(),
            punishmentTypes: this.getSelectedPunishmentTypes(),
            sanctionTypes: this.getSelectedSanctionTypes(),
            users,
            audiences
        };
        this._filterTypes.forEach((type: FilterType) => type.selected() && body.types.push(type.name()));
        this._filtersEnabled.forEach((value: FilterValue, key: Filter) => {
            if (value.selected) {
                body.filters[key] = value.value === null ? value.selected : value.value
            }
        });
        return body;
    }

    abstract search(start: Date, end: Date, users: string[], audiences: string[]): Promise<void>;

    export(start: Date, end: Date, users: string[], audiences: string[], exportType?: string): void {
        let url: string = `/statistics-presences/structures/${window.structure.id}/indicators/${this.name()}/export?`;
        url += `start=${moment(start).format(DateUtils.FORMAT["YEAR-MONTH-DAY-T-HOUR-MIN-SEC"])}&end=${moment(end).format(DateUtils.FORMAT["YEAR-MONTH-DAY-T-HOUR-MIN-SEC"])}`;
        users.forEach(user => url += `&users=${user}`);
        audiences.forEach(audience => url += `&audiences=${audience}`);
        if (exportType) url += `&export_option=${exportType}`;
        this._filterTypes.forEach((type: FilterType) => type.selected() && (url += `&types=${type.name()}`));
        this._filtersEnabled.forEach((value: FilterValue, key: Filter) => {
            if (value.selected) {
                url += `&${key}=${value.value === null ? value.selected : value.value}`
            }
        });

        this.getSelectedPunishmentTypes().forEach((type: number) => url += `&punishmentTypes=${type}`);
        this.getSelectedSanctionTypes().forEach((type: number) => url += `&sanctionTypes=${type}`);
        this.getSelectedReasons().forEach((reason: number) => url += `&reasons=${reason}`);
        window.open(url);
    }

    private getSelectedReasons(): number[] {
        const selection: number[] = [];
        Object.keys(this._factoryFilter.reasonsMap).forEach((reason: string) => {
            if (this._factoryFilter.reasonsMap[reason]) selection.push(parseInt(reason));
        });

        return selection;
    }

    private getSelectedPunishmentTypes(): number[] {
        const selection: number[] = [];
        Object.keys(this._factoryFilter.punishmentTypesMap).forEach((type: string) => {
            if (this._factoryFilter.punishmentTypes.find((punishmentType: IPunishmentType) => punishmentType.id === parseInt(type)
                && punishmentType.type === PunishmentsUtils.RULES.punishment) !== undefined && this._factoryFilter.punishmentTypesMap[type]) {

                selection.push(parseInt(type));
            }
        });

        return selection;
    }

    private getSelectedSanctionTypes(): number[] {
        const selection: number[] = [];
        Object.keys(this._factoryFilter.punishmentTypesMap).forEach((type: string) => {
            if (this._factoryFilter.punishmentTypes.find((punishmentType: IPunishmentType) => punishmentType.id === parseInt(type)
                && punishmentType.type === PunishmentsUtils.RULES.sanction) !== undefined && this._factoryFilter.punishmentTypesMap[type]) {

                selection.push(parseInt(type));
            }
        });

        return selection;
    }

}