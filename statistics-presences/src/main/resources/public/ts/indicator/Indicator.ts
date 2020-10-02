import {idiom, moment} from "entcore";
import {Mix} from "entcore-toolkit";
import http from 'axios';

import {Reason} from '@presences/models';
import {Filter, FilterType, FilterTypeFactory, FilterValue} from "../filter";
import {DateUtils} from "@common/utils";

declare const window: any;

export interface IIndicator {
    name(): string

    i18nName(): string

    filterTypes(): FilterType[]

    filterEnabled(filter: string): boolean;

    cloneFilterTypes();

    cloneFilterTypes(): FilterType[];
}

export abstract class Indicator implements IIndicator {
    values: any;
    _factoryFilter: FilterTypeFactory
    _filtersEnabled: Map<Filter, FilterValue>
    _filterTypes: FilterType[]
    _name: string;
    _page: number;

    constructor(name: string, reasons: Reason[]) {
        this.values = {};
        this._factoryFilter = new FilterTypeFactory(reasons);
        this._filterTypes = [];
        this._filtersEnabled = new Map([
            [Filter.FROM, this._factoryFilter.getFilterValue(0, null)],
            [Filter.TO, this._factoryFilter.getFilterValue(0, null)],
            [Filter.HOUR_DETAIL, this._factoryFilter.getFilterValue(null, null)]
        ]);
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

    set page(value: number) {
        this._page = value;
    }

    get page() {
        return this._page;
    }

    abstract resetValues();

    abstract isEmpty();

    async search(start: Date, end: Date, users: string[], audiences: string[]): Promise<any> {
        const body = {
            start: moment(start).format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
            end: moment(end).format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
            types: [],
            filters: {},
            reasons: this.getSelectedReasons(),
            users,
            audiences
        };
        this._filterTypes.forEach(type => type.selected() && body.types.push(type.name()));
        this._filtersEnabled.forEach((value: FilterValue, key: Filter) => {
            if (value.selected) {
                body.filters[key] = value.value === null ? value.selected : value.value
            }
        });
        // body.filters[Filter.HOUR_DETAIL] = this._filtersEnabled.get(Filter.HOUR_DETAIL).selected;
        const {data} = await http.post(`/statistics-presences/structures/${window.structure.id}/indicators/${this.name()}?page=${this._page}`, body);
        return data;
    }

    export(start: Date, end: Date, users: string[], audiences: string[]): void {
        let url = `/statistics-presences/structures/${window.structure.id}/indicators/${this.name()}/export?`;
        url += `start=${moment(start).format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])}&end=${moment(end).format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])}`;
        users.forEach(user => url += `&users=${user}`);
        audiences.forEach(audience => url += `&audiences=${audience}`);
        this._filterTypes.forEach(type => type.selected() && (url += `&types=${type.name()}`));
        this._filtersEnabled.forEach((value: FilterValue, key: Filter) => {
            if (value.selected) {
                url += `&${key}=${value.value === null ? value.selected : value.value}`
            }
        });
        this.getSelectedReasons().forEach(reason => url += `&reasons=${reason}`)
        window.open(url);
    }

    private getSelectedReasons() {
        const selection = [];
        Object.keys(this._factoryFilter.reasonsMap).forEach(reason => {
            if (this._factoryFilter.reasonsMap[reason]) selection.push(parseInt(reason));
        });

        return selection;
    }

}