import {_, model, moment, ng, notify, template} from 'entcore';
import {
    Absence,
    Course,
    Courses,
    Departure,
    EventType,
    Lateness,
    Register,
    RegisterStatus,
    Remark
} from '../models'
import {UserService} from '../services';
import {CourseUtils, DateUtils} from '@common/utils'
import rights from '../rights'

interface ViewModel {
    register: Register;
    courses: Courses;
    filter: { date: Date, student: any, teachers: any[], teacher: any; };
    RegisterStatus: any;

    openRegister(course: Course): Promise<void>;

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

    loadCourses(user?: string, structure?: string): Promise<void>;

    isFuturCourse(course: Course): boolean;

    searchTeacher(value: string): void;

    selectTeacher(model: any, teacher: any): void;

    dropTeacherFilter(): void;
}

export const registersController = ng.controller('RegistersController', ['$scope', 'UserService', function ($scope, UserService: UserService) {
    const vm: ViewModel = this;
    vm.register = undefined;
    vm.courses = new Courses();
    vm.RegisterStatus = RegisterStatus;
    vm.courses.eventer.on('loading::true', () => $scope.safeApply());
    vm.courses.eventer.on('loading::false', () => $scope.safeApply());
    vm.filter = {
        date: new Date(),
        student: undefined,
        teachers: undefined,
        teacher: undefined
    };

    vm.searchTeacher = async function (value) {
        const structureId = model.me.structures[0];
        try {
            vm.filter.teachers = await UserService.search(structureId, value, 'Teacher');
            vm.filter.teachers.map((teacher) => teacher.toString = () => teacher.displayName);
            $scope.safeApply();
        } catch (err) {
            throw err;
        }
    };

    vm.selectTeacher = function (model, teacher) {
        vm.filter.teacher = teacher;
        vm.filter.teachers = undefined;
        vm.loadCourses(vm.filter.teacher.id);
    };

    vm.dropTeacherFilter = function () {
        delete vm.filter.teacher;
        delete vm.register;
        vm.courses.clear();
        $scope.safeApply();
    };

    vm.formatHour = (date: string) => DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);

    const changeDate = function (step: number) {
        delete vm.register;
        vm.filter.date = DateUtils.add(vm.filter.date, step);
        if (model.me.hasWorkflow(rights.workflow.search) && !vm.filter.teacher) return;
        const id = model.me.hasWorkflow(rights.workflow.search) ? vm.filter.teacher.id : model.me.userId;
        vm.loadCourses(id);
    };

    vm.nextDate = () => changeDate(1);
    vm.previousDate = () => changeDate(-1);

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
        vm.openRegister(course || vm.courses.all[0]);
        $scope.safeApply();
    };

    vm.openRegister = async function (course: Course) {
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
        vm.register.eventer.once('loading::true', () => $scope.safeApply());
        vm.register.eventer.once('loading::false', () => $scope.safeApply());
        vm.register.sync();
        delete vm.filter.student;
        template.open('register', 'register/list-view');
        template.open('register-panel', 'register/panel');
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
                slot.events.push(event);
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
            student.remark.save();
        } else {
            student.remark.delete();
            student.remark = new Remark(vm.register.id, student.id, vm.register.start_date, vm.register.end_date);
        }
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
        const priority = [EventType.ABSENCE, EventType.LATENESS, EventType.DEPARTURE];
        const className = ['absence', 'lateness', 'departure'];
        let index = 3;
        for (let i = 0; i < events.length; i++) {
            let arrayIndex = priority.indexOf(events[i].type_id);
            index = arrayIndex < index ? arrayIndex : index;
        }

        return className[index] || '';
    };

    vm.isCurrentSlot = function (slot) {
        return Math.abs(moment(slot.start).diff(vm.register.start_date)) < 3000 && Math.abs(moment(slot.end).diff(vm.register.end_date)) < 3000;
    };

    vm.loadCourses = async function (user: string = model.me.userId, structure: string = model.me.structures[0]): Promise<void> {
        delete vm.register;
        vm.courses.all = [];
        $scope.safeApply();
        await vm.courses.sync(user, structure, DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]), DateUtils.format(vm.filter.date, DateUtils.FORMAT["YEAR-MONTH-DAY"]));
        if (vm.courses.all.length > 0) {
            await setCurrentRegister();
        }
    };

    vm.isFuturCourse = function (course) {
        if (!course) return true;
        return moment().isSameOrBefore(course.startDate);
    };

    if (!model.me.hasWorkflow(rights.workflow.search)) {
        vm.loadCourses();
    }
}]);