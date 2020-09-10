import {_, idiom as lang, Me, model, moment, ng, notify, template, toasts} from 'entcore';
import {
    Absence,
    Course,
    Courses,
    Departure,
    Event,
    Events,
    EventType,
    Lateness,
    Presences,
    Register,
    RegisterStatus,
    Remark
} from '../models';
import {GroupService, IRegisterRequest, ReasonService, SearchService} from '../services';
import {CourseUtils, DateUtils, PreferencesUtils, PresencesPreferenceUtils} from '@common/utils'
import rights from '../rights';
import {Scope} from './main';
import http from 'axios';
import {EventsUtils, RegisterUtils, StudentsSearch} from "../utilities";
import {Reason} from "@presences/models/Reason";
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";
import {INFINITE_SCROLL_EVENTER} from "@common/core/enum/infinite-scroll-eventer";
import {IRegisterService} from "@presences/services/RegisterService";

declare let window: any;

interface Filter {
    date: Date;
    start_date: Date;
    end_date: Date;
    offset?: number;
    limit?: number;
    student: any;
    teacher: string;
    teachers: any[];
    class: string;
    classes: any[];
    course: Course;
    selected: { teachers: any[], classes: any[], registerTeacher: any };
    forgotten: boolean;
    multipleSlot: boolean;
}

export interface ViewModel {
    widget: { forgottenRegisters: boolean };
    register: Register;
    courses: Courses;
    filter: Filter;
    RegisterStatus: any;
    studentsSearch: StudentsSearch;
    presences: Presences;
    reasons: Reason[];

    /* search bar auto complete */
    searchStudents(value): Promise<void>;

    selectStudent(valueInput, studentItem): Promise<void>;

    // removeStudent(value): void;

    openRegister(course: Course, $event): Promise<void>;

    tooltipMultipleSlot(): string;

    isCurrentRegister(course: Course): boolean;

    isCurrentCourse(course: Course): boolean;

    nextDate(): void;

    previousDate(): void;

    formatHour(date: string): string;

    toggleAbsence(student): void;

    toggleLateness(student): Promise<void>;

    toggleDeparture(student): Promise<void>;

    handleRemark(student): Promise<void>;

    getBirthDate(student): string;

    openPanel(student): void;

    updateLateness(): void;

    updateDeparture(): void;

    manageAbsence(events, student): void;

    getHistoryEventClassName(events, slot): string;

    isCurrentSlot(slot: { end: string, start: string }): boolean;

    closePanel(): void;

    loadCourses(users?: string[], groups?: string[], structure?: string, start_date?: string,
                end_date?: string, forgotten_registers?: boolean, multipleSlot?: boolean, limit?: number): Promise<void>;

    isFuturCourse(course: Course): boolean;

    searchTeacher(value: string): void;

    selectTeacher(model: any, teacher: any): void;

    selectClass(model: any, classObject: any): void;

    searchClass(value: string): Promise<void>;

    dropFilter(object, list): void;

    isLoading(): boolean;

    isEmptyGroupRegister(): boolean

    isEmptyDayHistory(student): boolean;

    isAbsenceDisabled(student): boolean;

    switchForgottenFilter(): void;

    switchMultipleSlot(): Promise<void>;

    formatDayDate(timestamp: number): string;

    formatHourTooltip(date: string): string;

    findEvent(events: Array<Event>): Event;

    changeDate(): void;

    changeFiltersDate(): void;

    switchRegisterTeacher(teacher): void;

    notify(): Promise<void>;

    canNotify(start_date: string, state: RegisterStatus): boolean;

    getGroups(classes: string[], groups: string[]): string[];

    export(): void;

    validRegister(): Promise<void>;

    onScroll(): Promise<void>;
}

export const registersController = ng.controller('RegistersController',
    ['$scope', '$timeout', '$route', '$location', '$rootScope', 'SearchService', 'GroupService', 'ReasonService', 'RegisterService',
        async function ($scope: Scope, $timeout, $route, $location, $rootScope,
                        SearchService: SearchService, GroupService: GroupService, ReasonService: ReasonService,
                        registerService: IRegisterService) {
            const vm: ViewModel = this;
            vm.widget = {
                forgottenRegisters: false,
            };
            const actions = {
                registers: () => {
                    template.open('registers', 'register/list');
                    if (model.me.hasWorkflow(rights.workflow.search)) {
                        vm.courses.clear();
                        delete vm.register;
                        vm.filter = {
                            ...vm.filter,
                            teacher: '',
                            class: '',
                            teachers: undefined,
                            classes: undefined,
                            selected: {
                                teachers: vm.filter.selected.teachers,
                                classes: vm.filter.selected.classes,
                                registerTeacher: undefined
                            }
                        };
                    }
                    vm.filter.offset = 0;
                    // access list of registers
                    vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName(),
                        undefined, undefined, undefined, undefined, undefined, vm.filter.limit);
                },
                getRegister: async ({id}) => {
                    template.open('registers', 'register/register');
                    if ('filter' in window && window.filter) {
                        vm.filter = {...vm.filter, ...window.filter};
                    }
                    vm.filter.course = RegisterUtils.initCourseToFilter();
                    template.open('register', 'register/list-view');
                    template.open('register-panel', 'register/panel');
                    getReasons();
                    if (vm.register !== undefined) {
                        await vm.register.sync();
                        await initCourses();
                        if (vm.register.teachers.length > 0) vm.filter.selected.registerTeacher = vm.register.teachers[0];
                        $scope.safeApply();
                    } else {
                        vm.register = new Register();
                        vm.register.id = id;
                        vm.register.eventer.once('loading::true', () => $scope.safeApply());
                        vm.register.eventer.once('loading::false', () => $scope.safeApply());
                        await vm.register.sync();
                        await initCourses();
                    }
                },
                // 16 correspond to the limit number needed in dashboard
                forgottenRegisterWidget: () => vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName(), undefined, undefined, undefined, undefined, undefined, 16),
            };

            const initCourses = async () => {
                if (!vm.register.teachers) return;
                vm.register.teachers.forEach(teacher =>
                    vm.filter.course.teachers.push({id: teacher.id, displayName: teacher.displayName})
                );
                if (vm.filter.selected.registerTeacher) {
                    if (vm.register.teachers.length > 0 && _.countBy(vm.register.teachers, (teacher) => teacher.id === vm.filter.selected.registerTeacher.id) === 0)
                        vm.filter.selected.registerTeacher = vm.register.teachers[0];
                }
                const teachers = vm.filter.course.teachers.length > 0 ? [vm.filter.course.teachers[0].id] : [];
                await vm.loadCourses(teachers, [], window.structure.id, DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                    DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), false);
                $scope.safeApply();
            };

            vm.onScroll = async (): Promise<void> => {
                vm.filter.offset++;
                registerService
                    .get(getRequestForListCourse())
                    .then((courses: Course[]) => {
                        RegisterUtils.appendCoursesMap(courses, vm.courses);

                        // Won't stop scrolling until we done loading courses
                        if (courses.length !== 0) {
                            $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                        }
                        $scope.safeApply();
                    });
            };

            const getRequestForListCourse = (): IRegisterRequest => {
                return {
                    start: DateUtils.format(vm.filter.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                    end: DateUtils.format(vm.filter.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                    structure: window.structure.id,
                    teachers: extractSelectedTeacherIds(),
                    groups: extractSelectedGroupsName(),
                    forgottenRegister: vm.filter.forgotten,
                    multipleSlot: vm.filter.multipleSlot,
                    offset: vm.filter.offset,
                    limit: vm.filter.limit
                };
            };

            // Get absences reasons as personal user info
            const getReasons = async (): Promise<void> => {
                if (model.me.profiles.some(profile => profile === "Personnel")) {
                    vm.reasons = await ReasonService.getReasons(window.structure.id);
                }
            };

            vm.register = undefined;
            vm.courses = new Courses();
            vm.courses.eventer.on('loading::true', () => $scope.safeApply());
            vm.courses.eventer.on('loading::false', () => $scope.safeApply());
            vm.RegisterStatus = RegisterStatus;

            let registerTimeSlot = await Me.preference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_REGISTER);

            vm.filter = {
                date: new Date(),
                start_date: new Date(),
                end_date: new Date(),
                offset: 0,
                limit: 100,
                student: undefined,
                teacher: "",
                teachers: undefined,
                class: "",
                classes: undefined,
                forgotten: true,
                course: undefined,
                selected: {
                    teachers: [],
                    classes: [],
                    registerTeacher: undefined
                },
                multipleSlot: registerTimeSlot.multipleSlot,
            };

            vm.changeFiltersDate = async function () {
                vm.filter.offset = 0;
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName(),
                    undefined, undefined, undefined, undefined, undefined, vm.filter.limit);
            };

            vm.export = function () {
                vm.courses.export(extractSelectedTeacherIds(), extractSelectedGroupsName(), window.structure.id,
                    DateUtils.format(vm.filter.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), DateUtils.format(vm.filter.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), vm.filter.forgotten)
            };

            const changeDate = async function (step: number) {
                vm.filter.date = DateUtils.add(vm.filter.date, step);
                delete vm.register;
                vm.filter.offset = 0;
                await vm.loadCourses(extractSelectedTeacherIds(), [], window.structure.id, DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                    DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), false);
                setCurrentRegister();
            };

            vm.nextDate = () => changeDate(1);
            vm.previousDate = () => changeDate(-1);
            vm.changeDate = () => changeDate(0);

            vm.searchTeacher = async function (value) {
                const structureId = window.structure.id;
                try {
                    vm.filter.teachers = await SearchService.searchUser(structureId, value, 'Teacher');
                    $scope.safeApply();
                } catch (err) {
                    vm.filter.teachers = [];
                    throw err;
                }
            };

            vm.searchClass = async function (value) {
                const structureId = window.structure.id;
                try {
                    vm.filter.classes = await GroupService.search(structureId, value);
                    vm.filter.classes.map((obj) => obj.toString = () => obj.name);
                    $scope.safeApply();
                } catch (err) {
                    vm.filter.classes = [];
                    throw err;
                }
                return;
            };

            vm.selectClass = function (model, classObject) {
                if (_.findWhere(vm.filter.selected.teachers, {id: classObject.id})) {
                    return;
                }
                vm.filter.selected.classes.push(classObject);
                vm.filter.class = '';
                vm.filter.classes = undefined;
                vm.filter.offset = 0;
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName(),
                    undefined, undefined, undefined, undefined, undefined, vm.filter.limit);
                $scope.safeApply();
            };

            const extractSelectedTeacherIds = function () {
                const ids = [];
                if (model.me.hasWorkflow(rights.workflow.search)) {
                    if ($route.current.action === 'getRegister') {
                        ids.push(vm.filter.selected.registerTeacher.id || vm.register.teachers[0].id);
                    } else {
                        vm.filter.selected.teachers.map((teacher) => ids.push(teacher.id));
                    }
                } else {
                    ids.push(model.me.userId);
                }
                return ids;
            };

            const extractSelectedGroupsName = function (): string[] {
                const names = [];
                if (model.me.hasWorkflow(rights.workflow.search)) {
                    vm.filter.selected.classes.map((group) => names.push(group.name));
                }
                return names;
            };

            vm.searchStudents = async (value): Promise<void> => {
                await vm.studentsSearch.searchStudentsFromArray(value, vm.register.students);
                $scope.safeApply();
            };

            vm.selectStudent = async (valueInput, studentItem): Promise<void> => {
                vm.studentsSearch.selectStudent(valueInput, studentItem);
                vm.studentsSearch.student = "";
                vm.studentsSearch.resetStudents();
            };

            vm.selectTeacher = function (model, teacher) {
                if (_.findWhere(vm.filter.selected.teachers, {id: teacher.id})) {
                    return;
                }
                vm.filter.selected.teachers.push(teacher);
                vm.filter.teacher = '';
                vm.filter.teachers = undefined;
                vm.filter.offset = 0;
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName(),
                    undefined, undefined, undefined, undefined, undefined, vm.filter.limit);
                $scope.safeApply();
            };

            vm.dropFilter = function (object, list) {
                vm.filter.selected[list] = _.without(vm.filter.selected[list], object);
                delete vm.register;
                vm.filter.offset = 0;
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName(),
                    undefined, undefined, undefined, undefined, undefined, vm.filter.limit);
                $scope.safeApply();
            };

            vm.formatHour = (date: string) => DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);

            const setCurrentRegister = async function (): Promise<void> {
                let i: number = 0;
                let course: Course;
                while (!course && i < vm.courses.all.length) {
                    if (CourseUtils.isCurrentCourse(vm.courses.all[i])) course = vm.courses.all[i];
                    i++;
                }
                return vm.openRegister(course || vm.courses.all[0], null);
            };

            vm.openRegister = async function (course: Course, $event) {
                if ($event && ($event.target as Element).className.includes('notify-bell')) {
                    notifyCourse(course);
                    return;
                }
                if (vm.isFuturCourse(course)) return;
                vm.register = RegisterUtils.createRegisterFromCourse(course);
                if (!course.registerId) {
                    try {
                        await vm.register.create();
                        course.registerId = vm.register.id;
                    } catch (err) {
                        notify.error('presences.register.creation.err');
                        vm.register.loading = false;
                        $scope.safeApply();
                        throw err;
                    }
                }
                vm.filter.date = moment(course.startDate).toDate();
                vm.filter.course = course;
                window.filter = vm.filter;
                if ($route.current.action !== 'getRegister') {
                    $scope.redirectTo(`/registers/${vm.register.id}`);
                    $scope.safeApply();
                } else {
                    if (vm.register.id) window.location.hash = window.location.hash.replace($route.current.params.id, vm.register.id);
                    vm.register.eventer.on('loading::true', () => $scope.safeApply());
                    vm.register.eventer.on('loading::false', () => $scope.safeApply());
                    await vm.register.sync();
                    if (vm.register.teachers.length > 0 && _.countBy(vm.register.teachers, (teacher) => teacher.id === vm.filter.selected.registerTeacher.id) === 0)
                        vm.filter.selected.registerTeacher = vm.register.teachers[0];
                }
            };

            vm.tooltipMultipleSlot = function (): string {
                let tooltipText;
                tooltipText = vm.filter.multipleSlot ? 'presences.widgets.day.set.multiple.slot.toolip.disable'
                    : 'presences.widgets.day.set.multiple.slot.toolip.activate';
                return lang.translate(tooltipText);
            };

            vm.isCurrentRegister = function (course: Course): boolean {
                if (course && vm.register) {
                    const courseStartDate = moment(course.startDate)
                        .format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                    const courseEndDate = moment(course.endDate)
                        .format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                    const registerStartDate = moment(vm.register.start_date)
                        .format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                    const registerEndDate = moment(vm.register.end_date)
                        .format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                    const matchDate: boolean = courseStartDate === registerStartDate && courseEndDate === registerEndDate;
                    return course.id === vm.register.course_id && matchDate;
                }
                return false;
            };

            vm.isCurrentCourse = function (course: Course): boolean {
                return CourseUtils.isCurrentCourse(course);
            };

            const removeEventFromSlot = function (student, event) {
                for (let i = 0; i < student.day_history.length; i++) {
                    const slot = student.day_history[i];
                    if (vm.isCurrentSlot(slot)) {
                        slot.events = _.filter(slot.events, (evt) => evt.id !== event.id);
                    }
                }
            };

            const addEventToSlot = function (student, event) {
                for (let i = 0; i < student.day_history.length; i++) {
                    const slot = student.day_history[i];
                    if (vm.isCurrentSlot(slot)) {
                        if (_.findWhere(slot.events, {id: event.id}) === undefined) {
                            slot.events.push(event);
                        }
                    }
                }
            };

            const toggleEvent = async function (student, event, start_date, end_date) {
                if (student[event]) {
                    try {
                        await student[event].delete();
                    } catch (err) {
                        notify.error('presences.event.deletion.err');
                        throw err;
                    }
                    removeEventFromSlot(student, student[event]);
                    delete student[event];
                    vm.register.students.map(registerStudent => {
                        if (registerStudent.id === student.id) {
                            delete registerStudent[event];
                            registerStudent.day_history = student.day_history;
                        }
                    });
                    if (event === 'absence') vm.register.absenceCounter--;
                } else {
                    let o;
                    switch (event) {
                        case 'lateness': {
                            o = new Lateness(vm.register.id, student.id, start_date, end_date);
                            break;
                        }
                        case 'departure': {
                            o = new Departure(vm.register.id, student.id, start_date, end_date);
                            break;
                        }
                        default: {
                            o = new Absence(vm.register.id, student.id, start_date, end_date);
                        }
                    }
                    student[event] = o;
                    try {
                        await student[event].create();
                    } catch (err) {
                        notify.error('presences.event.creation.err');
                        throw err;
                    }
                    addEventToSlot(student, student[event]);
                    vm.register.students.map(registerStudent => {
                        if (registerStudent.id === student.id) {
                            registerStudent[event] = student[event];
                            registerStudent.day_history = student.day_history;
                        }
                    });
                    if (event === 'absence') vm.register.absenceCounter++;
                }
                vm.register.setStatus(RegisterStatus.IN_PROGRESS);
                if (student.absence && event === 'lateness') {
                    await toggleEvent(student, 'absence', start_date, end_date);
                }
                $scope.safeApply();
            };

            vm.toggleAbsence = async (student) => {
                if (vm.isAbsenceDisabled(student)) {
                    return;
                }
                // if ((student.absence && student.absence.counsellor_input) || (student.exempted && !student.exemption_attendance)) return;
                await toggleEvent(student, 'absence', vm.register.start_date, vm.register.end_date);
                student.departure = undefined;
                student.lateness = undefined;
                $scope.safeApply();
            };

            vm.toggleLateness = async (student) => {
                const endDateTime = moment(moment(vm.register.start_date).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]) + ' ' +
                    moment().millisecond(0).second(0).format(DateUtils.FORMAT["HOUR-MINUTES"]));
                await toggleEvent(student, 'lateness', vm.register.start_date, endDateTime.format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]));
                if (student.lateness) student.lateness.end_date_time = endDateTime.toDate();
                $scope.safeApply();
            };

            vm.toggleDeparture = async (student) => {
                const startDateTime = moment(moment(vm.register.start_date).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]) + ' ' +
                    moment().millisecond(0).second(0).format(DateUtils.FORMAT["HOUR-MINUTES"]));
                await toggleEvent(student, 'departure',
                    startDateTime.format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                    vm.register.end_date);
                student.departure.start_date_time = startDateTime.toDate();
                $scope.safeApply();
            };

            vm.handleRemark = async (student) => {
                if (student.remark.comment.trim() !== '') {
                    await student.remark.save();
                    addEventToSlot(student, student.remark);
                } else {
                    await student.remark.delete();
                    removeEventFromSlot(student, student.remark);
                    student.remark = new Remark(vm.register.id, student.id, vm.register.start_date, vm.register.end_date);
                }
                $scope.safeApply();
            };

            vm.getBirthDate = function (student) {
                return DateUtils.format(student.birth_date, DateUtils.FORMAT.BIRTHDATE);
            };

            vm.openPanel = function (student) {
                vm.filter.student = student;
                $scope.safeApply();
            };

            vm.updateLateness = function () {
                vm.filter.student.lateness.update();

                const startRegister = vm.register.start_date;
                const endRegister = vm.register.end_date;

                // update day_history events
                vm.filter.student.day_history.forEach(item => {
                    if (!(item.start >= startRegister && item.end <= endRegister)) return;
                    const endDate = moment(vm.filter.student.lateness.end_date_time).format("YYYY-MM-DDTHH:mm:ss");
                    item.events.forEach((e, index) => {
                        if (e.id === vm.filter.student.lateness.id) {
                            if (!((item.end <= endDate) || (endDate > item.start))) {
                                item.events.splice(index, 1);
                            }
                        }
                    });
                    if ((item.end <= endDate) || (endDate > item.start)) {
                        item.events.push(vm.filter.student.lateness);
                    }
                });
            };

            vm.updateDeparture = function () {
                vm.filter.student.departure.update();

                const startRegister = vm.register.start_date;
                const endRegister = vm.register.end_date;

                // update day_history events
                vm.filter.student.day_history.forEach(item => {
                    if (!(item.start >= startRegister && item.end <= endRegister)) return;
                    const endDate = moment(vm.filter.student.departure.end_date_time).format("YYYY-MM-DDTHH:mm:ss");
                    item.events.forEach((e, index) => {
                        if (e.id === vm.filter.student.departure.id) {
                            if (!((item.end <= endDate) || (endDate > item.start))) {
                                item.events.splice(index, 1);
                            }
                        }
                    });
                    if ((item.end <= endDate) || (endDate > item.start)) {
                        item.events.push(vm.filter.student.departure);
                    }
                });
            };

            vm.manageAbsence = async function (events, student) {
                if (events.id) {
                    if (typeof events.register_id === 'string') {
                        events.register_id = parseInt(events.register_id);
                    }
                    new Events().updateReason([events], events.reason_id, student.id, window.structure.id);
                } else {
                    let reason_id = events.reason_id;
                    student.absence = undefined;
                    await vm.toggleAbsence(student);
                    if (typeof student.absence.register_id === 'string') {
                        student.absence.register_id = parseInt(student.absence.register_id);
                    }
                    new Events().updateReason([student.absence], reason_id, student.id, window.structure.id);
                    student.absence.reason_id = reason_id;
                    $scope.safeApply();
                }
            };

            vm.closePanel = function () {
                delete vm.filter.student;
            };

            vm.getHistoryEventClassName = function (events, slot) {
                if (events.length === 0) return '';
                const priority = [EventType.ABSENCE, EventType.LATENESS, EventType.DEPARTURE, EventType.REMARK];
                const className = ['absence', 'lateness', 'departure', 'remark'];
                let index = 4;
                for (let i = 0; i < events.length; i++) {
                    let arrayIndex = priority.indexOf(events[i].type_id);
                    index = arrayIndex < index ? arrayIndex : index;
                }

                return className[index] || '';
            };

            vm.isCurrentSlot = function (slot: { start: string, end: string }) {
                // return Math.abs(moment(slot.start).diff(vm.register.start_date)) < 3000 && Math.abs(moment(slot.end).diff(vm.register.end_date)) < 3000;
                return (DateUtils.isBetween(vm.register.start_date, vm.register.end_date, slot.start, slot.end));

            };

            vm.loadCourses = async function (users: string[] = [model.me.userId], groups: string[] = [], structure: string = window.structure.id,
                                             start_date: string = DateUtils.format(vm.filter.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                                             end_date: string = DateUtils.format(vm.filter.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                                             forgotten_registers: boolean = vm.filter.forgotten,
                                             multipleSlot: boolean = vm.filter.multipleSlot, limit?: number): Promise<void> {
                if (model.me.profiles.some(profile => profile === "Personnel")) {
                    multipleSlot = true;
                }

                if (vm.filter.offset === 0) {
                    vm.courses.clear();
                }
                await vm.courses.sync(users, groups, structure, start_date, end_date, forgotten_registers, multipleSlot, limit, vm.filter.offset);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                $scope.safeApply();
            };

            vm.isEmptyGroupRegister = function () {
                return !vm.register || vm.register.students.length === 0;
            };

            vm.isLoading = function () {
                return (vm.register && vm.register.loading) || vm.courses.loading;
            };

            vm.isFuturCourse = function (course: Course) {
                if (!course) return true;
                return moment().isSameOrBefore(course.startDate);
            };

            vm.isEmptyDayHistory = function (student) {
                let count = 0;
                for (let i = 0; i < student.day_history.length; i++) {
                    const {events} = student.day_history[i];
                    count += (events.length || 0)
                }
                return count === 0;
            };

            vm.isAbsenceDisabled = function (student): boolean {
                return RegisterUtils.isAbsenceDisabled(student, vm.register);
            };

            vm.switchForgottenFilter = function () {
                vm.filter.forgotten = !vm.filter.forgotten;
                vm.filter.offset = 0;
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName(),
                    undefined, undefined, undefined, undefined, undefined, vm.filter.limit);
            };

            vm.switchMultipleSlot = async function (): Promise<void> {
                await PresencesPreferenceUtils.updatePresencesRegisterPreference(vm.filter.multipleSlot);
                vm.filter.offset = 0;
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName(),
                    undefined, undefined, undefined, false)
            };

            vm.switchRegisterTeacher = function (teacher) {
                vm.filter.selected.registerTeacher = teacher;
                vm.filter.offset = 0;
                vm.courses.sync([teacher.id], [], window.structure.id, DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                    DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), false, false);
            };

            vm.formatDayDate = function (timestamp: number): string {
                return DateUtils.format(timestamp, DateUtils.FORMAT["DAY-DATE"]);
            };

            vm.formatHourTooltip = function (date) {
                return DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);
            };

            vm.findEvent = (events: Array<Event>): Event => {
                return events.find(event => event.type === EventsUtils.ALL_EVENTS.event);
            };

            const notifyCourse = async function (course: Course) {
                try {
                    const {data} = await notifyTeachers(course.id, course.startDate, course.endDate);
                    if ('register_id' in data) {
                        course.registerId = data.register_id;
                    }
                    course.notified = true;
                    $scope.safeApply();
                } catch (err) {
                    notify.error('presences.register.notify.err');
                    throw err;
                }
            };

            const notifyTeachers = async function (id, start, end) {
                return await http.post(`/presences/courses/${id}/notify`, {start, end});
            };

            vm.notify = async function () {
                try {
                    const {course_id, start_date, end_date} = vm.register;
                    await notifyTeachers(course_id, start_date, end_date);
                    vm.register.notified = true;
                    $scope.safeApply();
                } catch (err) {
                    notify.error('presences.register.notify.err');
                    throw err;
                }
            };

            const isForgotten = function (start_date): boolean {
                return moment().isAfter(moment(start_date).add(15, 'm'));
            };

            vm.canNotify = function (start_date, state) {
                if (state && state === RegisterStatus.DONE) {
                    return false;
                }

                return model.me.hasWorkflow(rights.workflow.notify)
                    && isForgotten(start_date)
                    && !vm.isFuturCourse(({startDate: start_date} as Course))
                    && moment(DateUtils.setFirstTime(moment())).diff(moment(DateUtils.setFirstTime(start_date)), 'days') < 2
            };

            vm.getGroups = function (classes, groups) {
                return [...classes, ...groups];
            };

            vm.validRegister = async function () {
                try {
                    vm.register.setStatus(vm.RegisterStatus.DONE);
                    toasts.confirm('presences.register.validation.success');
                } catch (err) {
                    toasts.warning('presences.register.validation.error');
                }
            };

            function startAction() {
                switch ($route.current.action) {
                    case 'getRegister':
                    case 'registers': {
                        actions[$route.current.action]($route.current.params);
                        break;
                    }
                    case 'dashboard': {
                        if (vm.widget.forgottenRegisters) {
                            actions.forgottenRegisterWidget();
                        }
                        break;
                    }
                    default:
                        return;
                }
            }

            startAction();

            /* events handler */

            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.FILTER, () => {
                startAction();
                vm.closePanel();
            });
            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.EDIT, startAction);
            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.DELETE, startAction);

            $scope.$watch(() => window.structure, (newVal, oldVal) => {
                if (newVal.id === oldVal.id) return;
                console.warn(`$scope.$watch window.structure: ${newVal.id}, ${oldVal.id}`);
                startAction();
            });

            $scope.$watch(() => $route.current.action, (newVal, oldVal) => {
                if (
                    (newVal === oldVal && !(newVal === 'dashboard' && oldVal === 'dashboard'))
                    || (newVal === 'registers' && oldVal === 'dashboard')
                    || (newVal === 'getRegister' && oldVal === 'dashboard')
                ) return;
                console.warn(`$scope.$watch $route.current.action: ${newVal}, ${oldVal}`);
                startAction();
            });

            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.CREATION, () => {
                $scope.$broadcast(SNIPLET_FORM_EVENTS.SET_PARAMS, {
                    student: vm.filter.student,
                    date: vm.filter.date
                });
            });

            /* Destroy */
            $scope.$on("$destroy", () => {
                if ($route.current.action === "dashboard") {
                    window.filter = {};
                }
            });
        }]);