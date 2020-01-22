import {_, angular, idiom as lang, moment, ng} from 'entcore';
import {Absence, Event, EventResponse, Events, EventType, Student, Students} from "../models";
import {DateUtils} from "@common/utils";
import {GroupService} from "@common/services/GroupService";
import {EventService, Reason, ReasonService} from "../services";
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

    downloadFile($event): void;

    doAction($event): void;

    stopAbsencePropagation($event): void;

    regularizedChecked(event: EventResponse): boolean;

    changeAllReason(event: EventResponse): Promise<void>;

    changeReason(history: Event, event: EventResponse): Promise<void>;

    toggleAllAbsenceRegularised(event: EventResponse): Promise<void>;

    toggleAbsenceRegularised(history: Event, event: EventResponse): Promise<void>;

    getNonRegularizedEvents(events): any[];

    hideGlobalCheckbox(event): boolean;

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
    'GroupService', 'ReasonService', 'EventService',
    function ($scope, $route, $location, GroupService: GroupService, ReasonService: ReasonService, eventService: EventService) {
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
            vm.eventId = null;
            $scope.safeApply();
        });
        const getEvents = async (actionMode?: boolean): Promise<void> => {
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
            // actionMode to define if we display the loading icon mode while changing filter, date etc...
            if (!actionMode) {
                // "page" uses sync() method at the same time it sets 0 (See LoadingCollection Class)
                vm.events.page = 0;
            } else {
                // case if we only interact with action, reason, counsellor regularized...
                refreshGetEventWhileAction();
            }
            $scope.safeApply();
        };

        const filterHistory = (): void => {
            vm.events.all = vm.events.all.filter(e => e.exclude !== true);
            vm.events.all.forEach(event => {
                event.events = EventsUtils.filterHistory(event.events);
                $scope.safeApply();
            });
            vm.events.all = vm.events.all.sort((a, b) =>
                moment(b.date).format(DateUtils.FORMAT["YEARMONTHDAY"]) -
                moment(a.date).format(DateUtils.FORMAT["YEARMONTHDAY"])
            )
        };

        const refreshGetEventWhileAction = async (): Promise<void> => {
            let filter = {
                structureId: vm.events.structureId,
                startDate: vm.events.startDate,
                endDate: vm.events.endDate,
                eventType: vm.events.eventType,
                regularized: vm.events.regularized,
                userId: vm.events.userId,
                classes: vm.events.classes,
                page: vm.events.page
            };
            let events = await eventService.get(filter);

            vm.events.pageCount = events.pageCount;
            vm.events.events = events.events;
            vm.events.all = events.all;
            filterHistory();
            $scope.safeApply();
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

        /* Add global reason_id to all events that exist */
        vm.changeAllReason = async (event: EventResponse): Promise<void> => {
            let initialReasonId = event.globalReason;
            let fetchedEventIds: number[] = [];
            let fetchedAbsenceIds: number[] = [];
            if (isWidget) {
                fetchedEventIds.push(event.id);
            } else {
                EventsUtils.fetchEventsAbsencesId(event, fetchedEventIds, fetchedAbsenceIds);
            }
            vm.events.all.forEach(e => {
                e.events.every(ee => {
                    if (fetchedAbsenceIds.indexOf(ee.id) === -1) {
                        return false;
                    }
                    EventsUtils.fetchEventsAbsencesId(e, fetchedEventIds, fetchedAbsenceIds);
                })
            });
            await Promise.all([
                vm.events.updateReason(fetchedEventIds, initialReasonId),
                new Absence(null, null, null, null)
                    .updateAbsenceReason(fetchedAbsenceIds, initialReasonId)
            ]).then(() => {
                if (isWidget) vm.events.page = 0;
                getEvents(true);
                vm.eventId = null;
            });
            $scope.safeApply();
        };

        /* Change its description reason id */
        vm.changeReason = async (history: Event, event: EventResponse): Promise<void> => {
            let initialReasonId = history.reason_id;
            let fetchedEventIds: number[] = [];
            let fetchedAbsenceIds: number[] = [];
            if (history.type === EventsUtils.ALL_EVENTS.event) {
                fetchedEventIds.push(history.id);
            } else {
                fetchedAbsenceIds.push(history.id);
                if ('events' in history) {
                    history.events.forEach(he => {
                        EventsUtils.addEventsAndAbsencesArray(he, fetchedEventIds, fetchedAbsenceIds);
                    });
                }
            }
            await Promise.all([
                vm.events.updateReason(fetchedEventIds, initialReasonId),
                new Absence(null, null, null, null)
                    .updateAbsenceReason(fetchedAbsenceIds, initialReasonId)
            ]).then(() => {
                getEvents(true);
                if (event.events.filter(e => !e.counsellor_regularisation).length === 0) {
                    vm.eventId = null;
                }
            });
            $scope.safeApply();
        };

        vm.toggleAllAbsenceRegularised = async (event: EventResponse): Promise<void> => {
            let initialCounsellorRegularisation = event.globalCounsellorRegularisation;
            let fetchedEventIds: number[] = [];
            let fetchedAbsenceIds: number[] = [];
            EventsUtils.fetchEventsAbsencesId(event, fetchedEventIds, fetchedAbsenceIds);
            vm.events.all.forEach(e => {
                e.events.every(ee => {
                    if (fetchedAbsenceIds.indexOf(ee.id) === -1) {
                        return false;
                    }
                    EventsUtils.fetchEventsAbsencesId(e, fetchedEventIds, fetchedAbsenceIds);
                })
            });
            await Promise.all([
                vm.events.updateRegularized(fetchedEventIds, initialCounsellorRegularisation),
                new Absence(null, null, null, null)
                    .updateAbsenceRegularized(fetchedAbsenceIds, initialCounsellorRegularisation)
            ]).then(() => {
                getEvents(true);
                vm.eventId = null;
            });
            $scope.safeApply();
        };

        vm.toggleAbsenceRegularised = async (history: Event, event: EventResponse): Promise<void> => {
            let initialCounsellorRegularisation = history.counsellor_regularisation;
            let fetchedEventIds: number[] = [];
            let fetchedAbsenceIds: number[] = [];
            if (history.type === EventsUtils.ALL_EVENTS.event) {
                fetchedEventIds.push(history.id);
            } else {
                fetchedAbsenceIds.push(history.id);
                if ('events' in history) {
                    history.events.forEach(ee => {
                        EventsUtils.addEventsAndAbsencesArray(ee, fetchedEventIds, fetchedAbsenceIds);
                    });
                }
            }
            await Promise.all([
                vm.events.updateRegularized(fetchedEventIds, initialCounsellorRegularisation),
                new Absence(null, null, null, null)
                    .updateAbsenceRegularized(fetchedAbsenceIds, initialCounsellorRegularisation)
            ]).then(() => {
                if (!isWidget) {
                    getEvents(true);
                    if (event.events.filter(e => !e.counsellor_regularisation).length === 0) {
                        vm.eventId = null;
                    }
                } else {
                    vm.events.page = 0;
                }
            });
        };

        vm.getNonRegularizedEvents = (events): any[] => {
            return events.filter(item => item.counsellorRegularisation === false);
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