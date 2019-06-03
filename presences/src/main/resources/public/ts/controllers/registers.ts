import {_, model, moment, ng, notify, template} from 'entcore';
import {Absence, Course, Courses, Departure, EventType, Lateness, Register, RegisterStatus, Remark} from '../models'
import {GroupService, UserService} from '../services';
import {CourseUtils, DateUtils} from '@common/utils'
import rights from '../rights'
import {Scope} from './main'
import http from 'axios'

declare let window: any;

interface Filter {
    date: Date;
    start_date: Date;
    end_date: Date;
    student: any;
    teacher: string;
    teachers: any[];
    class: string;
    classes: any[];
    course: Course;
    selected: { teachers: any[], classes: any[], registerTeacher: any };
    forgotten: boolean;
}

interface ViewModel {
    register: Register;
    courses: Courses;
    filter: Filter;
    RegisterStatus: any;

    openRegister(course: Course, $event: Event): Promise<void>;

    nextDate(): void;

    previousDate(): void;

    formatHour(date: string): string;

    toggleAbsence(student): void;

    toggleLateness(student): Promise<void>;

    toggleDeparture(student): Promise<void>;

    handleRemark(student): Promise<void>;

    selectStudent(student): void;

    getBirthDate(student): string;

    openPanel(student): void;

    getHistoryEventClassName(events): string;

    isCurrentSlot(slot: { end: string, start: string }): boolean;

    closePanel(): void;

    loadCourses(users?: string[], groups?: string[], structure?: string, start_date?: string, end_date?: string, forgotten_registers?: boolean): Promise<void>;

    isFuturCourse(course: Course): boolean;

    searchTeacher(value: string): void;

    selectTeacher(model: any, teacher: any): void;

    selectClass(model: any, classObject: any): void;

    searchClass(value: string): Promise<void>;

    dropFilter(object, list): void;

    isEmptyDayHistory(student): boolean;

    switchForgottenFilter(): void;

    formatDayDate(date: string): string;

    changeDate(): void;

    changeFiltersDate(): void;

    switchRegisterTeacher(teacher): void;

    notify(): Promise<void>;

    canNotify(start_date: string, state: RegisterStatus): boolean;

    getGroups(classes: string[], groups: string[]): string[];

    export(): void;
}

export const registersController = ng.controller('RegistersController',
    ['$scope', '$route', '$rootScope', 'UserService', 'GroupService',
        function ($scope: Scope, $route, $rootScope, UserService: UserService, GroupService: GroupService) {
            const vm: ViewModel = this;
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
                    vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName());
                },
                getRegister: async () => {
                    template.open('registers', 'register/register');
                    if (vm.register !== undefined) {
                        template.open('register', 'register/list-view');
                        template.open('register-panel', 'register/panel');
                        let promises = [vm.register.sync()];
                        if (vm.filter.course.teachers.length > 0) {
                            let cp = vm.loadCourses([vm.filter.course.teachers[0].id], [], window.structure.id, DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                                DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), false);
                            promises.push(cp);
                        }
                        await Promise.all(promises);
                        if (vm.register.teachers.length > 0) vm.filter.selected.registerTeacher = vm.register.teachers[0];
                        $scope.safeApply();
                    } else {
                        $scope.redirectTo('/registers');
                    }
                }
            };

            $scope.$watch(() => $route.current.action, () => {
                actions[$route.current.action]($route.current.params);
            });
            vm.register = undefined;
            vm.courses = new Courses();
            vm.courses.eventer.on('loading::true', () => $scope.safeApply());
            vm.courses.eventer.on('loading::false', () => $scope.safeApply());
            vm.RegisterStatus = RegisterStatus;
            vm.filter = {
                date: new Date(),
                start_date: new Date(),
                end_date: new Date(),
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
                }
            };

            vm.changeFiltersDate = async function () {
                await vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName());
            };

            vm.export = function () {
                vm.courses.export(extractSelectedTeacherIds(), extractSelectedGroupsName(), window.structure.id,
                    DateUtils.format(vm.filter.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), DateUtils.format(vm.filter.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), vm.filter.forgotten)
            };

            const changeDate = async function (step: number) {
                vm.filter.date = DateUtils.add(vm.filter.date, step);
                delete vm.register;
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
                    vm.filter.teachers = await UserService.search(structureId, value, 'Teacher');
                    vm.filter.teachers.map((teacher) => teacher.toString = () => teacher.displayName);
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
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName());
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

            vm.selectTeacher = function (model, teacher) {
                if (_.findWhere(vm.filter.selected.teachers, {id: teacher.id})) {
                    return;
                }
                vm.filter.selected.teachers.push(teacher);
                vm.filter.teacher = '';
                vm.filter.teachers = undefined;
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName());
                $scope.safeApply();
            };

            vm.dropFilter = function (object, list) {
                vm.filter.selected[list] = _.without(vm.filter.selected[list], object);
                delete vm.register;
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName());
                $scope.safeApply();
            };

            vm.formatHour = (date: string) => DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);

            const createRegisterFromCourse = function (course: Course): Register {
                const register = new Register();
                if (course.register_id) {
                    register.id = course.register_id;
                    register.course_id = course._id;
                } else {
                    register.course_id = course._id;
                    register.structure_id = course.structureId;
                    register.start_date = course.startDate;
                    register.end_date = course.endDate;
                    register.subject_id = course.subjectId;
                    register.groups = course.groups;
                    register.classes = course.classes;
                }

                return register;
            };


            const setCurrentRegister = async function (): Promise<void> {
                let i: number = 0;
                let course: Course;
                while (!course && i < vm.courses.all.length) {
                    if (CourseUtils.isCurrentCourse(vm.courses.all[i])) course = vm.courses.all[i];
                    i++;
                }
                return vm.openRegister(course || vm.courses.all[0], null);
            };

            vm.openRegister = async function (course: Course, $event: Event) {
                if ($event && ($event.target as Element).className.includes('notify-bell')) {
                    notifyCourse(course);
                    return;
                }
                if (vm.isFuturCourse(course)) return;
                vm.register = createRegisterFromCourse(course);
                if (!course.register_id) {
                    try {
                        await vm.register.create();
                        course.register_id = vm.register.id;
                    } catch (err) {
                        notify.error('presences.register.creation.err');
                        vm.register.loading = false;
                        $scope.safeApply();
                        throw err;
                    }
                }
                vm.filter.date = moment(course.startDate).toDate();
                vm.filter.course = course;
                if ($route.current.action !== 'getRegister') {
                    $scope.redirectTo(`/registers/${vm.register.id}`);
                    $scope.safeApply();
                } else {
                    vm.register.eventer.on('loading::true', () => $scope.safeApply());
                    vm.register.eventer.on('loading::false', () => $scope.safeApply());
                    await vm.register.sync();
                    if (vm.register.teachers.length > 0 && _.countBy(vm.register.teachers, (teacher) => teacher.id === vm.filter.selected.registerTeacher.id) === 0)
                        vm.filter.selected.registerTeacher = vm.register.teachers[0];
                }
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
                    if (event === 'absence') vm.register.absenceCounter++;
                }
                vm.register.setStatus(RegisterStatus.IN_PROGRESS);
                $scope.safeApply();
            };

            vm.toggleAbsence = async (student) => {
                if (student.absence && student.absence.counsellor_input) return;
                await toggleEvent(student, 'absence', vm.register.start_date, vm.register.end_date);
                student.departure = undefined;
                student.lateness = undefined;
                $scope.safeApply();
            };

            vm.toggleLateness = async (student) => {
                const time = moment().millisecond(0).second(0);
                await toggleEvent(student, 'lateness', vm.register.start_date, time.format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]));
                student.lateness.end_date_time = time.toDate();
                $scope.safeApply();
            };

            vm.toggleDeparture = async (student) => {
                const time = moment().millisecond(0).second(0);
                await toggleEvent(student, 'departure', time.format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]), vm.register.end_date);
                student.departure.start_date_time = time.toDate();
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

            vm.closePanel = function () {
                delete vm.filter.student;
            };

            vm.getHistoryEventClassName = function (events) {
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

            vm.isCurrentSlot = function (slot) {
                return Math.abs(moment(slot.start).diff(vm.register.start_date)) < 3000 && Math.abs(moment(slot.end).diff(vm.register.end_date)) < 3000;
            };

            vm.loadCourses = async function (users: string[] = [model.me.userId], groups: string[] = [], structure: string = window.structure.id,
                                             start_date: string = DateUtils.format(vm.filter.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                                             end_date: string = DateUtils.format(vm.filter.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                                             forgotten_registers: boolean = vm.filter.forgotten): Promise<void> {
                vm.courses.clear();
                await vm.courses.sync(users, groups, structure, start_date, end_date, forgotten_registers);
                // if (vm.courses.all.length > 0 && $route.current.action === 'getRegister') {
                //     await setCurrentRegister();
                // }
            };

            vm.isFuturCourse = function (course) {
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

            vm.switchForgottenFilter = function () {
                vm.filter.forgotten = !vm.filter.forgotten;
                vm.loadCourses(extractSelectedTeacherIds(), extractSelectedGroupsName());
            };

            vm.formatDayDate = function (date) {
                return DateUtils.format(parseInt(date), DateUtils.FORMAT["DAY-DATE"]);
            };

            vm.switchRegisterTeacher = function (teacher) {
                vm.filter.selected.registerTeacher = teacher;
                vm.courses.sync([teacher.id], [], window.structure.id, DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                    DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), false);
            };

            const notifyCourse = async function (course: Course) {
                try {
                    const {data} = await notifyTeachers(course._id, course.startDate, course.endDate);
                    if ('register_id' in data) {
                        course.register_id = data.register_id;
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

            $scope.$watch(() => window.structure, () => {
                if ($route.current.action === "registers") {
                    actions[$route.current.action]($route.current.params)
                } else {
                    $scope.redirectTo('/registers');
                }
            });

            actions[$route.current.action]($route.current.params)
        }]);