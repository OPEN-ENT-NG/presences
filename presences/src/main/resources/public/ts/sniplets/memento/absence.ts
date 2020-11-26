import {angular, idiom, moment, toasts} from 'entcore';
import {DateUtils} from '@common/utils'
import {IPeriod, PeriodService} from '../../services/PeriodService'
import {IPeriodSummary, MementoService} from '../../services/MementoService';
import * as ApexCharts from 'apexcharts';
import ApexOptions = ApexCharts.ApexOptions;

enum EVENT_TYPES {
    UNREGULARIZED = "UNREGULARIZED",
    REGULARIZED = "REGULARIZED",
    NO_REASON = "NO_REASON",
    LATENESS = "LATENESS",
    DEPARTURE = "DEPARTURE"
}


interface ISummary {
    UNREGULARIZED: number
    REGULARIZED: number
    NO_REASON: number
    LATENESS: number
    DEPARTURE: number
}

interface IViewModel {
    that: any;
    disabled: boolean,
    student: string,
    group: Array<string>,
    periods: Array<IPeriod>;
    graphSummary: IPeriodSummary;
    periodSummary: IPeriodSummary;
    summary: ISummary;
    chart: ApexCharts;
    selected: { period: IPeriod };
    $eval: any;

    apply(): void;

    init(student: string, group: Array<string>): Promise<void>;

    loadPeriodData(): Promise<void>;
}

declare let window: any;

function translatePeriods(periods: Array<IPeriod>) {
    periods.forEach(period => period.label = `${idiom.translate(`viescolaire.periode.${period.type}`)}  ${period.ordre}`);
}

function countSummary() {
    vm.periodSummary.months.forEach(month => {
        vm.summary.UNREGULARIZED = vm.summary.UNREGULARIZED += month.types.UNREGULARIZED;
        vm.summary.REGULARIZED = vm.summary.REGULARIZED += month.types.REGULARIZED;
        vm.summary.NO_REASON = vm.summary.NO_REASON += month.types.NO_REASON;
        vm.summary.DEPARTURE = vm.summary.DEPARTURE += month.types.DEPARTURE;
        vm.summary.LATENESS = vm.summary.LATENESS += month.types.LATENESS;
    });
}

function getDefaultSummary() {
    return {
        UNREGULARIZED: 0,
        REGULARIZED: 0,
        NO_REASON: 0,
        LATENESS: 0,
        DEPARTURE: 0
    }
}

async function loadYearEvents(): Promise<IPeriodSummary> {
    try {
        const start: string = moment(vm.periods[0].timestamp_dt).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        const end: string = moment(vm.periods[vm.periods.length - 1].timestamp_fn).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        const types: Array<string> = [EVENT_TYPES.UNREGULARIZED, EVENT_TYPES.REGULARIZED, EVENT_TYPES.NO_REASON, EVENT_TYPES.LATENESS];
        return await MementoService.getStudentEventsSummary(vm.student, window.structure.id, start, end, types);
    } catch (err) {
        throw err;
    }
}

async function loadPeriodEvents(): Promise<IPeriodSummary> {
    try {
        const start: string = moment(vm.selected.period.timestamp_dt).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        const end: string = moment(vm.selected.period.timestamp_fn).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        const types: Array<string> = [EVENT_TYPES.UNREGULARIZED, EVENT_TYPES.REGULARIZED, EVENT_TYPES.NO_REASON, EVENT_TYPES.LATENESS];
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
        UNREGULARIZED: '#ff8a84',
        REGULARIZED: '#72bb53',
        NO_REASON: '#e61610',
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
    async init(student: string, group: Array<string>): Promise<void> {
        try {
            vm.student = student;
            vm.group = group;
            vm.summary = getDefaultSummary();
            vm.periods = await PeriodService.getPeriods(window.structure.id, group);
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