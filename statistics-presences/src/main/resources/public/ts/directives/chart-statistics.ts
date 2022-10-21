import {idiom, ng} from 'entcore';
import {Indicator} from '../indicator';
import * as ApexCharts from 'apexcharts';
import {ApexOptions} from 'apexcharts';
import {IMonthlyGraph, IMonthlyGraphSeries} from '../model/Monthly';
import {DateUtils} from '@common/utils';
import {FilterType} from '@statistics/filter';
import {Filter} from '@statistics/controllers';

declare let window: any;

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    translate(key: string): string;

    getSeries(): Array<IMonthlyGraphSeries>;

    getColors(): Array<string>;

    getMonthLabel(month: string): string;

    selectFilter(filterType: FilterType): void;

    chart: ApexCharts;

    indicator: Indicator;

    filter: Filter
}

export const chartStatistics = ng.directive('chartStatistics', () => {
    return {
        restrict: 'E',
        scope: {
            onChange: '&'
        },
        template: `
             <div class="statistics-chart">
                <div class="statistics-chart-graph eleven centered"></div>
                    <div class="statistics-chart-legend">
                        <div class="statistics-chart-legend-item" ng-repeat="filterType in vm.indicator.filterTypes()">
                            <label class="checkbox">
                                <input type="checkbox" ng-class="filterType.name()" data-ng-checked="filterType.selected()"
                                        data-ng-click="vm.selectFilter(filterType)">                                    
                                <span>[[vm.translate('statistics-presences.indicator.filter.type.' + filterType.name())]]</span>
                            </label>
                        </div>
                    </div>
            </div>             
        `,
        controllerAs: 'vm',
        bindToController: {
            indicator: '<',
            filter: '<'
        },
        controller: function ($scope) {
            const vm: IViewModel = <IViewModel>this;
            const colorMap = {
                UNREGULARIZED: '#ff8a84',
                REGULARIZED: '#72bb53',
                NO_REASON: '#e61610',
                DEPARTURE: '#f2c9fb',
                LATENESS: '#9c29b7',
                PUNISHMENT: '#ffb600',
                SANCTION: '#d68227'
            };

            const DEFAULT_CHART_OPTIONS: ApexOptions = {
                chart: {
                    height: 450,
                    width: (window.screen.width) * 0.66,
                    type: 'line',
                    toolbar: {
                        show: false
                    }
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
                stroke: {
                    curve: 'straight',
                    width: 2
                },
                markers: {
                    size: 8
                },
                yaxis: {
                    min: 0,
                    tickAmount: 5,
                    forceNiceScale: true,
                    labels: {
                        minWidth: 30,
                    }
                }
            };

            let defaultOptions = JSON.parse(JSON.stringify(DEFAULT_CHART_OPTIONS));
            
            vm.$onInit = () => {

                $scope.$watch(() => vm.indicator.graphValues, async (newVal, oldVal) => {
                    if ((JSON.stringify((<IMonthlyGraph> newVal).data) === JSON.stringify((<IMonthlyGraph> oldVal).data)) &&
                        (JSON.stringify((<IMonthlyGraph> newVal).months) === JSON.stringify((<IMonthlyGraph> oldVal).months)))
                        return;


                    const series: Array<IMonthlyGraphSeries> = vm.getSeries();

                    const options: ApexOptions = {
                        series: series,
                        ...defaultOptions,
                        colors: vm.getColors(),
                        xaxis: {
                            labels: {
                                minHeight: 30,
                            },
                            categories: vm.indicator.graphValues.months.map((month:string) => vm.getMonthLabel(month))
                        },
                        title: {
                            text: ((vm.filter.studentsSearch.getSelectedStudents().length === 0) && (vm.filter.groupsSearch.getSelectedGroups().length === 0)) ?
                                vm.translate('statistics-presences.indicator.graph.structure.stats') + ' ' + window.structure.name : ' ',
                            align: 'left'
                        }
                    }

                    if (vm.chart) {
                        vm.chart.destroy()
                    }

                    vm.chart = new ApexCharts(document.querySelector('.statistics-chart-graph'), {...options, series});
                    vm.chart.render();
                });
            };

            vm.$onDestroy = () => {
                vm.chart.destroy();
            };

            vm.translate = (key: string): string => idiom.translate(key);

            vm.getSeries = (): IMonthlyGraphSeries[] => {

                if (vm.indicator.graphValues.data === null)
                    return;

                let series: IMonthlyGraphSeries[] = [];

                for (const type of Object.keys(vm.indicator.graphValues.data)) {
                    let serie: IMonthlyGraphSeries = {
                        name: vm.translate('statistics-presences.indicator.filter.type.' + type),
                        data: [] };

                    vm.indicator.graphValues.months.forEach((month : string, index: number) => {
                        serie.data.push(vm.indicator.graphValues.data[type][index][month].count);
                    });

                    series.push(serie);
                }

                return series;
            };

            vm.getColors = (): Array<string> => {
                if (vm.indicator.graphValues.data === null)
                    return;
                return Object.keys(vm.indicator.graphValues.data).map((type : string) => {
                    return colorMap[type];
                });
            };

            vm.getMonthLabel = (month: string): string => {
                return vm.translate(DateUtils.format(month, DateUtils.FORMAT['SHORT-MONTH']));
            };

        },
        link: function ($scope) {
            const vm: IViewModel = $scope.vm;

            vm.selectFilter = (filterType: FilterType): void => {
                filterType.select(!filterType.selected());
                filterType.process(filterType.selected());
                if ($scope.$parent.$eval($scope.onChange)) $scope.$parent.$eval($scope.onChange);
            };
        }
    };
});