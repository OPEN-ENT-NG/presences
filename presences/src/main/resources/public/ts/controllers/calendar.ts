import {model, moment, ng} from 'entcore';
import {
    AbsenceService,
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
import {Absence, ICalendarItems, Presence, Presences, User} from '../models';
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
        list: Array<Course>
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

    absences: {
        list: Array<Absence>
    };

    selectItem(model: any, student: any): void;

    searchItem(value: string): void;

    changeAbsence(item: Course): void;

    isAbsenceOnly(item): boolean;

    isAbsenceJustifiedOnly(item): boolean;

    loadForgottenNotebook(): Promise<void>;

    formatExemptionDate(date: any): string;

    formatPresenceDate(date: string): string;

    canDisplayPresence(course: Course): boolean;

    actionAbsenceForm(item: Course, items): void;

    editForgottenNotebook($item): void;

    absenceEvents(course: Course): CourseEvent[];

}

interface CalendarScope extends Scope {
    hoverExemption($event, exemption: { start_date: string, end_date: string }): void;

    hoverOutExemption(): void;

    hoverPresence($event, course: Course, presences: Array<Presence>): void;

    hoverOutPresence(): void;
}

export const calendarController = ng.controller('CalendarController',
    ['$scope', '$timeout', 'route', '$location', 'StructureService', 'CalendarService',
        'GroupService', 'SearchService', 'ForgottenNotebookService', 'AbsenceService', 'PresenceService',
        function ($scope: CalendarScope, $timeout, route, $location, StructureService: StructureService,
                  CalendarService: CalendarService,
                  GroupService: GroupService,
                  SearchService: SearchService,
                  ForgottenNotebookService: ForgottenNotebookService
        ) {
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
            vm.courses = {list: null};
            vm.absences = {list: null};
            vm.presences = new Presences(window.structure.id);

            vm.slots = {list: []};

            if ('date' in window.item) {
                const date = moment(window.item.date);
                model.calendar.setDate(date);
            } else {
                model.calendar.setDate(moment());
            }
            const hover = document.getElementById('exemption-hover');

            async function loadCalendarItems(student: User, startWeekDate: string, structureId: string): Promise<ICalendarItems> {
                return Promise.all([
                    CalendarService.loadCourses(student, startWeekDate, structureId),
                    CalendarService.loadPresences(student, startWeekDate, structureId),
                    CalendarService.loadAbsences(student, startWeekDate, structureId)]
                ).then((values) => {
                    return {
                        courses: values[0],
                        presences: values[1],
                        absences: values[2],
                    }
                });
            }

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

                let values = await loadCalendarItems(vm.filter.student, model.calendar.firstDay, window.structure.id);
                vm.courses.list = values.courses;
                await vm.loadForgottenNotebook();

                vm.presences = values.presences;

                let absenceEvents = vm.courses.list.map((course: Course) => vm.absenceEvents(course));
                let concatAbsenceEvents = [].concat(...absenceEvents);
                vm.absences.list = [...values.absences, ...concatAbsenceEvents];
                $scope.safeApply();
            }

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
                    CalendarService.loadCourses(vm.filter.student, model.calendar.firstDay, window.structure.id);
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

            vm.actionAbsenceForm = function (item: Course, items): void {
                if (item.absences.length > 0) {
                    if (item.absences[0].type_id) {
                        $scope.$broadcast(ABSENCE_FORM_EVENTS.EDIT_EVENT, formatAbsenceForm(item));
                    } else $scope.$broadcast(ABSENCE_FORM_EVENTS.EDIT, formatAbsenceForm(item));
                } else {
                    $scope.$broadcast(ABSENCE_FORM_EVENTS.OPEN, formatAbsenceForm(item));
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

            function formatAbsenceForm(itemCourse) {
                let startDate = moment(itemCourse.startDate).toDate();
                let endDate = moment(itemCourse.endDate).toDate();
                if (itemCourse.absences.length === 1) {
                    startDate = moment(itemCourse.absences[0].start_date).toDate();
                    endDate = moment(itemCourse.absences[0].end_date).toDate();
                }
                return {
                    startDate: startDate,
                    endDate: endDate,
                    startTime: startDate,
                    endTime: endDate,
                    studentId: window.item.id,
                    eventType: ('subjectId' in itemCourse) ? EventsUtils.ALL_EVENTS.event : EventsUtils.ALL_EVENTS.absence,
                    absences: itemCourse.absences
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

            vm.absenceEvents = function (course: Course): CourseEvent[] {
                return CalendarUtils.absenceEvents(course);
            };

            // Callback methods for positioning absences
            const positioningAbsence = () => {
                if (vm.absences.list && vm.courses.list) {
                    const {structure, entcore} = window;
                    CalendarUtils.initPositionAbsence(vm.absences.list, vm.courses.list, structure.id, entcore.calendar.dayHeight, $scope);
                }
                initActionAbsence();
                vm.show.loader = false;
            };

            // watching our vm.courses from initCalendar methods
            $scope.$watch(() => vm.absences.list, () => {
                // positioning absence with all courses (absence including), is set from initCalendar
                vm.show.loader = true;
                if (vm.absences.list != null && vm.courses.list != null) $timeout(positioningAbsence);
                else vm.show.loader = false;
            });


            $scope.$watch(() => window.structure, async () => {
                const structure_slots = await StructureService.getSlotProfile(window.structure.id);
                if (Object.keys(structure_slots).length > 0) vm.slots.list = structure_slots.slots;
                else vm.slots.list = null;
                $scope.safeApply();
            });
        }]);