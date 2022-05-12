import {Me, model, moment, ng, toasts} from 'entcore';
import {EventService, Group, GroupService, SearchItem, SearchService} from '../services';
import {DateUtils, PreferencesUtils} from '@common/utils';
import rights from '../rights';
import {Course, EventType} from '../models';
import {alertService} from '../services/AlertService';
import {IAngularEvent} from 'angular';
import {COURSE_EVENTS} from '@common/model';
import {Alert} from '@presences/models/Alert';
import {EventAbsenceSummary} from '@presences/models/Event/EventAbsenceSummary';

declare let window: any;

interface Filter {
    search: {
        item: string;
        items: SearchItem[];
    }
    searchClass: {
        item: string;
        items: Group[];
    }
}

interface ViewModel {
    filter: Filter;
    date: string;
    eventType: string[]; /* [0]:ABSENCE, [1]:LATENESS, [2]:INCIDENT, [3]:DEPARTURE */
    alert: Alert;
    course: Course;
    absencesSummary: EventAbsenceSummary;

    selectItem(model: any, student: any): void;

    searchItem(value: string): void;

    selectItemToRegistry(model: any, student: any): void;

    searchItemToRegistry(value: string): void;

    getSubSize(): string;

    getAlert(): Promise<void>;

    goToAlerts(type: string): void;

    getAbsentsCounts(): Promise<void>;
}

export const dashboardController = ng.controller('DashboardController', ['$scope', '$route', '$location',
    'SearchService', 'GroupService', 'EventService',
    function ($scope, $route, $location, SearchService: SearchService, GroupService: GroupService, eventService: EventService) {
        const vm: ViewModel = this;

        const initData = async (): Promise<void> => {
            if (!window.structure) {
                window.structure = await Me.preference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_STRUCTURE);
            } else {
                await Promise.all([vm.getAlert(), vm.getAbsentsCounts()]);
            }
            $scope.safeApply();
        };

        vm.date = DateUtils.format(moment(), DateUtils.FORMAT['DATE-FULL-LETTER']);
        vm.filter = {
            search: {
                item: null,
                items: null
            },
            searchClass: {
                item: null,
                items: null
            }
        };
        vm.course = {} as Course;
        vm.eventType = [
            EventType[EventType.ABSENCE],
            EventType[EventType.LATENESS],
            EventType[EventType.INCIDENT],
            EventType[EventType.DEPARTURE]
        ];

        vm.absencesSummary = {
            nb_day_students: 0,
            nb_absents: 0,
            nb_presents: 0
        };
        

        /* Calendar interaction */
        vm.selectItem = function (model, item) {
            window.item = item;
            $location.path(`/calendar/${item.id}`);
        };

        vm.searchItem = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.filter.search.items = await SearchService.search(structureId, value);
                $scope.safeApply();
            } catch (err) {
                vm.filter.search.items = [];
            }
        };

        /* Registry interaction */
        vm.selectItemToRegistry = (model, item) => {
            const structureId = window.structure.id;
            window.item = item;
            $location.path('/registry').search({
                structureId: structureId,
                month: moment(new Date()).format(DateUtils.FORMAT["YEAR-MONTH"]),
                group: [item.id],
                type: vm.eventType,
                forgottenNotebook: true
            });
        };

        vm.searchItemToRegistry = async (value) => {
            const structureId = window.structure.id;
            try {
                vm.filter.searchClass.items = await GroupService.search(structureId, value);
                vm.filter.searchClass.items.forEach((item) => item.toString = () => item.name);
                $scope.safeApply();
            } catch (err) {
                vm.filter.searchClass.items = [];
            }
        };

        vm.getSubSize = function () {
            const SIDE_HEIGHT = 54;
            const hasRegisterWidget = model.me.hasWorkflow(rights.workflow.widget_current_course);
            const hasAbsencesWidget = model.me.hasWorkflow(rights.workflow.widget_absences);
            return `calc(100% - ${0
            + (hasRegisterWidget ? SIDE_HEIGHT : 0)
            + (hasAbsencesWidget ? SIDE_HEIGHT : 0)}px)`;
        };

        vm.getAlert = async (): Promise<void> => {
            const hasAlertWidget = model.me.hasWorkflow(rights.workflow.widget_alerts);
            try {
                let defaultAlert = {
                    ABSENCE: 0,
                    LATENESS: 0,
                    INCIDENT: 0,
                    FORGOTTEN_NOTEBOOK: 0
                };
                if (hasAlertWidget) {
                    let structureAlert: Alert = await alertService.getAlerts(window.structure.id);
                    vm.alert = {...defaultAlert, ...structureAlert};
                }
            } catch (e) {
                toasts.warning("presences.error.get.alert");
                throw e;
            }
        };

        vm.goToAlerts = function (type) {
            $location.path('/alerts').search({
                type: type
            });
        };


        /**
         * Get counts for nb of current absent students, absent day students
         * and nb of present students.
         */
        vm.getAbsentsCounts = async (): Promise<void> => {
            const structureId: string = window.structure.id;
            const canReadAbsentsCounts: boolean = model.me.hasWorkflow(rights.workflow.readAbsentsCounts);
            try {
                if (canReadAbsentsCounts) {
                    vm.absencesSummary = await eventService.getAbsentsCounts(structureId, null, null);
                }
            } catch (e) {
                vm.absencesSummary = {
                    nb_day_students: 0,
                    nb_absents: 0,
                    nb_presents: 0
                };
            }
        };


        /* Event SEND_COURSE sent from register widget */
        $scope.$on(COURSE_EVENTS.SEND_COURSE, (event: IAngularEvent, args) => vm.course = args);
        $scope.$watch(() => window.structure, async (): Promise<void> => {
            if ($route.current.action === 'dashboard') {
                await initData();
            } else {
                $scope.redirectTo('/dashboard');
            }
        });

    }]);