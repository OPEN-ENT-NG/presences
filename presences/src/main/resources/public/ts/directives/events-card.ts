import {ng} from 'entcore';
import {COLOR_TYPE} from "@common/core/constants/ColorType";
import {DASHBOARD_STUDENT_EVENTS} from "../core/enum/dashboard-student-events";
import {Student} from "@common/model/Student";
import {IPeriod, Notebook} from "../services";
import {Event, EVENT_TYPES} from "../models";
import {Incident, IPunishment} from "@incidents/models";
import {DateUtils} from "@common/utils";

interface IViewModel {
    title: string;
    events: Array<Event> | Array<Incident> | Array<IPunishment> | Array<Notebook>;
    type: string;
    recoveryMethod: string;
    eventCount: number;
    color: string;
    student: Student;
    period: IPeriod;

    isEventToggled: boolean;
    hasToggledOnce: boolean;

    getColorEvent(color: string): string;

    getTwoLastEvents(events: Array<Event> | Array<Incident> | Array<IPunishment> | Array<Notebook>):
        Array<Event> | Array<Incident> | Array<IPunishment> | Array<Notebook>;

    getToggledEvents(events: Array<Event> | Array<Incident> | Array<IPunishment> | Array<Notebook>):
        Array<Event> | Array<Incident> | Array<IPunishment> | Array<Notebook>;

    getEventDate(event: Event | Incident | IPunishment | Notebook): string;

    hasOnlyTwoEvents(): boolean;

    toggleList(): void;
}

export const EventsCard = ng.directive('eventsCard', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            title: '=',
            student: '=',
            period: '=',
            events: '=',
            type: '=',
            recoveryMethod: '=',
            eventCount: '=',
            color: "="
        },
        template: `
            <div class="events-card">
                <div class="events-card-content">
                    <!--  title -->
                    <span class="events-card-content-title">[[vm.title]]</span>
                        
                    <div class="events-card-content-inside">
                        <!--  side --> 
                        <div class="events-card-content-inside-side">
                            <span class="events-card-content-inside-side-eventCount">[[vm.eventCount]]</span>
                            <span class="events-card-content-inside-side-eventOption">[[vm.recoveryMethod]]</span>
                        </div>
                        
                        <!--  events list content -->
                        <ul class="events-card-content-inside-list">
                            <div class="events-card-content-inside-list-first" ng-class="{'collapsed': vm.isEventToggled}">
                                <li class="[[vm.getColorEvent(vm.color)]]" 
                                    ng-repeat="event in vm.getTwoLastEvents(vm.events)"> 
                                    <span>[[vm.getEventDate(event)]]</span>
                                    <span>[[event.reason.label]]</span>
                                </li>
                            </div>
                            <div class="events-card-content-inside-list-full flex-collapse" ng-class="{'open-details': vm.isEventToggled}">
                                <div class="flex-content">
                                     <li class="[[vm.getColorEvent(vm.color)]]" ng-repeat="event in vm.getToggledEvents(vm.events)"> 
                                        <span>[[vm.getEventDate(event)]]</span>
                                        <span>[[event.reason.label]]</span>
                                     </li>
                                </div>
                            </div>
                        </ul>
                   </div>
                </div>
               
                <!--  increase or decrease toggle -->
                <div class="events-card-toggle [[vm.getColorEvent(vm.color)]]">
                     <!-- scroll down click -->
                     <i class="plus-sign" 
                        ng-if="!vm.hasOnlyTwoEvents() && !vm.isEventToggled"
                        data-ng-model="vm.isEventToggled"
                        data-ng-click="vm.toggleList()"></i>
                     
                     <!-- reduce scroll down -->
                     <i class="minus-sign" 
                        ng-if="!vm.hasOnlyTwoEvents() && vm.isEventToggled" 
                        data-ng-model="vm.isEventToggled"
                        data-ng-click="vm.toggleList()"></i>
                </div>
            </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        controller: function () {
            const vm: IViewModel = <IViewModel>this;

            vm.isEventToggled = false;
            vm.hasToggledOnce = false;

            vm.getColorEvent = (color: string): string => {
                switch (color) {
                    case COLOR_TYPE.red:
                        return 'red-event';
                    case COLOR_TYPE.pink:
                        return 'pink-event';
                    case COLOR_TYPE.purple:
                        return 'purple-event';
                    case COLOR_TYPE.yellow:
                        return 'yellow-event';
                    case COLOR_TYPE.turquoise:
                        return 'turquoise-event';
                    case COLOR_TYPE.blue:
                        return 'blue-event';
                    case COLOR_TYPE.grey:
                        return 'grey-event';
                }
            };

            vm.getEventDate = (event: Event | Incident | IPunishment | Notebook): string => {
                if (event) {
                    switch (vm.type) {
                        case EVENT_TYPES.JUSTIFIED:
                        case EVENT_TYPES.UNJUSTIFIED:
                        case EVENT_TYPES.LATENESS:
                        case EVENT_TYPES.DEPARTURE: {
                            return formatDayDate((<Event>event).start_date) + ' - ' +
                                formatHour((<Event>event).start_date) + ' - ' + formatHour((<Event>event).end_date);
                        }
                        case EVENT_TYPES.PUNISHMENT:
                            return (<IPunishment>event).type.label + ' ' + formatDate((<IPunishment>event).created_at);
                        case EVENT_TYPES.INCIDENT:
                            return (<Incident>event).type.label + ' ' + formatDate((<Incident>event).date.toString()) + ' ' +
                                (<Incident>event).protagonist.label;
                        case EVENT_TYPES.NOTEBOOK:
                            return (<Notebook>event).date;
                        default:
                            return '';
                    }
                } else {
                    return '';
                }
            };

            const formatHour = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);
            const formatDayDate = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-HALFYEAR"]);
            const formatDate = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["YEAR/MONTH/DAY-HOUR-MIN"]);

            vm.getTwoLastEvents = (events: Array<Event> | Array<Incident> | Array<IPunishment> | Array<Notebook>):
                Array<Event> | Array<Incident> | Array<IPunishment> | Array<Notebook> => {
                if (events) {
                    if (events.length > 1) {
                        return events.slice(0, 2);
                    } else {
                        return events;
                    }
                } else {
                    return [];
                }
            };

            vm.getToggledEvents = (events: Array<Event> | Array<Incident> | Array<IPunishment> | Array<Notebook>):
                Array<Event> | Array<Incident> | Array<IPunishment> | Array<Notebook> => {
                if (events) {
                    if (events.length > 1) {
                        return events.slice(2);
                    } else {
                        return events;
                    }
                } else {
                    return [];
                }
            };
        },
        link: function ($scope, $element: HTMLDivElement) {
            const vm: IViewModel = $scope.vm;

            vm.hasOnlyTwoEvents = (): boolean => {
                return vm.eventCount <= 2;
            };

            vm.toggleList = (): void => {
                if (vm.hasOnlyTwoEvents()) {
                    return;
                }
                vm.isEventToggled = !vm.isEventToggled;
                $scope.$emit(DASHBOARD_STUDENT_EVENTS.SEND_TOGGLE, vm.hasToggledOnce, vm.type);
                if (vm.isEventToggled) vm.hasToggledOnce = true;
            };

            $scope.$watch(() => vm.student, () => {
                vm.hasToggledOnce = false;
            });

            $scope.$watch(() => vm.period, () => {
                vm.hasToggledOnce = false;
            });
        }
    };
});