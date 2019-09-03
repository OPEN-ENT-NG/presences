import {_, angular, idiom as lang, moment, ng} from 'entcore';
import {Event, EventResponse, Events, EventType, Student, Students} from "../models";
import {DateUtils} from "@common/utils";
import {GroupService} from "@common/services/GroupService";
import {EventService, ReasonType} from "../services";

declare let window: any;

interface Filter {
    startDate: Date;
    endDate: Date;
    students: any;
    classes: any;
    absences: boolean;
    late: boolean;
    departure: boolean;
    regularized: boolean;
}

interface ViewModel {
    filter: Filter;

    /* Get reasons type */
    eventReasonsType: ReasonType[];
    eventReasonsTypeDescription: ReasonType[];

    /* Events */
    eventType: number[];
    events: Events;
    multipleSelect: ReasonType;

    eventTypeState(periods, event): string;

    isDayHistoryEmpty(periods, event): boolean;

    editPeriod($event): void;

    /* dayHistory interaction */
    mouseIsDown: boolean;
    indexDayHistory: number;
    indexDayHistoryArray: number[];

    period($event): void;

    setAbsent(): void;

    treatAbsentAfterMouseLeave(event: EventResponse): Promise<void>;

    drawAbsent($event): void;

    interactAbsent(period, event: EventResponse, $event, parentIndex): void;

    /* setAbsent(periods, event, $event, $index): void;
*/
    reasonSelect($event): void;

    getSelectValue(event): number;

    getRegularizedValue(event): boolean;

    filterSelect(options: ReasonType[], event): ReasonType[];

    changeReason(event): void;

    downloadFile($event): void;

    doAction($event): void;

    stopAbsencePropagation($event): void;

    regularizedChecked(event: EventResponse): boolean;

    toggleAllAbsenceRegularised(event: EventResponse): void;

    toggleAbsenceRegularised(history, event): void;

    /* Events description */
    changeDescriptionReason(periods, event): void;

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

export const absencesController = ng.controller('AbsencesController', ['$scope', '$route',
    'GroupService', 'EventService',
    function ($scope, $route, GroupService: GroupService, EventService: EventService) {
        console.log('AbsencesController');
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
            regularized: false,
        };
        vm.mouseIsDown = false;
        vm.eventType = [];
        vm.multipleSelect = {
            id: 0,
            label: lang.translate("presences.absence.select.multiple"),
            structure_id: "",
            comment: "",
            default: false,
            proving: false,
            group: false
        } as ReasonType;
        vm.studentSearchInput = '';
        vm.classesSearchInput = '';
        vm.students = new Students();
        vm.classesFiltered = undefined;

        vm.events = new Events();
        vm.events.regularized = isWidget ? vm.filter.regularized : null;
        vm.events.eventer.on('loading::true', () => $scope.safeApply());
        vm.events.eventer.on('loading::false', () => $scope.safeApply());

        /* ----------------------------
          Events
        ---------------------------- */
        const setStudentToSync = () => {
            vm.events.userId = vm.filter.students ? vm.filter.students
                .map(students => students.id)
                .filter(function () {
                    return true
                })
                .toString() : '';
        };

        const setClassToSync = () => {
            vm.events.classes = vm.filter.classes ? vm.filter.classes
                .map(classes => classes.id)
                .filter(function () {
                    return true
                })
                .toString() : '';
        };

        const getEvents = async (): Promise<void> => {
            vm.events.structureId = window.structure.id;
            vm.events.startDate = vm.filter.startDate.toDateString();
            vm.events.endDate = vm.filter.endDate.toDateString();

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
                vm.eventReasonsType = await EventService.getReasonsType(window.structure.id);
                vm.eventReasonsTypeDescription = _.clone(vm.eventReasonsType);
                if (!isWidget) vm.eventReasonsType.push(vm.multipleSelect);
            }

            setStudentToSync();
            setClassToSync();
            // "page" uses sync() method at the same time it sets 0 (See LoadingCollection)
            vm.events.page = 0;
            $scope.safeApply();
        };

        vm.editPeriod = ($event): void => {
            $event.stopPropagation();
        };

        vm.period = ($event): void => {
            $event.stopPropagation();
        };

        vm.setAbsent = (): void => {
            vm.mouseIsDown = true;
            vm.indexDayHistoryArray = [];
        };

        vm.treatAbsentAfterMouseLeave = async (event: EventResponse) => {
            vm.mouseIsDown = false;
            /* @TODO !!feature not available yet!! While sliding on node, create absence
                  that fit on register_id date and time to create event */
            return;
        };

        const initGlobalCounsellorRegularisation = function (event: EventResponse): boolean {
            let regularizedArray = [];
            event.events.forEach(e => {
                regularizedArray.push(e.counsellor_regularisation);
            });
            return regularizedArray.reduce((current, initial) => initial && current)
        };

        const initGlobalReason = function (event: EventResponse) {
            let reasonIds = getReasonIds(event.events);
            if (!reasonIds.every((val, i, arr) => val === arr[0])) {
                event.globalReason = 0;
            } else {
                event.globalReason = parseInt(_.uniq(reasonIds));
                if (isNaN(event.globalReason)) {
                    event.globalReason = null;
                }
            }
        };

        const getReasonIds = function (events): number[] {
            let reasonIds = [];
            events.forEach(item => {
                reasonIds.push(item.reason_id);
            });
            return reasonIds;
        };

        vm.drawAbsent = ($event): void => {
            /* @TODO !!feature not available yet!! When clicking on node, create absence that
                 should match with register_id date/time to create event */
            return;
        };

        vm.interactAbsent = (period, event: EventResponse, $event, parentIndex): void => {
            $event.stopPropagation();
            if (vm.indexDayHistoryArray.length === 0) {
                let eventClass = $event.currentTarget.getAttribute("class");

                if (eventClass === "late" && period.name === "Repas" && eventClass === "departure") {
                    return;
                }

                // delete event
                if (eventClass === "justified" || eventClass === "absent") {
                    let eventIdToDelete;
                    period.events.forEach((event, index) => {
                        if (event.type_id === 1) {
                            eventIdToDelete = event.id;
                            period.events.splice(index, 1);
                        }
                    });
                    let eventToDelete = event.events.find(element => element.id === eventIdToDelete);
                    let eventObj = new Event(eventToDelete.register_id, event.studentId, eventToDelete.start_date, eventToDelete.end_date);
                    eventObj.id = eventToDelete.id;
                    eventObj.delete();
                    event.dayHistory.forEach(history => {
                        history.events.forEach((event, index) => {
                            if (event.id === eventIdToDelete) {
                                history.events.splice(index, 1);
                            }
                        })
                    });
                    event.events = event.events.filter(item => item.id !== eventIdToDelete);
                    if (event.events.length === 0) {
                        vm.events.all.splice(parentIndex, 1);
                        return;
                    }
                    initGlobalReason(event);
                    event.globalCounsellorRegularisation = initGlobalCounsellorRegularisation(event);
                } else {
                    /* @TODO !!feature not available yet!! When click on node,
                         add absent/justified class to fill node and create absence */
                    return;
                }
            }
        };

        /* Change CSS class depending on their event_type id */
        vm.eventTypeState = (periods, event): string => {
            if (periods.events.length === 0) return '';
            const priority = [EventType.ABSENCE, EventType.LATENESS, EventType.DEPARTURE, EventType.REMARK];
            const className = ['absent', 'late', 'departure', 'remark', 'justified','empty'];
            let index = 4;
            for (let i = 0; i < periods.events.length; i++) {
                if (periods.events[i].type_id === 1) {
                    index = periods.events[i].reason_id !== null ? 4 : 0;
                } else if (periods.events[i].type_id === 2) {
                    index = 1;
                } else {
                    let arrayIndex = priority.indexOf(periods.events[i].type_id);
                    index = arrayIndex < index ? arrayIndex : index;
                }
            }
            return className[index] || '';
        };

        vm.reasonSelect = ($event): void => {
            $event.stopPropagation();
        };

        /* filtering by removing multiple choices if there is no reason_id */
        vm.filterSelect = function (options: ReasonType[], event): ReasonType[] {
            let reasonIds = getReasonIds(event.events);
            if (reasonIds.every((val, i, arr) => val === arr[0])) {
                return options.filter(option => option.id !== 0);
            }
            return options;
        };

        /* Add global reason_id to all events that exist */
        vm.changeReason = (event: EventResponse): void => {
            /* Fetch all event id */
            let eventsArrayId = [];
            if (isWidget) {
                eventsArrayId.push(event.id);
                event.globalReason = event.reason_id;
            } else {
                event.dayHistory.forEach(periods => {
                    periods.events.forEach(period => {
                        if ("reason_id" in period) {
                            period.reason_id = event.globalReason;
                            eventsArrayId.push(period.id);
                        }
                    })
                });
                event.events.forEach(item => {
                    item.reason_id = event.globalReason;
                });
            }
            eventsArrayId = eventsArrayId.filter((item, index) => eventsArrayId.indexOf(item) === index);
            vm.events.updateReason(eventsArrayId, event.globalReason);
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
            });
            return !regularized.every((val, i, arr) => val === arr[0]) && !event.globalCounsellorRegularisation;
        };

        vm.toggleAllAbsenceRegularised = (event: EventResponse): void => {
            let eventsId = [];
            event.dayHistory.forEach(history => {
                history.events.forEach(e => {
                    e.counsellor_regularisation = event.globalCounsellorRegularisation;
                });
            });
            event.events.forEach(item => {
                item.counsellor_regularisation = event.globalCounsellorRegularisation;
                eventsId.push(item.id);
            });
            vm.events.updateRegularized(eventsId, event.globalCounsellorRegularisation);
        };

        vm.toggleAbsenceRegularised = (history, event): void => {
            let eventsId = [history.id];
            vm.events.updateRegularized(eventsId, history.counsellor_regularisation);
            if (!isWidget) event.globalCounsellorRegularisation = initGlobalCounsellorRegularisation(event);
            else vm.events.page = 0;
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
        vm.changeDescriptionReason = (history, event: EventResponse): void => {
            let eventsArrayId = [history.id];
            event.dayHistory.forEach(e => {
                e.events.forEach(item => {
                    if (item.id === history.id) {
                        item.reason_id = history.reason_id;

                        // check if id already exist in array
                        if (eventsArrayId.indexOf(item.id) === -1) {
                            eventsArrayId.push(item.id);
                        }
                    }
                });
            });
            vm.events.updateReason(eventsArrayId, history.reason_id);
            initGlobalReason(event);
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
            setStudentToSync();
            setClassToSync();
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
        };

        vm.switchDepartureFilter = function () {
            vm.filter.departure = !vm.filter.departure;
            if (vm.filter.departure) {
                if (!vm.eventType.some(e => e == EventType.DEPARTURE)) {
                    vm.eventType.push(EventType.LATENESS);
                }
            } else {
                vm.eventType = _.without(vm.eventType, EventType.DEPARTURE);
            }
            vm.updateFilter();
        };

        vm.switchRegularizedFilter = async function () {
            vm.filter.regularized = !vm.filter.regularized;
            vm.events.regularized = vm.filter.regularized;
            vm.events.page = 0;
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