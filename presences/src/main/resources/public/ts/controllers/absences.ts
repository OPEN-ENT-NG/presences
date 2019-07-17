import {_, moment, idiom as lang, ng} from 'entcore';
import {Events, Event, EventType, Student, Students} from "../models";
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
    unjustified: boolean;
    late: boolean;
    regularized: boolean;
}

interface ViewModel {
    filter: Filter;
    notifications: any[];

    // Get reasons type
    eventReasonsType: ReasonType[];
    eventReasonsTypeDescription: ReasonType[];

    // Events
    eventType: number[];
    events: Events;
    multipleSelect: ReasonType;
    eventTypeState(periods, event, $index): string;
    editPeriod($event): void;
    setAbsent(periods, event, $event): void;
    reasonSelect($event): void;
    getSelectValue(event): number;
    changeReason(event): void;
    downloadFile($event): void;
    doAction($event): void;
    stopAbsencePropagation($event): void;
    toggleAbsenceRegularised(event: Event): void;

    // Events description
    hasPeriod(period): boolean;
    isDayHistoryEmpty(event): boolean;
    getPeriodStartDate(periods, event): string;
    getPeriodEndDate(periods, event): string;
    getPeriodData(periods, event): any;
    changeDescriptionReason(periods, event): void;

    // Collapse event
    eventId: number;
    collapse: boolean;
    toggleCollapse(event): void;
    isCollapsibleOpen(id): boolean;

    // Students
    studentSearchInput: string;
    students: Students;
    searchByStudent(string): void;
    selectStudent(model: Student, option: Student): void;
    excludeStudentFromFilter(audience): void;

    // Classes
    classesSearchInput: string;
    classes: any;
    classesFiltered: any[];
    searchByClass(value: string): Promise<void>;
    selectClass(model: any, option: any): void;
    excludeClassFromFilter(audience): void;

    // update filter
    updateFilter(student?, audience?): void;
    updateDate(): void;

    // switch event type
    switchAbsencesFilter(): void;
    switchUnjustifiedFilter(): Promise<void>;
    switchLateFilter(): void;
    switchAbsenceRegularizedFilter(): Promise<void>;

    // Export
    exportPdf(): void;
    exportCsv(): void;
}

export const absencesController = ng.controller('AbsencesController', ['$scope', '$route',
    'GroupService', 'EventService',
    function ($scope, $route, GroupService: GroupService, EventService: EventService) {
        console.log('AbsencesController');
        const vm: ViewModel = this;
        vm.notifications = [];
        vm.filter = {
            startDate: DateUtils.add(new Date(), -30, "d"),
            endDate: moment().endOf('day').toDate(),
            students: [],
            classes: [],
            absences: false,
            unjustified: false,
            late: false,
            regularized: false,
        };
        vm.eventType = [];
        vm.multipleSelect = {id: 0, label: lang.translate("presences.absence.select.multiple"), structureId: ""} as ReasonType;
        vm.studentSearchInput = '';
        vm.classesSearchInput = '';
        vm.students = new Students();
        vm.classesFiltered = undefined;

        vm.events = new Events();
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

            if (!vm.eventReasonsType) {
                vm.eventReasonsType = await EventService.getReasonsType(window.structure.id);
                vm.eventReasonsTypeDescription = _.clone(vm.eventReasonsType);
                vm.eventReasonsType.push(vm.multipleSelect);
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

        vm.setAbsent = (period, event, $event): void => {
            $event.stopPropagation();
        };

        /* Change CSS class depending on their event_type id */
        vm.eventTypeState = (periods, event, $index): string => {
            if (periods.events.length !== 0) {
                let state;
                switch (event.event_type.id) {
                    case 1:
                        event.student.day_history[$index].events.forEach(period => {
                            if (period.reason_id !== null && period.type_id == event.event_type.id) {
                                state = "justified"
                            } else {
                                state = "absent";
                            }
                        });
                        break;
                    case 2:
                        state = "late";
                        break;
                    default:
                        state = "";
                        break;
                }
                return state
            }
            return "";
        };

        vm.reasonSelect = ($event): void => {
            $event.stopPropagation();
        };

        /* store all reason id in an array and check if they are all
        the same to display multiple select or unique */
        vm.getSelectValue = (event): number => {
            let reasonIds = [];
            event.student.day_history.forEach(periods => {
                periods.events.forEach(period => {
                    if ("reason_id" in period && period.type_id == event.event_type.id) {
                        reasonIds.push(period.reason_id);
                    }
                })
            });

            // Check if reason array have different reason id to set multiple select
            if (!reasonIds.every((val, i, arr) => val === arr[0])) {
                // all events have different reason id
                event.reason_id = 0;
            }
            return event.reason_id;
        };

        /* Add global reason_id to all events that exist */
        vm.changeReason = (event): void => {
            /* Fetch all event id */
            let eventsArrayId = [event.id];
            event.student.day_history.forEach(periods => {
                periods.events.forEach(period => {
                    if ("reason_id" in period && period.type_id == event.event_type.id) {
                        period.reason_id = event.reason_id;
                        eventsArrayId.push(period.id);
                    }
                })
            });
            eventsArrayId = eventsArrayId.filter((item, index) => eventsArrayId.indexOf(item) === index);
            vm.events.updateReason(eventsArrayId, event.reason_id);

            // Delete multiple select since we add all events the same reason_id
            vm.eventReasonsType = _.without(vm.eventReasonsType, vm.eventReasonsType.find(item => item.id === 0));
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

        vm.toggleAbsenceRegularised = (event: Event): void => {
            console.log("do toggle Event: ", event);
            event.update();
        };

        // toggle Collapse
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

        // open the concerned event
        vm.isCollapsibleOpen = (event): boolean => {
            return event.id == vm.eventId
        };

        // Description content
        vm.hasPeriod = (period): boolean => {
            return period.events.length !== 0;
        };

        vm.isDayHistoryEmpty = (event): boolean => {
            if (event) {
                let eventsMatchRegisterId = [];
                // Looping through 9 events in day_history to check if events length is empty
                event.student.day_history.forEach(e => {
                    eventsMatchRegisterId.push(e.events.length !== 0);
                });
                return eventsMatchRegisterId.indexOf(true) !== -1;
            } else {
                return false;
            }
        };

        vm.getPeriodStartDate = (periods, event): string => {
            return moment(periods.events.find(period => period.type_id === event.event_type.id)["start_date"]).format("HH:mm");
        };

        vm.getPeriodEndDate = (periods, event): string => {
            return moment(periods.events.find(period => period.type_id === event.event_type.id)["end_date"]).format("HH:mm");
        };

        vm.getPeriodData = (periods, event): any => {
            return periods.events.find(period => period.type_id === event.event_type.id);
        };

        /* Change its description reason id */
        vm.changeDescriptionReason = (period, event): void => {
            let eventsArrayId = [period.id];
            vm.events.updateReason(eventsArrayId, period.reason_id);

            // check all reason id in the main event
            let reasonIds = [];
            event.student.day_history.forEach(periods => {
                periods.events.forEach(period => {
                    if ("reason_id" in period && period.type_id == event.event_type.id) {
                        reasonIds.push(period.reason_id);
                    }
                })
            });

            // update main event select with reasonIds
            if (reasonIds.filter((item, index) => reasonIds.indexOf(item) === index).length > 1) {
                // all events have differents reason id
                event.reason_id = 0;
                // add value multiple select in the main event if no exist
                if (vm.eventReasonsType.every(item => item.id !== 0)) {
                    vm.eventReasonsType.push(vm.multipleSelect);
                }
            } else {
                // all events have same reason id
                event.reason_id = parseInt(reasonIds.filter((item, index) => reasonIds.indexOf(item) === index).toString());
                if (isNaN(event.reason_id)) {
                    event.reason_id = null;
                }
                vm.eventReasonsType = _.without(vm.eventReasonsType, vm.eventReasonsType.find(item => item.id === 0));
            }

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
                if (!vm.eventType.some(x => x == EventType.ABSENCE)) {
                    vm.eventType.push(EventType.ABSENCE);
                }
            } else {
                vm.eventType = _.without(vm.eventType, EventType.ABSENCE);
            }
            vm.updateFilter();
        };

        vm.switchUnjustifiedFilter = async function () {
            vm.filter.unjustified = !vm.filter.unjustified;
            vm.events.unjustified = vm.filter.unjustified;
            await vm.events.syncPagination();
        };

        vm.switchLateFilter = function () {
            vm.filter.late = !vm.filter.late;
            if (vm.filter.late) {
                if (!vm.eventType.some(x => x == EventType.LATENESS)) {
                    vm.eventType.push(EventType.LATENESS);
                }
            } else {
                vm.eventType = _.without(vm.eventType, EventType.LATENESS);
            }
            vm.updateFilter();
        };

        vm.switchAbsenceRegularizedFilter = async function () {
            vm.filter.regularized = !vm.filter.regularized;
            vm.events.regularized = vm.filter.regularized;
            await vm.events.syncPagination();
        };


        /* on switch (watch) */
        $scope.$watch(() => window.structure, () => {
            getEvents();
        });
}]);