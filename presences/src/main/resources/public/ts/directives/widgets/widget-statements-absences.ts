import {idiom, moment, ng} from 'entcore';
import {IStatementsAbsences, IStatementsAbsencesRequest, StatementsAbsences} from "../../models";
import {IStatementsAbsencesService} from "../../services";
import {DateUtils} from "@common/utils";

declare let window: any;

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    statementsAbsences: StatementsAbsences;
    filter: IStatementsAbsencesRequest;

    formatDate(date: string): string;

    isSameDate(statement: IStatementsAbsences): boolean;

    getStartDateLabel(statement: IStatementsAbsences): string;

    redirectCalendar(statementAbsence: IStatementsAbsences): void;
}

export const WidgetStatementsAbsences = ng.directive('widgetStatementsAbsences',
    ['$location', 'StatementsAbsencesService',
        function ($location, statementsAbsenceService: IStatementsAbsencesService) {
            return {
                restrict: 'E',
                transclude: true,
                template: `
                <div class="statements-widgets statements row">
                    <!-- title -->
                    <h2><i18n>presences.widgets.statements</i18n></h2>
        
                    <div class="statements-widgets-content">
                        <!-- empty state -->
                        <div class="empty-state" data-ng-if="vm.statementsAbsences.statementAbsenceResponse.all.length === 0">
                            <div class="tick-color red">&nbsp;</div>
                            <div class="empty-state-title">
                                <i18n>presences.statistics.empty.state.2</i18n>
                            </div>
                            <div class="tick-color purple">&nbsp;</div>
                        </div>
                    
                        <!-- content-->
                        <div class="card statement cell" 
                             data-ng-if="vm.statementsAbsences.statementAbsenceResponse.all.length !== 0"
                             data-ng-click="vm.redirectCalendar(statement)"
                             data-ng-repeat="statement in vm.statementsAbsences.statementAbsenceResponse.all">
                             
                            <!-- description -->
                            <div class="statement-description">
                                <div class="statement-description-title">
                                    <span>[[statement.student.name]], [[statement.student.className]]</span>
                                </div>
                                <span class="statement-description-inside ellipsis-multiline-three">[[statement.description]]</span>
                            </div>
                            
                            
                            <!-- date -->
                            <div class="statement-date" data-ng-class="{ same: vm.isSameDate(statement) }">
                            
                                <!-- beginning of start date OR current date -->
                                <div class="statement-date-time">
                                    <span>[[vm.getStartDateLabel(statement)]]</span>
                                    <span class="font-bold">[[vm.formatDate(statement.start_at)]]</span>
                                </div>
                                
                                <!-- end of start date if no current date -->
                                <div class="statement-date-time" data-ng-if="!vm.isSameDate(statement)">
                                    <span><i18n>presences.to</i18n></span>
                                    <span class="font-bold">[[vm.formatDate(statement.end_at)]]</span>
                                </div>
                            </div>
                        </div> 
                    </div>
                   
                    
                </div>
                `,
                controllerAs: 'vm',
                bindToController: true,
                replace: true,
                controller: function ($scope) {
                    const vm: IViewModel = <IViewModel>this;

                    vm.$onInit = () => {
                        /* on  (watch) */
                        $scope.$watch(() => window.structure, () => {
                            if ('structure' in window) {
                                load();
                            }
                        });
                    };

                    const load = async (): Promise<void> => {
                        vm.statementsAbsences = new StatementsAbsences(window.structure.id);
                        vm.filter = {
                            structure_id: vm.statementsAbsences.structure_id,
                            start_at: DateUtils.format(moment(), DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                            end_at: DateUtils.format(DateUtils.setLastTime(moment()), DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                            isTreated: false,
                            limit: 6
                        }
                        vm.statementsAbsences.build(await statementsAbsenceService.get(vm.filter));
                        $scope.$apply();
                    }

                },
                link: function ($scope, $element: HTMLDivElement) {
                    const vm: IViewModel = $scope.vm;

                    vm.formatDate = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);

                    vm.isSameDate = (statement: IStatementsAbsences): boolean => {
                        return DateUtils.format(statement.start_at, DateUtils.FORMAT["DAY-MONTH-YEAR"]) ===
                            DateUtils.format(statement.end_at, DateUtils.FORMAT["DAY-MONTH-YEAR"])
                    };

                    vm.getStartDateLabel = (statement: IStatementsAbsences): string => {
                        return DateUtils.format(statement.start_at, DateUtils.FORMAT["DAY-MONTH-YEAR"]) ===
                        DateUtils.format(statement.end_at, DateUtils.FORMAT["DAY-MONTH-YEAR"]) ?
                            `${idiom.translate(`presences.the`)}` :
                            `${idiom.translate(`presences.from`)}`
                    };

                    vm.redirectCalendar = (statementsAbsence: IStatementsAbsences): void => {
                        const date: string = DateUtils.format(statementsAbsence.start_at, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                        window.item = {
                            id: statementsAbsence.student.id,
                            date: date,
                            displayName: statementsAbsence.student.name,
                            type: 'USER',
                            groupName: statementsAbsence.student.className,
                            toString: function () {
                                return statementsAbsence.student.name;
                            }
                        };
                        $location.path(`/calendar/${statementsAbsence.student.id}?date=${date}`);
                        $scope.safeApply();
                    };
                }
            };
        }]);