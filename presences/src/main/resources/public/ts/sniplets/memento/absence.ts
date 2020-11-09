import {angular, idiom, moment, toasts} from 'entcore';
import {DateUtils} from '@common/utils'
import {IPeriod, PeriodService} from '../../services/PeriodService'
import {IPeriodSummary, MementoService} from '../../services/MementoService';
import * as ApexCharts from 'apexcharts';
import ApexOptions = ApexCharts.ApexOptions;

enum EVENT_TYPES {
    UNJUSTIFIED = "UNJUSTIFIED",
    JUSTIFIED = "JUSTIFIED",
    LATENESS = "LATENESS",
    DEPARTURE = "DEPARTURE"
}


interface ISummary {
    UNJUSTIFIED: number
    JUSTIFIED: number
    LATENESS: number
    DEPARTURE: number
}

interface IViewModel {
    that: any;
    disabled: boolean,
    student: string,
    group: string,
    periods: Array<IPeriod>;
    graphSummary: IPeriodSummary;
    periodSummary: IPeriodSummary;
    summary: ISummary;
    chart: ApexCharts;
    selected: { period: IPeriod };
    $eval: any;

    apply(): void;

    init(student: string, group: string): Promise<void>;

    loadPeriodData(): Promise<void>;
}

declare let window: any;

function translatePeriods(periods: Array<IPeriod>) {
    periods.forEach(period => period.label = `${idiom.translate(`viescolaire.periode.${period.type}`)}  ${period.ordre}`);
}

function countSummary() {
    vm.periodSummary.months.forEach(month => {
        vm.summary.UNJUSTIFIED = vm.summary.UNJUSTIFIED += month.types.UNJUSTIFIED;
        vm.summary.JUSTIFIED = vm.summary.JUSTIFIED += month.types.JUSTIFIED;
        vm.summary.DEPARTURE = vm.summary.DEPARTURE += month.types.DEPARTURE;
        vm.summary.LATENESS = vm.summary.LATENESS += month.types.LATENESS;
    });
}

function getDefaultSummary() {
    return {
        UNJUSTIFIED: 0,
        JUSTIFIED: 0,
        LATENESS: 0,
        DEPARTURE: 0
    }
}

async function loadYearEvents(): Promise<IPeriodSummary> {
    try {
        const start: string = moment(vm.periods[0].timestamp_dt).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        const end: string = moment(vm.periods[vm.periods.length - 1].timestamp_fn).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        const types: Array<string> = [EVENT_TYPES.UNJUSTIFIED, EVENT_TYPES.JUSTIFIED, EVENT_TYPES.LATENESS, EVENT_TYPES.DEPARTURE];
        return await MementoService.getStudentEventsSummary(vm.student, window.structure.id, start, end, types);
    } catch (err) {
        throw err;
    }
}

async function loadPeriodEvents(): Promise<IPeriodSummary> {
    try {
        const start: string = moment(vm.selected.period.timestamp_dt).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        const end: string = moment(vm.selected.period.timestamp_fn).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        const types: Array<string> = [EVENT_TYPES.UNJUSTIFIED, EVENT_TYPES.JUSTIFIED, EVENT_TYPES.LATENESS, EVENT_TYPES.DEPARTURE];
        return await MementoService.getStudentEventsSummary(vm.student, window.structure.id, start, end, types);
    } catch (err) {
        throw err;
    }
}

const DEFAULT_CHART_OPTIONS: ApexOptions = {
    chart: {
        type: 'bar',
        height: 250
    },
    legend: {
        show: false,
        labels: {
            useSeriesColors: true
        }
    },
    dataLabels: {
        enabled: false
    },
    plotOptions: {
        bar: {
            endingShape: 'rounded',
            columnWidth: '70%',
            barHeight: '70%'
        }
    },
    stroke: {
        show: true,
        width: 2,
        colors: ['transparent']
    },
    yaxis: [{
        min: 0,
        labels: {
            formatter(val) {
                return val.toFixed(0);
            }
        }
    }]
};

function getCurrentPeriod(periods: Array<IPeriod> ): IPeriod {
    for (let i = 0; i < periods.length; i++) {
        if (moment().isBetween(periods[i].timestamp_dt, periods[i].timestamp_fn)) return periods[i];
    }

    return periods[0];
}

function transformGraphSummaryToChartData(): void {
    const colors = {
        JUSTIFIED: '#ff8a84',
        UNJUSTIFIED: '#e61610',
        LATENESS: '#9c29b7',
        DEPARTURE: '#24a1ac'
    };
    const seriesMap = {};
    const series = [];
    const xaxis = {
        categories: []
    };
    let maxValue = 0;
    Object.keys(colors).forEach(type => seriesMap[type] = []);
    vm.graphSummary.months.forEach(month => {
        xaxis.categories.push(idiom.translate(`presences.memento.absence.month.${month.month}`));
        Object.keys(colors).forEach(type => {
            let value = month.types[type];
            seriesMap[type].push(value);
            if (value > maxValue) maxValue = value;
        });
    });
    Object.keys(seriesMap).forEach(name => series.push({
        name: idiom.translate(`presences.memento.absence.type.${name}`),
        data: seriesMap[name]
    }));
    const colorValues = [];
    Object.keys(colors).forEach(type => colorValues.push(colors[type]));
    let defaultOptions = JSON.parse(JSON.stringify(DEFAULT_CHART_OPTIONS));
    const options = {
        ...defaultOptions,
        xaxis,
        colors: colorValues,
        yaxis: [
            {
                ...defaultOptions.yaxis[0],
                tickAmount: maxValue > 0 ? (maxValue <= 5 ? maxValue : Math.round(maxValue / 5) + 1) : 5,
                max: maxValue <= 5 ? maxValue : Math.round(maxValue / 5) * 5 + 5
            }
        ]
    };
    if (vm.chart) {
        vm.chart.updateSeries(series, true);
        vm.chart.updateOptions(options, true);
    } else {
        vm.chart = new ApexCharts(document.querySelector('#absence-chart'), {...options, series});
        vm.chart.render();
    }
}

window.transformGraphSummaryToChartData = transformGraphSummaryToChartData;

const vm: IViewModel = {
    that: null,
    disabled: false,
    student: null,
    group: null,
    periods: [],
    graphSummary: null,
    periodSummary: null,
    selected: {
        period: null
    },
    apply: null,
    $eval: null,
    chart: null,
    summary: getDefaultSummary(),
    async init(student: string, group: string): Promise<void> {
        try {
            vm.student = student;
            vm.group = group;
            vm.summary = getDefaultSummary();
            vm.periods = await PeriodService.get(window.structure.id, group);
            if (vm.periods.length === 0) {
                vm.disabled = true;
                vm.apply();
                return;
            }

            vm.disabled = false;
            translatePeriods(vm.periods);
            vm.selected.period = getCurrentPeriod(vm.periods);
            const promises = [loadYearEvents(), loadPeriodEvents()];
            const results = await Promise.all(promises);
            vm.graphSummary = results[0];
            vm.periodSummary = results[1];
            transformGraphSummaryToChartData();
            countSummary();
            vm.apply();
        } catch (e) {
            toasts.warning('presences.memento.absence.init.failed');
            throw e
        }
    },
    async loadPeriodData(): Promise<void> {
        try {
            vm.summary = getDefaultSummary();
            vm.periodSummary = await loadPeriodEvents();
            countSummary();
            vm.apply();
        } catch (err) {
            throw err;
        }
    }
};

export const absenceMementoWidget = {
    title: 'presences.memento.absence.title',
    public: false,
    controller: {
        init: function () {
            this.vm = vm;
            this.setHandler();
            vm.$eval = this.$eval;
        },
        setHandler: function () {
            if (!window.memento) return;
            this.$on('memento:init', (evt, {student, group}) => {
                const sniplet = document.getElementById('memento-absence-sniplet');
                vm.apply = angular.element(sniplet).scope().$apply;
                vm.init(student, group)
            });
            this.$on('memento:close', () => {
                if (vm.chart) {
                    vm.chart.destroy();
                    vm.chart = null;
                }
            });
        }
    }
};