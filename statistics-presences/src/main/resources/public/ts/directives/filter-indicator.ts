import {idiom, ng} from 'entcore';
import {Indicator} from '../indicator';
import {FILTER_TYPE, FilterType} from '../filter';
import {INDICATOR_TYPE} from '../core/constants/IndicatorType';

interface IViewModel {
    $onInit(): any;

    $onChanges(changes: any): void;

    $onDestroy(): any;

    selectFilter(filterType: FilterType): void;

    isAbsenceFilter(filterType: FilterType): boolean;

    indicator: Indicator;

    filters: FilterType[];

    translate(text: string): string;
}

export const filterIndicator = ng.directive('filterIndicator', () => {
    return {
        restrict: 'E',
        scope: {},
        template: `
             <div class="chips">
                <div class="chip" 
                     ng-repeat="filterType in vm.filters"
                     ng-class="{ selected: filterType.selected() }"
                     data-ng-click="vm.selectFilter(filterType)">
                    <span>[[vm.translate('statistics-presences.indicator.filter.type.' + filterType.name())]]</span>
                </div>
             </div>
        `,
        controllerAs: 'vm',
        bindToController: {
            indicator: "<",
            filters: "="
        },
        controller: function ($scope) {
            const vm: IViewModel = <IViewModel>this;

            vm.$onInit = () => {
            };

            vm.$onChanges = (changes: any) => {
                if (vm.indicator) {
                    switch (vm.indicator.name()) {
                        case INDICATOR_TYPE.global:
                            vm.filters.forEach((type: FilterType) => {
                                type.select(true);
                                type.process(type.selected());
                            });
                            break;
                        case INDICATOR_TYPE.monthly:
                            vm.filters.forEach((type: FilterType) => {
                                type.select(false);
                                type.process(type.selected());
                            });
                            vm.filters[0].select(true);
                            vm.filters[0].process(vm.filters[0].selected());
                            break;
                    }
                }
            };
        },
        link: function ($scope) {
            const vm: IViewModel = $scope.vm;

            vm.translate = (key: string): string => idiom.translate(key);

            vm.isAbsenceFilter = (filterType: FilterType): boolean => {
                return filterType.name() === FILTER_TYPE.NO_REASON ||
                       filterType.name() === FILTER_TYPE.UNREGULARIZED ||
                       filterType.name() === FILTER_TYPE.REGULARIZED;
            }

            vm.selectFilter = (filterType: FilterType): void => {
                switch (vm.indicator.name()) {
                    case INDICATOR_TYPE.global:
                        filterType.select(!filterType.selected());
                        filterType.process(filterType.selected());
                        break;
                    case INDICATOR_TYPE.monthly:
                    case INDICATOR_TYPE.weekly:
                        if (vm.isAbsenceFilter(filterType)) {
                            vm.filters.forEach((type: FilterType) => {
                                if (!vm.isAbsenceFilter(type)) {
                                    type.select(false);
                                    type.process(type.selected());
                                }
                            });

                        } else {
                            vm.filters.forEach((type: FilterType) => {
                                if (type.name() !== filterType.name()) {
                                    type.select(false);
                                    type.process(type.selected());
                                }
                            });
                        }

                        filterType.select(!filterType.selected());
                        filterType.process(filterType.selected());
                        break;
                }

                switch (filterType.name()) {
                    case FILTER_TYPE.REGULARIZED:
                        let unregularizedType: FilterType = vm.filters
                            .find((type: FilterType) => type.name() === FILTER_TYPE.UNREGULARIZED);
                        if (unregularizedType && unregularizedType.selected()) {
                            unregularizedType.process(true);
                        }
                        break;
                    case FILTER_TYPE.UNREGULARIZED:
                        let regularizedType: FilterType = vm.filters
                            .find((type: FilterType) => type.name() === FILTER_TYPE.REGULARIZED);
                        if (regularizedType && regularizedType.selected()) {
                            regularizedType.process(true);
                        }
                        break;
                }
            }
        }
    };
});