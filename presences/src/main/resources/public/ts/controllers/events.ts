import {_, angular, idiom as lang, moment, ng} from 'entcore';
import {Absence, EventResponse, Events, EventType, Student, Students} from "../models";
import {DateUtils} from "@common/utils";
import {GroupService} from "@common/services/GroupService";
import {Reason, ReasonService} from "../services";
import {EventsFilter, EventsUtils} from "../utilities";

declare let window: any;

interface ViewModel {
    filter: EventsFilter;

    /* Get reasons type */
    eventReasonsType: Reason[];
    eventReasonsTypeDescription: Reason[];

    /* Events */
    eventType: number[];
    events: Events;
    multipleSelect: Reason;
    provingReasonsMap: any;

    eventTypeState(periods, event): string;

    editPeriod($event, event): void;

    reasonSelect($event): void;

    getRegularizedValue(event): boolean;

    filterSelect(options: Reason[], event): Reason[];

    changeReason(event, index): Promise<void>;

    downloadFile($event): void;

    doAction($event): void;

    stopAbsencePropagation($event): void;

    regularizedChecked(event: EventResponse): boolean;

    toggleAllAbsenceRegularised(event: EventResponse, index: number): void;

    toggleAbsenceRegularised(history, event, index: number): void;

    getNonRegularizedEvents(events): any[];

    hideGlobalCheckbox(event): boolean;

    /* Events description */
    changeDescriptionReason(periods, event, index: number): void;

    /* Collapse event */
    eventId: number;
    collapse: boolean;

    toggleCollapse(event): void;

    isCollapsibleOpen($index): boolean;

    /* Students */
    studentSearchInput: string;
    students: Students;

    searchByStudent(string): void;

    selectStudent(model: Student, option: Student): void;

    selectStudentFromDashboard(model: Student, option: Student): void;

    excludeStudentFromFilter(audience): void;

    /* Classes */
    classesSearchInput: string;
    classes: any;
    classesFiltered: any[];

    searchByClass(value: string): Promise<void>;

    selectClass(model: any, option: any): void;

    excludeClassFromFilter(audience): void;

    /* update filter */
    updateFilter(student?, audience?): void;

    updateDate(): void;

    /*  switch event type */
    switchAbsencesFilter(): void;

    switchLateFilter(): void;

    switchDepartureFilter(): void;

    switchRegularizedFilter(): Promise<void>;

    /* Export*/
    exportPdf(): void;

    exportCsv(): void;
}

export const eventsController = ng.controller('EventsController', ['$scope', '$route', '$location',
    'GroupService', 'ReasonService',
    function ($scope, $route, $location, GroupService: GroupService, ReasonService: ReasonService) {
        const isWidget = $route.current.action === 'dashboard';
        const vm: ViewModel = this;
        vm.filter = {
            startDate: isWidget ? DateUtils.add(new Date(), -5, "d") : DateUtils.add(new Date(), -30, "d"),
            endDate: isWidget ? DateUtils.add(new Date(), -1, "d") : moment().endOf('day').toDate(),
            students: [],
            classes: [],
            absences: true,
            departure: true,
            late: $route.current.action !== 'dashboard',
            regularized: true,
        };
        vm.provingReasonsMap = {};
        vm.eventType = [];
        vm.multipleSelect = {
            id: 0,
            label: lang.translate("presences.absence.select.multiple"),
            structure_id: "",
            comment: "",
            default: false,
            proving: false,
            group: false
        } as Reason;
        vm.studentSearchInput = '';
        vm.classesSearchInput = '';
        vm.students = new Students();
        vm.classesFiltered = undefined;

        vm.events = new Events();
        vm.events.regularized = isWidget ? vm.filter.regularized : null;
        vm.events.eventer.on('loading::true', () => $scope.safeApply());
        vm.events.eventer.on('loading::false', () => {
            filterHistory();
            $scope.safeApply();
        });
        const getEvents = async (): Promise<void> => {
            vm.events.structureId = window.structure.id;
            vm.events.startDate = vm.filter.startDate.toDateString();
            vm.events.endDate = vm.filter.endDate.toDateString();
            vm.events.regularized = vm.filter.regularized;

            if (vm.filter.absences) {
                if (!vm.eventType.some(e => e == EventType.ABSENCE)) {
                    vm.eventType.push(EventType.ABSENCE);
                }
            }
            if (vm.filter.late) {
                if (!vm.eventType.some(e => e == EventType.LATENESS)) {
                    vm.eventType.push(EventType.LATENESS);
                }
            }
            if (vm.filter.departure) {
                if (!vm.eventType.some(e => e == EventType.DEPARTURE)) {
                    vm.eventType.push(EventType.DEPARTURE);
                }
            }
            vm.events.eventType = vm.eventType.toString();

            if (!vm.eventReasonsType || vm.eventReasonsType.length <= 1) {
                vm.eventReasonsType = await ReasonService.getReasons(window.structure.id);
                vm.eventReasonsTypeDescription = _.clone(vm.eventReasonsType);
                vm.eventReasonsType.map((reason: Reason) => vm.provingReasonsMap[reason.id] = reason.proving);
                if (!isWidget) vm.eventReasonsType.push(vm.multipleSelect);
            }

            EventsUtils.setStudentToSync(vm.events, vm.filter);
            EventsUtils.setClassToSync(vm.events, vm.filter);
            // "page" uses sync() method at the same time it sets 0 (See LoadingCollection Class)
            vm.events.page = 0;
            $scope.safeApply();
        };

        const filterHistory = (): void => {
            vm.events.all = vm.events.all.filter(e => e.exclude !== true);
            vm.events.all.forEach(event => {
                event.events = EventsUtils.filterHistory(event.events);
                $scope.safeApply();
            });
        };

        vm.editPeriod = ($event, {studentId, date, displayName, className, classId}): void => {
            $event.stopPropagation();
            window.item = {
                id: studentId,
                date,
                displayName,
                type: 'USER',
                groupName: className,
                groupId: classId
            };
            $location.path(`/calendar/${studentId}?date=${date}`);
            $scope.safeApply();
        };

        /* Change CSS class depending on their event_type id */
        vm.eventTypeState = (periods, event): string => {
            if (periods.events.length === 0) return '';
            const priority = [EventType.ABSENCE, EventType.LATENESS, EventType.DEPARTURE, EventType.REMARK];
            const className = ['absent', 'late', 'departure', 'remark', 'justified', 'empty'];
            let index = 4;
            for (let i = 0; i < periods.events.length; i++) {
                if ("type_id" in periods.events[i]) {
                    if (periods.events[i].type_id === 1) {
                        index = periods.events[i].reason_id !== null ? 4 : 0;
                    } else if (periods.events[i].type_id === 2) {
                        index = 1;
                    } else {
                        let arrayIndex = priority.indexOf(periods.events[i].type_id);
                        index = arrayIndex < index ? arrayIndex : index;
                    }
                } else {
                    index = periods.events[i].reason_id != null ? 4 : 0;
                }
            }
            return className[index] || '';
        };

        vm.reasonSelect = ($event): void => {
            $event.stopPropagation();
        };

        /* filtering by removing multiple choices if there is no reason_id */
        vm.filterSelect = function (options: Reason[], event): Reason[] {
            let reasonIds = EventsUtils.getReasonIds(event.events);
            if (reasonIds.every((val, i, arr) => val === arr[0])) {
                return options.filter(option => option.id !== 0);
            }
            return options;
        };

        /* Add global reason_id to all events that exist */
        vm.changeReason = async (event: EventResponse, index: number): Promise<void> => {
            /* Fetch all event id */
            let eventsArrayId = [];
            let absencesArrayId = [];
            if (isWidget) {
                eventsArrayId.push(event.id);
                event.globalReason = event.reason_id;
            } else {
                event.dayHistory.forEach(periods => {
                    periods.events.forEach(period => {
                        if ("reason_id" in period) {
                            period.reason_id = event.globalReason;
                            EventsUtils.addEventsAndAbsencesArray(period, eventsArrayId, absencesArrayId);
                        }
                    })
                });
                event.events.forEach(item => {
                    item.reason_id = event.globalReason;
                    item.counsellor_regularisation = vm.provingReasonsMap[item.reason_id];
                    if ('events' in item && item.events.length > 0) {
                        item.events.forEach(itemEvent => {
                            eventsArrayId.push(itemEvent.id);
                        })
                    }
                });
            }
            await Promise.all([
                vm.events.updateReason(eventsArrayId, event.globalReason),
                new Absence(null, null, null, null)
                    .updateAbsenceReason(absencesArrayId, event.globalReason)
            ]).then(() => {
                if (isWidget) vm.events.page = 0;
            });
            event.globalCounsellorRegularisation = EventsUtils.initGlobalCounsellorRegularisation(event);
            if (vm.filter.regularized && vm.provingReasonsMap[event.globalReason]) vm.events.all = vm.events.all.filter((evt, i) => i !== index);
            $scope.safeApply();
        };

        vm.downloadFile = ($event): void => {
            $event.stopPropagation();
            console.log("downloading File");
        };

        vm.doAction = ($event): void => {
            $event.stopPropagation();
            console.log("do action");
        };

        vm.stopAbsencePropagation = ($event): void => {
            $event.stopPropagation();
        };

        vm.regularizedChecked = (event: EventResponse): boolean => {
            let regularized = [];
            event.events.forEach((elem) => {
                regularized.push(elem.counsellor_regularisation);
                if ('events' in elem && elem.events.length > 0) {
                    elem.events.forEach(itemEvent => {
                        regularized.push(itemEvent.counsellor_regularisation);
                    });
                }
            });
            return !regularized.every((val, i, arr) => val === arr[0]) && !event.globalCounsellorRegularisation;
        };

        vm.toggleAllAbsenceRegularised = (event: EventResponse, index: number): void => {
            let eventsId = [];
            let absencesId = [];
            event.dayHistory.forEach(history => {
                history.events.forEach(e => {
                    if (e.reason_id !== null || !vm.provingReasonsMap[e.reason_id]) {
                        e.counsellor_regularisation = event.globalCounsellorRegularisation;
                    }
                });
            });
            event.events.forEach(item => {
                if (item.reason_id !== null || !vm.provingReasonsMap[item.reason_id]) {
                    item.counsellor_regularisation = event.globalCounsellorRegularisation;
                    EventsUtils.addEventsAndAbsencesArray(item, eventsId, absencesId);
                    if ('events' in item && item.events.length > 0) {
                        item.events.forEach(itemEvent => {
                            itemEvent.counsellor_regularisation = event.globalCounsellorRegularisation;
                            eventsId.push(itemEvent.id);
                        })
                    }
                }
            });
            if (eventsId.length > 0) vm.events.updateRegularized(eventsId, event.globalCounsellorRegularisation);
            if (absencesId.length > 0) new Absence(null, null, null, null)
                .updateAbsenceRegularized(absencesId, event.globalCounsellorRegularisation);
            if (vm.filter.regularized) {
                event.events = event.events.filter((event) =>
                    eventsId.indexOf(event.id) === -1 && absencesId.indexOf(event.id) === -1
                );
            }
            if (event.events.length === 0 && vm.filter.regularized) {
                vm.events.all = vm.events.all.filter((evt, i) => i !== index);
                vm.eventId = null;
            }
        };

        vm.toggleAbsenceRegularised = (history, event, index: number): void => {
            if (history.type === EventsUtils.ALL_EVENTS.event) {
                let eventsId = [history.id];
                vm.events.updateRegularized(eventsId, history.counsellor_regularisation);
            } else {
                let absencesId = [history.id];
                new Absence(null, null, null, null)
                    .updateAbsenceRegularized(absencesId, history.counsellor_regularisation);
                if ('events' in history && history.events.length > 0) {
                    let eventsId = [];
                    history.events.forEach(itemEvent => {
                        itemEvent.counsellor_regularisation = history.counsellor_regularisation;
                        eventsId.push(itemEvent.id);
                    });
                    vm.events.updateRegularized(eventsId, history.counsellor_regularisation);
                }
            }

            if (!isWidget) {
                EventsUtils.manageEventDrop(vm.events, vm.filter, vm.eventId, history, event, index);
                if (event.events.length > 0) event.globalCounsellorRegularisation = EventsUtils.initGlobalCounsellorRegularisation(event);
            } else {
                vm.events.page = 0;
            }
        };

        vm.getNonRegularizedEvents = (events): any[] => {
            return events.filter(item => item.counsellor_regularisation === false);
        };

        /* Toggle Collapse */
        vm.toggleCollapse = (event): void => {
            if (vm.eventId == event.currentTarget.getAttribute("data-id")) {
                vm.collapse = !vm.collapse;
                if (vm.collapse) {
                    vm.eventId = event.currentTarget.getAttribute("data-id");
                } else {
                    vm.eventId = null;
                }
            } else {
                vm.collapse = true;
                vm.eventId = event.currentTarget.getAttribute("data-id");
            }
            $scope.safeApply();
        };

        /* Open the concerned event */
        vm.isCollapsibleOpen = ($index): boolean => {
            return $index == vm.eventId
        };

        /* Change its description reason id */
        vm.changeDescriptionReason = (history, event: EventResponse, index): void => {
            let eventsArrayId = [];
            let absencesArrayId = [];
            event.dayHistory.forEach(e => {
                e.events.forEach(item => {
                    if (item.id === history.id) {
                        item.reason_id = history.reason_id;

                        // check if id already exist in event array
                        if ("type_id" in item) {
                            if (eventsArrayId.indexOf(item.id) === -1) {
                                eventsArrayId.push(item.id);
                            }
                        } else {
                            // check if id already exist in absence array
                            if (absencesArrayId.indexOf(item.id) === -1) {
                                absencesArrayId.push(item.id);
                            }
                        }
                        if ('events' in item && item.events.length > 0) {
                            item.events.forEach(itemEvent => {
                                eventsArrayId.push(itemEvent.id);
                            })
                        }

                    }
                });
            });
            if ("type_id" in history) {
                vm.events.updateReason(eventsArrayId, history.reason_id);
            } else {
                if (absencesArrayId.length > 0)
                    new Absence(null, null, null, null)
                        .updateAbsenceReason(absencesArrayId, history.reason_id);
            }
            history.counsellor_regularisation = vm.provingReasonsMap[history.reason_id];
            EventsUtils.manageEventDrop(vm.events, vm.filter, vm.eventId, history, event, index);
        };

        /* ----------------------------
          Student methods
        ---------------------------- */
        vm.searchByStudent = async (searchText: string) => {
            await vm.students.search(window.structure.id, searchText);
            $scope.safeApply();
        };

        vm.selectStudent = function (model: Student, option: Student) {
            vm.updateFilter(option);
            vm.studentSearchInput = '';
        };

        vm.selectStudentFromDashboard = function (model: Student, option: Student) {
            vm.filter.students = [];
            vm.selectStudent(model, option);
        };

        vm.excludeStudentFromFilter = (student) => {
            vm.filter.students = _.without(vm.filter.students, _.findWhere(vm.filter.students, student));
            vm.updateFilter();
        };

        /* ----------------------------
          Classes methods
        ---------------------------- */
        vm.searchByClass = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.classesFiltered = await GroupService.search(structureId, value);
                vm.classesFiltered.map((obj) => obj.toString = () => obj.name);
                $scope.safeApply();
            } catch (err) {
                vm.classesFiltered = [];
                throw err;
            }
            return;
        };

        vm.selectClass = (model: Student, option: Student): void => {
            vm.updateFilter(null, option);
            vm.classesSearchInput = '';
        };

        vm.excludeClassFromFilter = (audience) => {
            vm.filter.classes = _.without(vm.filter.classes, _.findWhere(vm.filter.classes, audience));
            vm.updateFilter();
        };

        /* ----------------------------
          update filter methods
        ---------------------------- */
        vm.updateFilter = (student?, audience?) => {
            if (audience && !_.find(vm.filter.classes, audience)) {
                vm.filter.classes.push(audience);
            }
            if (student && !_.find(vm.filter.students, student)) {
                vm.filter.students.push(student);
            }
            vm.events.eventType = vm.eventType.toString();
            EventsUtils.setStudentToSync(vm.events, vm.filter);
            EventsUtils.setClassToSync(vm.events, vm.filter);
            vm.events.page = 0;
            $scope.safeApply();
        };

        vm.updateDate = async () => {
            getEvents();
            $scope.safeApply();
        };

        /* ----------------------------
          Export methods
        ---------------------------- */
        vm.exportPdf = function () {
            console.log("exporting Pdf");
        };

        vm.exportCsv = function () {
            console.log("exporting Csv");
        };

        /* ----------------------------
         Switch type methods
        ---------------------------- */
        vm.switchAbsencesFilter = function () {
            vm.filter.absences = !vm.filter.absences;
            if (vm.filter.absences) {
                if (!vm.eventType.some(e => e == EventType.ABSENCE)) {
                    vm.eventType.push(EventType.ABSENCE);
                }
            } else {
                vm.eventType = _.without(vm.eventType, EventType.ABSENCE);
            }
            vm.updateFilter();
            EventsUtils.resetEventId(vm.eventId);
        };

        vm.switchLateFilter = function () {
            vm.filter.late = !vm.filter.late;
            if (vm.filter.late) {
                if (!vm.eventType.some(e => e == EventType.LATENESS)) {
                    vm.eventType.push(EventType.LATENESS);
                }
            } else {
                vm.eventType = _.without(vm.eventType, EventType.LATENESS);
            }
            vm.updateFilter();
            EventsUtils.resetEventId(vm.eventId);
        };

        vm.switchDepartureFilter = function () {
            vm.filter.departure = !vm.filter.departure;
            if (vm.filter.departure) {
                if (!vm.eventType.some(e => e == EventType.DEPARTURE)) {
                    vm.eventType.push(EventType.DEPARTURE);
                }
            } else {
                vm.eventType = _.without(vm.eventType, EventType.DEPARTURE);
            }
            vm.updateFilter();
            EventsUtils.resetEventId(vm.eventId);
        };

        vm.switchRegularizedFilter = async function () {
            vm.filter.regularized = !vm.filter.regularized;
            vm.events.regularized = vm.filter.regularized;
            vm.events.page = 0;
            EventsUtils.resetEventId(vm.eventId);
        };

        vm.hideGlobalCheckbox = function (event) {
            const {events} = event;
            const isProving = (evt) => evt.reason_id === null || vm.provingReasonsMap[evt.reason_id];

            return events.every(isProving);
        };

        /* on switch (watch) */
        $scope.$watch(() => window.structure, () => {
            getEvents();
        });

        /* Destroy directive and scope */
        $scope.$on("$destroy", () => {
            /* Remove directive/ghost div that remains on the view before changing route */
            angular.element(document.querySelectorAll(".datepicker")).remove();
            angular.element(document.querySelectorAll(".tooltip")).remove();
        });
    }]);