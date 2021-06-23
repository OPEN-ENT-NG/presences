import {idiom as lang, ng} from 'entcore';
import {Indicator} from "../indicator";
import {DateUtils} from "@common/utils";
import {MonthlyStatistics} from "../model/Monthly";

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    indicator: Indicator;

    getMonthLabel(month: string): string;

    toggleAudience(audience: MonthlyStatistics);
}

export const monthlyTable = ng.directive('monthlyTable', () => {
    return {
        restrict: 'E',
        scope: {
            indicator: '='
        },
        template: `
          <div class="monthly-indicator">
            <div class="flex-table">
                <!-- header -->
                <div class="flex-head flex-row row__11" role="rowgroup">
                    <!-- classes/students -->
                    <div class="flex-col col__2 audience-student-header" role="cell">
                        <span><i18n>statistics-presences.classes</i18n>/<i18n>statistics-presences.students</i18n></span>
                    </div>
                    <!-- months fetched -->
                    <div class="flex-col indicator-values-months" role="cell">
                        
                        <div class="flex-row" role="rowgroup">
                            <div class="flex-col ellipsis" role="cell" ng-repeat="month in vm.indicator.values.months">
                                [[ vm.getMonthLabel(month) ]]
                            </div>
                        </div>
                    </div>
                    <!-- total absence -->
                    <div class="flex-col col__1" role="cell">
                        <i18n>statistics-presences.indicator.filter.type.ABSENCE_TOTAL.abbr.totale</i18n>
                    </div>
                </div>
              
                <!-- content -->
                <div class="flex-body" role="rowgroup">
                
                    <div ng-repeat="audience in vm.indicator.values.data" 
                         class="flex-content" 
                         role="rowgroup"
                         data-ng-click="vm.toggleAudience(audience)">
                         
                         
                         
                         <div ng-if="audience.students.length > 0">
                            <div class="flex-row row__11" role="rowgroup">
                         
                                <!-- Audience info -->
                                <div class="flex-col col__2 text-center audience-name" role="cell">
                                    <span>[[audience.audience]]</span>
                                </div>
                                
                                <div class="flex-col indicator-values-months" role="cell">
                                    <div class="flex-row" role="rowgroup">
                                        <div ng-repeat="month in vm.indicator.values.months"
                                             class="flex-col text-center"
                                             role="cell"
                                             ng-class="{'max-value': audience.monthsMap.get(month).max}">
                                             
                                              <!-- count -->
                                              <span ng-if="audience.monthsMap.get(month) !== null && 
                                                        audience.monthsMap.get(month).count > 0">
                                                [[audience.monthsMap.get(month).count]]
                                                
                                                 <!-- slots -->
                                                <em class="metadata"
                                                    ng-if="audience.monthsMap.get(month).slots > 0 && 
                                                    vm.indicator.filter('HOUR_DETAIL').selected && vm.indicator.absenceSelected()">
                                                    ([[audience.monthsMap.get(month).slots]] <i18n>statistics-presences.slots</i18n>)
                                                </em>
                                              </span>
                                            
                                            <!-- empty state if count undefined/0 -->
                                            <span ng-if="audience.monthsMap.get(month) === null || audience.monthsMap.get(month).count === 0">
                                                -
                                            </span>
                                        </div>
                                    </div>
                                </div>
                      
                                <!-- class count total -->
                                <div class="flex-col col__1 total-count" role="cell">
                                    [[audience.total]]
                                </div>
                            </div>
                     
                        
                        <!-- collapse -->
                        <div class="flex-collapse" data-ng-click="$event.stopPropagation()" ng-class="{'open-details': audience.isClicked}">
                            <div class="flex-content">
                                <div ng-repeat="student in audience.students" class="flex-row" role="rowgroup">
                                     <div class="flex-row row__11" role="rowgroup">
                                     
                                        <!-- student info -->
                                        <div class="flex-col col__2 student-name" role="cell">
                                            <span>[[student.name]]</span>
                                        </div>
                                        
                                        <!-- count area -->
                                        <div class="flex-col indicator-values-months" role="cell">
                                            <div class="flex-row" role="rowgroup">
                                                <div ng-repeat="month in vm.indicator.values.months"
                                                     class="flex-col text-center indicator-value"
                                                     role="cell"
                                                     ng-class="{'max-value': student.monthsMap.get(month).max}">
                                                     
                                                     <!-- count -->
                                                    <span ng-if="student.monthsMap.get(month) !== undefined 
                                                            && student.monthsMap.get(month).count > 0">
                                                        [[student.monthsMap.get(month).count]]
                                                        
                                                         <!-- slots -->
                                                        <em class="metadata"
                                                            ng-if="student.monthsMap.get(month).slots > 0 && vm.indicator.filter('HOUR_DETAIL').selected
                                                            && vm.indicator.absenceSelected()">
                                                            ([[student.monthsMap.get(month).slots]] <i18n>statistics-presences.slots</i18n>)
                                                        </em>
                                                    </span>
                                                    
                                                    <!-- empty state if count undefined/0 -->
                                                    <span ng-if="student.monthsMap.get(month) === null || 
                                                            student.monthsMap.get(month).count === 0">
                                                        -
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                        
                                         <!-- student count total -->
                                         <div class="flex-col col__1 total-count">
                                            [[student.total]]
                                        </div>
                                     </div>
                                </div>
                            </div>
                        </div>
                    </div>
                  </div>
                </div>     
            </div>
          </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        controller: function ($scope) {
            const vm: IViewModel = <IViewModel>this;

            vm.$onInit = () => {
            };

        },
        link: function ($scope) {
            const vm: IViewModel = $scope.vm;

            vm.getMonthLabel = (month: string): string => {
                return lang.translate(DateUtils.format(month, DateUtils.FORMAT["SHORT-MONTH"]));
            };

            vm.toggleAudience = (audience: MonthlyStatistics): void => {
                audience.isClicked = !audience.isClicked;
            };

        }
    };
});