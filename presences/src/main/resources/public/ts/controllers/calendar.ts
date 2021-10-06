import {idiom as lang, Me, model, moment, ng} from 'entcore';
import {
    CalendarService,
    Course,
    CourseEvent,
    ForgottenNotebookService,
    GroupService,
    IViescolaireService,
    Notebook,
    NotebookRequest,
    ReasonService,
    SearchItem,
    SearchService,
    Setting,
} from '../services';
import {Scope} from './main';
import {Absence, EventType, ICalendarItems, ITimeSlot, Presence, Presences, Reason, User} from '../models';
import {Punishments} from '@incidents/models';
import {DateUtils, PreferencesUtils} from '@common/utils';
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS, SNIPLET_FORM_EVENTS} from '@common/model';
import {NOTEBOOK_FORM_EVENTS} from '../sniplets';
import {CalendarAbsenceUtils, CalendarUtils, EventsUtils} from '../utilities';
import {ABSENCE_FORM_EVENTS, LATENESS_FORM_EVENTS} from '@common/core/enum/presences-event';
import {IAngularEvent} from 'angular';

declare let window: any;

interface ViewModel {
    show: {
        loader: boolean,
        exemption: { start_date: string, end_date: string },
        presences: { displayName: string, startDate: string, endDate: string },
        event: { reason_id: number }
    };
    courses: {
        list: Array<Course>
        templateInitialized: Boolean
    };
    slots: { list: Array<ITimeSlot> };
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

    reasonsMap: Map<number, Reason>;

    absences: {
        list: Array<Absence>
    };
    punishments: Punishments;

    selectItem(model: any, student: any): void;

    searchItem(value: string): void;

    isAbsenceOnly(item: Course): boolean;

    isAbsenceJustifiedOnly(item: Course): boolean;

    hasFollowedAbsence(item: Course): boolean;

    reloadCalendarByStudent(): void;

    loadForgottenNotebook(): Promise<void>;

    formatExemptionDate(date: any): string;

    getStudentPicture(structure: string, studentId: string): string;

    getEventType(event: CourseEvent): string;

    getEventTypeDate(event: CourseEvent): string;

    formatPresenceDate(date: string): string;

    hasEventAbsenceWithReason(course: Course): boolean;

    hasEventAbsenceRegularized(course: Course): boolean;

    containsAbsence(course: Course): boolean;

    containsReasonAbsence(course: Course): boolean;

    canDisplayPresence(course: Course): boolean;

    canDisplayReasonInEvent(course: Course): boolean;

    actionEventForm(item: Course): void;

    editForgottenNotebook($item): void;

    absenceEvents(course: Course): CourseEvent[];

}

interface CalendarScope extends Scope {
    hoverExemption($event, course: any): void;

    hoverOutExemption(): void;

    hoverPresence($event, course: Course, presences: Array<Presence>): void;

    hoverOutPresence(): void;

    hoverEvents($event, course: Course): void;

    hoverOutEvents(): void;
}

export const calendarController = ng.controller('CalendarController',
    ['$scope', '$timeout', '$route', '$location', 'CalendarService',
        'GroupService', 'SearchService', 'ForgottenNotebookService', 'ReasonService',
        'ViescolaireService',
        function ($scope: CalendarScope, $timeout, $route, $location,
                  CalendarService: CalendarService,
                  GroupService: GroupService,
                  SearchService: SearchService,
                  ForgottenNotebookService: ForgottenNotebookService,
                  reasonService: ReasonService,
                  viescolaireService: IViescolaireService
        ) {
            const vm: ViewModel = this;
            vm.show = {
                loader: true,
                exemption: null,
                presences: null,
                event: {
                    reason_id: null
                }
            };
            vm.filter = {
                search: {
                    item: '',
                    items: null
                },
                student: null,
                students: null
            };
            vm.courses = {list: null, templateInitialized: false};
            vm.absences = {list: null};
            vm.presences = new Presences(window.structure.id);
            vm.reasonsMap = new Map();


            vm.slots = {list: []};

            if ('date' in window.item) {
                const date = moment(window.item.date);
                model.calendar.setDate(date);
            } else {
                model.calendar.setDate(moment());
            }
            const hover = document.getElementById('exemption-hover');

            const loadCalendarItems = async (student: User, startWeekDate: string, structureId: string): Promise<ICalendarItems> => {
                return Promise.all([
                    CalendarService.loadCourses(student, startWeekDate, structureId),
                    CalendarService.loadPresences(student, startWeekDate, structureId),
                    CalendarService.loadAbsences(student, startWeekDate, structureId),
                    reasonService.getReasons(structureId)]
                ).then((values: [Array<Course>, Presences, Array<Absence>, Array<Reason>]) => {
                    return {
                        courses: values[0],
                        presences: values[1],
                        absences: values[2],
                        reasons: values[3]
                    }
                });
            };

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
                vm.reloadCalendarByStudent();
                $scope.safeApply();
            }

            vm.reloadCalendarByStudent = async (student: User = vm.filter.student): Promise<void> => {
                if (vm.filter.student.id !== student.id) {
                    vm.show.loader = true;
                    vm.filter.student = student;
                    window.item = student;
                }

                let values: ICalendarItems = await loadCalendarItems(vm.filter.student, model.calendar.firstDay, window.structure.id);

                vm.courses.list = values.courses;
                await vm.loadForgottenNotebook();

                vm.presences = values.presences;

                values.reasons.forEach((reason: Reason) => vm.reasonsMap.set(reason.id, reason));

                let absenceEvents = vm.courses.list.map((course: Course) => vm.absenceEvents(course));
                let concatAbsenceEvents = [].concat(...absenceEvents);
                vm.absences.list = [...values.absences, ...concatAbsenceEvents];
                $scope.safeApply();
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

            vm.isAbsenceOnly = (item: Course): boolean => {
                return item.absence && item.absenceReason === 0;
            };

            vm.isAbsenceJustifiedOnly = (item: Course): boolean => {
                return item.absence && item.absenceReason > 0;
            };

            vm.hasFollowedAbsence = (item: Course): boolean => {
                return (item.events.findIndex((event: CourseEvent) => event.followed === true) !== -1) ||
                    (item.absences.findIndex((absence: Absence) => absence.followed) !== -1);
            };

            vm.actionEventForm = (item: Course): void => {
                if (item.absences.length > 0) { // ABSENCE
                    $scope.$broadcast(item.absences[0].type_id ? ABSENCE_FORM_EVENTS.EDIT_EVENT : ABSENCE_FORM_EVENTS.EDIT,
                        formatEventForm(item, EventType.ABSENCE));
                } else if (item.events.length > 0 && item.events[0].type_id) { // OTHER EVENTS (LATENESS, REMARK...)
                    switch (item.events[0].type_id) {
                        case EventType.LATENESS:
                            $scope.$broadcast(LATENESS_FORM_EVENTS.EDIT, formatEventForm(item, EventType.LATENESS));
                            break;
                    }
                } else {
                    $scope.$broadcast(ABSENCE_FORM_EVENTS.OPEN, formatEventForm(item, EventType.ABSENCE));
                }
            };

            vm.formatExemptionDate = function (date) {
                return DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
            };

            vm.getStudentPicture = (structure: string, studentId: string): string => {
                return '/viescolaire/structures/' + structure + '/students/' + studentId + '/picture';
            };

            vm.formatPresenceDate = function (date: string): string {
                return DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);
            };

            /**
             * Determine in course if they have at least one event/absence with reason
             *
             * @param course Course object
             */
            vm.hasEventAbsenceWithReason = (course: Course): boolean => {
                if (course.events.length === 0 && course.absences.length === 0) return false;
                return !!course.events
                        .find((event: CourseEvent) => event.type_id === EventType.ABSENCE && event.reason_id !== null) ||
                    !!course.absences
                        .find((absence: Absence) => absence.type_id === EventType.ABSENCE && absence.reason_id != null);
            };

            /**
             * Determine in course if they have at least one event/absence with counsellor_regularisation
             *
             * @param course Course object
             */
            vm.hasEventAbsenceRegularized = (course: Course): boolean => {
                if (course.events.length === 0 && course.absences.length === 0) return false;
                return !!course.events
                        .find((event: CourseEvent) => event.type_id === EventType.ABSENCE && event.counsellor_regularisation === true) ||
                    !!course.absences
                        .find((absence: Absence) => absence.type_id === EventType.ABSENCE && absence.counsellor_regularisation === true);
            };

            vm.containsAbsence = (course: Course): boolean => {
                let contains = false;
                course.events.map((event) => contains = contains ||
                    (event.type_id === EventType.ABSENCE && (event.reason_id === null || event.reason_id === 0)));
                course.absences.map((event) => contains = contains ||
                    (event.reason_id === null || event.reason_id === 0));
                return contains;
            };

            vm.containsReasonAbsence = (course: Course): boolean => {
                let contains = false;
                course.events.map((event) => contains = contains ||
                    (event.type_id === EventType.ABSENCE && (event.reason_id !== null || event.reason_id > 0)));
                course.absences.map((event) => contains = contains ||
                    (event.reason_id !== null || event.reason_id > 0));
                return contains;
            };

            const findEventAbsenceJustified = (course: Course): CourseEvent | Absence => {
                let foundJustified: CourseEvent | Absence;
                if (course.events.length !== 0) {
                    foundJustified = course.events.find(event => event.type_id === EventType.ABSENCE && event.reason_id !== null)
                } else if (course.absences.length !== 0) {
                    foundJustified = course.absences.find(absence => absence.type_id === EventType.ABSENCE && absence.reason_id !== null)
                }
                return foundJustified;
            };

            vm.getEventType = (event: CourseEvent): string => {
                switch (event.type_id) {
                    case EventType.LATENESS:
                        return lang.translate('presences.register.event_type.lateness');
                    case EventType.DEPARTURE:
                        return lang.translate('presences.register.event_type.departure');
                    case EventType.REMARK:
                        return lang.translate('presences.register.event_type.remark');
                }
            };

            vm.getEventTypeDate = (event: CourseEvent): string => {
                switch (event.type_id) {
                    case EventType.LATENESS:
                        return DateUtils.format(event.end_date, DateUtils.FORMAT['HOUR-MINUTES']);
                    case EventType.DEPARTURE:
                    case EventType.REMARK:
                        return DateUtils.format(event.start_date, DateUtils.FORMAT['HOUR-MINUTES']);
                }
            };

            vm.canDisplayPresence = (course: Course): boolean => {
                let hasPresence = vm.presences.all.some(presence =>
                    DateUtils.isBetween(presence.startDate, presence.endDate, course.startDate, course.endDate,
                        DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]
                    )
                );
                return (hasPresence && (course.absences.length > 0 || course.events.length > 0));
            };

            const formatEventForm = (itemCourse: Course, type_id: number): any => {

                switch (type_id) {
                    case EventType.ABSENCE:
                    case EventType.LATENESS:
                        let startDate: Date = moment(itemCourse.startDate).toDate();
                        let endDate: Date = moment(itemCourse.endDate).toDate();
                        let counsellor_regularisation: boolean;
                        let followed: boolean;

                        if (itemCourse.events.length === 1) {
                            startDate = moment(itemCourse.events[0].start_date).toDate();
                            endDate = moment(itemCourse.events[0].end_date).toDate();
                            counsellor_regularisation = itemCourse.events[0].counsellor_regularisation;
                            followed = itemCourse.events[0].followed;
                        }

                        if (itemCourse.absences.length === 1) {
                            startDate = moment(itemCourse.absences[0].start_date).toDate();
                            endDate = moment(itemCourse.absences[0].end_date).toDate();
                            counsellor_regularisation = itemCourse.absences[0].counsellor_regularisation;
                            followed = itemCourse.absences[0].followed;
                        }

                        return {
                            id: itemCourse.events.length > 0 ? itemCourse.events[0].id : null,
                            startDate: startDate,
                            endDate: endDate,
                            startTime: startDate,
                            endTime: endDate,
                            comment: itemCourse.events.length > 0 ? itemCourse.events[0].comment : null,
                            studentId: window.item.id,
                            eventType: ('subjectId' in itemCourse) ? EventsUtils.ALL_EVENTS.event : EventsUtils.ALL_EVENTS.absence,
                            absences: itemCourse.absences,
                            counsellor_regularisation: counsellor_regularisation,
                            followed: followed
                        };
                }
            };

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

            $scope.hoverExemption = ($event, course: any) => {
                const {width, height}: any = getComputedStyle(hover);

                let exemption: { end_date: string, start_date: string } = {end_date: null, start_date: null};

                if (!course.exemption) {
                    exemption.start_date = course.startDate;
                    exemption.end_date = course.endDate;
                } else {
                    exemption = course.exemption;
                }

                const {x, y}: any = $event.target.closest('.exemption-label').getBoundingClientRect();
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

            $scope.hoverEvents = ($event, course: Course) => {
                if (!vm.hasEventAbsenceWithReason(course)) return;
                const hover = document.getElementById('event-absence-hover');
                const {minWidth, minHeight} = getComputedStyle(hover);
                let {x, y} = $event.target.closest('.course-item-container').getBoundingClientRect();
                hover.style.top = `${y - parseInt(minHeight)}px`;
                hover.style.left = `${x - (parseInt(minWidth) / 5)}px`;
                hover.style.display = 'flex';
                vm.show.event.reason_id = findEventAbsenceJustified(course).reason_id;
                $scope.safeApply();
            };

            $scope.hoverOutEvents = () => {
                const hover = document.getElementById('event-absence-hover');
                hover.style.display = 'none';
            };

            model.calendar.on('date-change', initCalendar);

            $scope.$on('$destroy', () => {
                const key: string = PreferencesUtils.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_CALENDAR_FILTER
                if (Me.preferences[key] && $route.current.action != 'events') {
                    PreferencesUtils.resetPreference(key);
                }
                model.calendar.callbacks['date-change'] = []
            });

            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.FILTER, initCalendar);
            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.EDIT, initCalendar);
            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.DELETE, initCalendar);
            $scope.$on(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.CREATION, initCalendar);
            $scope.$on(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.EDIT, initCalendar);
            $scope.$on(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.DELETE, initCalendar);

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

            // on opening event "event" form
            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.CREATION, () => {
                $scope.$broadcast(SNIPLET_FORM_EVENTS.SET_PARAMS, {
                    student: window.item,
                    start_date: moment(),
                    end_date: moment()
                });
            });

            // watching our vm.courses from initCalendar methods
            $scope.$watch(() => vm.absences.list, () => {
                // positioning absence with all courses (absence including), is set from initCalendar
                vm.show.loader = true;
                if (vm.absences.list != null && vm.courses.list != null) $timeout(positioningAbsence);
                else vm.show.loader = false;
            });


            $scope.$watch(() => window.structure, async (newVal, oldVal) => {
                // when changing structure after having already loaded courses and absences => redirect to dashboard
                if (vm.absences.list && vm.courses.list) {
                    $scope.redirectTo(`/dashboard`);
                }
                const structure_slots = await viescolaireService.getSlotProfile(window.structure.id);
                if (oldVal !== newVal)
                    await PreferencesUtils.resetPreference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_CALENDAR_FILTER);
                if (Object.keys(structure_slots).length > 0) vm.slots.list = structure_slots.slots;
                else vm.slots.list = null;
                model.calendar.setTimeslots(vm.slots.list);
                $scope.safeApply();
            });

            $scope.$watch(() => window.entcore.calendar.startOfDay, async (newVal, oldVal) => {
                if (newVal !== oldVal) {
                    // positioning absence with all courses (absence including), will set show loader true before proceeding
                    vm.show.loader = true;
                    $timeout(positioningAbsence);
                }
            });

            $scope.$on('$includeContentLoaded', function (event: IAngularEvent, target) {
                if (target.includes("course-item") && !vm.courses.templateInitialized) {
                    vm.courses.templateInitialized = true;
                    if (vm.absences.list != null && vm.courses.list != null) $timeout(positioningAbsence);
                }
            });
        }]);