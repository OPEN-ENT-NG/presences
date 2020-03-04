import {model, moment, ng} from 'entcore';
import {
    CalendarService,
    Course,
    CourseEvent,
    ForgottenNotebookService,
    GroupService,
    Notebook,
    NotebookRequest,
    PresenceService,
    SearchItem,
    SearchService,
    Setting,
    StructureService,
    TimeSlot
} from '../services';
import {Scope} from './main';
import {EventType, Presence, PresenceRequest, Presences, User} from '../models';
import {DateUtils} from '@common/utils';
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";
import {NOTEBOOK_FORM_EVENTS} from "../sniplets";
import {CalendarAbsenceUtils, CalendarUtils, EventsUtils} from "../utilities";
import {ABSENCE_FORM_EVENTS} from "@common/enum/presences-event";

declare let window: any;

interface ViewModel {
    show: {
        loader: boolean, exemption: { start_date: string, end_date: string },
        presences: { displayName: string, startDate: string, endDate: string }
    };
    courses: {
        list: Array<Course>,
        hasLoaded: boolean
    };
    slots: { list: Array<TimeSlot> };
    filter: {
        search: {
            item: string,
            items: Array<SearchItem>
        },
        student: User,
        students: Array<User>
    };
    settings: Setting,
    student: {
        alerts: any
    };
    presences: Presences;

    selectItem(model: any, student: any): void;

    searchItem(value: string): void;

    changeAbsence(item: Course): string;

    isAbsenceOnly(item): boolean;

    isAbsenceJustifiedOnly(item): boolean;

    isGlobalAbsence(item): boolean;

    isGlobalAbsenceReason(item): boolean;

    loadCourses(): Promise<void>;

    loadPresences(): Promise<void>;

    loadForgottenNotebook(): Promise<void>;

    formatExemptionDate(date: any): string;

    formatPresenceDate(date: string): string;

    canDisplayPresence(course: Course): boolean;

    actionAbsenceForm(item: Course, items): void;

    editForgottenNotebook($item): void;

    eventContainsAbsence(event: CourseEvent, item): boolean;

}

interface CalendarScope extends Scope {
    hoverExemption($event, exemption: { start_date: string, end_date: string }): void;

    hoverOutExemption(): void;

    hoverPresence($event, course: Course, presences: Array<Presence>): void;

    hoverOutPresence(): void;
}

export const calendarController = ng.controller('CalendarController',
    ['$scope', 'route', '$location', 'StructureService', 'CalendarService',
        'GroupService', 'SearchService', 'ForgottenNotebookService', 'PresenceService',
        function ($scope: CalendarScope, route, $location, StructureService: StructureService,
                  CalendarService: CalendarService,
                  GroupService: GroupService,
                  SearchService: SearchService,
                  ForgottenNotebookService: ForgottenNotebookService,
                  presenceService: PresenceService) {
            const vm: ViewModel = this;
            vm.show = {
                loader: true,
                exemption: null,
                presences: null
            };
            vm.filter = {
                search: {
                    item: '',
                    items: null
                },
                student: null,
                students: null
            };
            vm.courses = {list: [], hasLoaded: false};
            vm.presences = new Presences(window.structure.id);

            vm.slots = {list: []};

            if ('date' in window.item) {
                const date = moment(window.item.date);
                model.calendar.setDate(date);
            } else {
                model.calendar.setDate(moment());
            }
            const hover = document.getElementById('exemption-hover');

            async function initCalendar() {
                vm.show.loader = true;
                $scope.safeApply();
                const {item, structure} = window;
                if (item === null || structure === null) {
                    $location.path('/');
                    return;
                }

                vm.filter.student = item;
                vm.filter.students = await CalendarService.getStudentsGroup(item.groupId);
                if (item.type === 'GROUP' && vm.filter.students.length > 0) {
                    vm.filter.student = vm.filter.students[0];
                    window.item = vm.filter.student;
                    $location.path(`/calendar/${vm.filter.student.id}`);
                }
                await Promise.all([
                    vm.loadCourses(),
                    vm.loadPresences()
                ]).then(async () => {
                    initActionAbsence();
                });
            }


            vm.changeAbsence = function (item: Course): string {
                vm.courses.hasLoaded = true;
                if ('hash' in item) CalendarUtils.changeAbsenceView(item);
                return "";
            };

            vm.loadForgottenNotebook = async function () {
                let diff = 7;
                if (!model.calendar.display.saturday) diff--;
                if (!model.calendar.display.synday) diff--;

                const notebookRequest = {} as NotebookRequest;
                notebookRequest.startDate = moment(model.calendar.firstDay).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                notebookRequest.endDate = moment(DateUtils.add(model.calendar.firstDay, diff)).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                notebookRequest.studentId = window.item.id;
                const notebooks = await ForgottenNotebookService.get(notebookRequest);
                let legends = document.querySelectorAll('legend:not(.timeslots)');
                CalendarUtils.renderLegends(legends, notebooks);
                onClickLegend(legends, notebooks);
            };

            vm.loadCourses = async function (student: User = vm.filter.student) {
                vm.show.loader = true;
                vm.courses.hasLoaded = false;
                if (vm.filter.student.id !== student.id) {
                    vm.filter.student = student;
                    window.item = student;
                }
                const {structure} = window;
                const start = DateUtils.format(model.calendar.firstDay, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                const end = DateUtils.format(DateUtils.add(model.calendar.firstDay, 1, 'w'), DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.courses.list = await CalendarService.getCourses(structure.id, student.id, start, end);
                await vm.loadForgottenNotebook();
                vm.show.loader = false;
                $scope.safeApply();
            };

            vm.loadPresences = async function () {
                let presencesRequest: PresenceRequest = {
                    structureId: window.structure.id,
                    startDate: DateUtils.format(model.calendar.firstDay, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                    endDate: DateUtils.format(DateUtils.add(model.calendar.firstDay, 1, 'w'), DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                    studentIds: [vm.filter.student.id]
                } as PresenceRequest;
                await vm.presences.build(await presenceService.get(presencesRequest));
                $scope.safeApply();
            };

            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.CREATION, () => {
                $scope.$broadcast(SNIPLET_FORM_EVENTS.SET_PARAMS, {
                    student: window.item,
                    start_date: moment(),
                    end_date: moment()
                });
            });

            vm.selectItem = function (model, item) {
                const needsToLoadGroup = (window.item.groupId !== item.groupId) || item.type === 'GROUP';
                window.item = item;
                vm.filter.search.items = undefined;
                vm.filter.search.item = '';
                if (needsToLoadGroup) {
                    initCalendar();
                } else {
                    vm.filter.student = item;
                    vm.loadCourses();
                }
            };

            vm.searchItem = async function (value: string) {
                const structureId = window.structure.id;
                try {
                    vm.filter.search.items = await SearchService.search(structureId, value);
                } catch (err) {
                    vm.filter.search.items = [];
                } finally {
                    $scope.safeApply();
                }
            };

            vm.isAbsenceOnly = function (item): boolean {
                return item.absence && item.absenceReason === 0;
            };

            vm.isAbsenceJustifiedOnly = function (item): boolean {
                return item.absence && item.absenceReason > 0;
            };

            vm.isGlobalAbsence = function (item): boolean {
                return item._id === '0' && item.absence && item.absenceReason === 0;
            };

            vm.isGlobalAbsenceReason = function (item): boolean {
                return item._id === '0' && item.absence && item.absenceReason > 0;
            };

            vm.eventContainsAbsence = function (event: CourseEvent, item: Course): boolean {
                if (event.type_id === EventType.ABSENCE) {
                    CalendarUtils.renderAbsenceFromCourse(item, event, vm.slots.list);
                    CalendarUtils.addEventIdInSplitCourse(item, item.events);
                    CalendarUtils.positionAbsence(event, item, vm.slots.list);
                    return true;
                }
                return false;
            };

            vm.actionAbsenceForm = function (item: Course, items): void {
                if (item._id === "0") {
                    $scope.$broadcast(ABSENCE_FORM_EVENTS.EDIT, item);
                } else {
                    let absenceItem = items.find(isMatchOrBetweenDate(item));
                    if (absenceItem === undefined) {
                        if ('subjectId' in item && ('events' in item && item.events.length > 0)) {
                            $scope.$broadcast(ABSENCE_FORM_EVENTS.EDIT_EVENT, formatAbsenceForm(item));
                        } else {
                            $scope.$broadcast(ABSENCE_FORM_EVENTS.OPEN, formatAbsenceForm(item));
                        }
                    } else {
                        $scope.$broadcast(ABSENCE_FORM_EVENTS.EDIT, absenceItem);
                    }
                }
            };

            vm.formatExemptionDate = function (date) {
                return DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
            };

            vm.formatPresenceDate = function (date: string) {
                return DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);
            };

            vm.canDisplayPresence = (course: Course): boolean => {
                let hasPresence = vm.presences.all.some(presence =>
                    DateUtils.isBetween(presence.startDate, presence.endDate, course.startDate, course.endDate,
                        DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]
                    )
                );
                return (hasPresence && (course.absence || course.events.length > 0));
            };

            function isMatchOrBetweenDate(item): (absence: Course) => boolean {
                /* callback find method */
                return function (absence: Course) {
                    return absence._id === '0' && ((absence.startDate === item.startDate && absence.endDate === item.endDate) ||
                        DateUtils.isBetween(absence.startDate, absence.endDate, item.startDate, item.endDate));
                }
            }

            function formatAbsenceForm(itemCourse) {
                return {
                    startDate: moment(itemCourse.startDate).toDate(),
                    endDate: moment(itemCourse.endDate).toDate(),
                    startTime: moment(itemCourse.startDate).toDate(),
                    endTime: moment(itemCourse.endDate).toDate(),
                    studentId: window.item,
                    eventType: ('subjectId' in itemCourse) ? EventsUtils.ALL_EVENTS.event : EventsUtils.ALL_EVENTS.absence,
                    id: ('subjectId' in itemCourse && ('events' in itemCourse && itemCourse.events.length > 0))
                        ? 1 : undefined
                };
            }

            function onClickLegend(legends: NodeList, notebooks: Notebook[]) {
                Array.from(legends).forEach((legend: HTMLElement) => {
                    legend.addEventListener('click', () => {
                        let notebook = notebooks.find(item => item.id === parseInt(legend.getAttribute("forgotten-id")));
                        if (notebook === undefined) return;
                        $scope.$broadcast(NOTEBOOK_FORM_EVENTS.EDIT, {student: window.item, notebook: notebook});
                        $scope.safeApply();
                    });
                });
            }

            function initActionAbsence() {
                CalendarAbsenceUtils.actionAbsenceTimeSlot($scope);
                CalendarAbsenceUtils.actionDragAbsence($scope);
                $scope.safeApply();
            }

            $scope.hoverExemption = function ($event, exemption: { end_date: string, start_date: string }) {
                const {width, height} = getComputedStyle(hover);
                let {x, y} = $event.target.closest('.exemption-label').getBoundingClientRect();
                hover.style.top = `${y - parseInt(height)}px`;
                hover.style.left = `${x - (parseInt(width) / 4)}px`;
                hover.style.display = 'flex';
                vm.show.exemption = exemption;
                $scope.safeApply();
            };

            $scope.hoverOutExemption = function () {
                hover.style.display = 'none';
            };

            $scope.hoverPresence = ($event, course: Course, presences: Array<Presence>) => {
                const hover = document.getElementById('presences-hover');
                const {width, height} = getComputedStyle(hover);
                let {x, y} = $event.target.closest('.exemption-label').getBoundingClientRect();
                hover.style.top = `${y - parseInt(height)}px`;
                hover.style.left = `${x - (parseInt(width) / 4)}px`;
                hover.style.display = 'flex';

                let index = presences.findIndex(presence =>
                    DateUtils.isBetween(presence.startDate, presence.endDate, course.startDate, course.endDate,
                        DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]
                    )
                );
                if (index !== -1) {
                    let presence: Presence = presences[index];
                    vm.show.presences = {
                        displayName: presence.owner.displayName,
                        startDate: presence.startDate,
                        endDate: presence.endDate
                    };
                }
                $scope.safeApply();
            };

            $scope.hoverOutPresence = () => {
                const hover = document.getElementById('presences-hover');
                hover.style.display = 'none';
            };

            model.calendar.on('date-change', initCalendar);

            $scope.$on('$destroy', () => model.calendar.callbacks['date-change'] = []);

            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.FILTER, initCalendar);
            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.EDIT, initCalendar);
            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.DELETE, initCalendar);

            $scope.$watch(() => vm.courses.hasLoaded, () => {
                if (vm.courses.hasLoaded) {
                    initActionAbsence();
                }
            });

            $scope.$watch(() => window.structure, async () => {
                const structure_slots = await StructureService.getSlotProfile(window.structure.id);
                if (Object.keys(structure_slots).length > 0) vm.slots.list = structure_slots.slots;
                else vm.slots.list = null;
                $scope.safeApply();
            });
        }]);