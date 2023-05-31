import {Me, model, moment, ng, notify, toasts} from 'entcore';
import {
    EventService,
    Group,
    GroupingService,
    GroupService,
    SearchItem,
    SearchService,
    InitService,
    IInitService, IInitStatusResponse
} from '../services';
import {DateUtils, GroupsSearch, PreferencesUtils} from '@common/utils';
import rights from '../rights';
import {Course, EventType} from '../models';
import {alertService} from '../services/AlertService';
import {IAngularEvent} from 'angular';
import {COURSE_EVENTS} from '@common/model';
import {Alert} from '@presences/models/Alert';
import {EventAbsenceSummary} from '@presences/models/Event/EventAbsenceSummary';
import {Grouping, instanceOfGrouping} from "@common/model/grouping";

declare let window: any;

interface Filter {
    search: {
        item: string;
        items: SearchItem[];
    }
}

interface ViewModel {
    filter: Filter;
    date: string;
    eventType: string[]; /* [0]:ABSENCE, [1]:LATENESS, [2]:INCIDENT, [3]:DEPARTURE */
    alert: Alert;
    course: Course;
    absencesSummary: EventAbsenceSummary;
    groupsSearch: GroupsSearch;

    isInit: Boolean;

    hasRight(right: string): boolean;

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
    'SearchService', 'GroupService', 'GroupingService', 'EventService', 'InitService',
    function ($scope, $route, $location, searchService: SearchService, groupService: GroupService, groupingService: GroupingService,
              eventService: EventService, initService: IInitService) {
        const vm: ViewModel = this;

        vm.isInit = undefined;

        const initData = async (): Promise<void> => {
            if (!window.structure) {
                window.structure = await Me.preference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_STRUCTURE);
            } else {
                initService.getViescoInitStatus(window.structure.id).then((r: IInitStatusResponse) => {
                    vm.isInit = (r.initialized !== undefined && r.initialized !== null) ? !r.initialized : null;
                });
                await Promise.all([vm.getAlert(), vm.getAbsentsCounts()])
                    .catch(error => {
                        console.log(error);
                        notify.error("presences.error.get.alert");
                    });
                vm.groupsSearch = new GroupsSearch(window.structure.id, searchService, groupService, groupingService)
            }
            $scope.safeApply();
        };

        vm.date = DateUtils.format(moment(), DateUtils.FORMAT['DATE-FULL-LETTER']);
        vm.filter = {
            search: {
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

        vm.hasRight = (right: string): boolean => {
            return model.me.hasWorkflow(rights.workflow[right]);
        };
        

        /* Calendar interaction */
        vm.selectItem = function (model, item) {
            window.item = item;
            $location.path(`/calendar/${item.id}`);
        };

        vm.searchItem = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.filter.search.items = await searchService.search(structureId, value);
                $scope.safeApply();
            } catch (err) {
                vm.filter.search.items = [];
            }
        };

        /* Registry interaction */
        vm.selectItemToRegistry = (model, item: Group | Grouping) => {
            const structureId = window.structure.id;
            window.item = item;
            let listGroup: Array<Group> = instanceOfGrouping(item) ? (<Grouping>item).groupList : [item];
            let listGroupId: Array<string> = listGroup.map((group: Group) => group.id);
            $location.path('/registry').search({
                structureId: structureId,
                month: moment(new Date()).format(DateUtils.FORMAT["YEAR-MONTH"]),
                group: listGroupId,
                type: vm.eventType,
                forgottenNotebook: true
            });
        };

        vm.searchItemToRegistry = async (value) => {
            try {
                await vm.groupsSearch.searchGroups(value);
                $scope.safeApply();
            } catch (err) {
                console.error(err);
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