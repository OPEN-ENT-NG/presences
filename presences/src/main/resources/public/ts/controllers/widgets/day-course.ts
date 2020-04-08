import {Me, model, moment, ng, notify} from 'entcore';
import {CourseUtils, DateUtils, PreferencesUtils} from "@common/utils";
import {Course, Courses, Register, RegisterStatus} from "../../models";
import {RegisterUtils} from "../../utilities";
import http from "axios";
import rights from "../../rights";

interface ViewModel {
    isMultipleSlot: boolean;
    dayCourse: Courses;
    register: Register;

    tooltipMultipleSlot(): string;

    switchMultipleSlot(): Promise<void>;

    formatHour(date: string): string;

    openRegister(course: Course, $event): Promise<void>;

    isFutureCourse(course: Course): boolean;

    isCurrentCourse(course: Course): boolean;

    canNotify(start_date: string, state: RegisterStatus): boolean;

    getGroups(classes: Array<string>, groups: Array<string>): Array<string>;
}

declare let window: any;

export const dayCourse = ng.controller('DayCourse', ['$scope', async function ($scope) {
    const vm: ViewModel = this;

    vm.dayCourse = new Courses();
    vm.register = new Register();

    let registerTimeSlot = await Me.preference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_REGISTER);

    vm.isMultipleSlot = registerTimeSlot.multipleSlot;

    const loadCourses = async (): Promise<void> => {
        let start_date = DateUtils.format(new Date(), DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        let end_date = DateUtils.format(new Date(), DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        let isMultipleSlot: boolean;
        if (model.me.profiles.some(profile => profile === "Personnel")) {
            isMultipleSlot = true;
        }
        await vm.dayCourse.sync([model.me.userId], [], window.structure.id, start_date, end_date, false, isMultipleSlot);
        console.log("vmDayCourse: ", vm.dayCourse);
        $scope.safeApply();
    };

    vm.openRegister = async (course: Course, $event): Promise<void> => {
        if ($event && ($event.target as Element).className.includes('notify-bell')) {
            await notifyCourse(course);
            return;
        }
        if (vm.isFutureCourse(course)) return;
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
        window.filter = {course: course};
        $scope.redirectTo(`/registers/${vm.register.id}`);
        $scope.safeApply();
    };

    vm.getGroups = function (classes: Array<string>, groups: Array<string>): Array<string> {
        return [...classes, ...groups];
    };

    vm.formatHour = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);

    vm.isFutureCourse = (course: Course): boolean => {
        if (!course) return true;
        return moment().isSameOrBefore(course.startDate);
    };

    vm.isCurrentCourse = (course: Course): boolean => {
        return CourseUtils.isCurrentCourse(course);
    };

    vm.canNotify = (start_date: string, state: RegisterStatus): boolean => {
        if (state && state === RegisterStatus.DONE) {
            return false;
        }
        return model.me.hasWorkflow(rights.workflow.notify)
            && isForgotten(start_date)
            && !vm.isFutureCourse(({startDate: start_date} as Course))
            && moment(DateUtils.setFirstTime(moment())).diff(moment(DateUtils.setFirstTime(start_date)), 'days') < 2
    };

    const notifyCourse = async (course: Course) => {
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

    const notifyTeachers = async (id, start, end) => {
        return http.post(`/presences/courses/${id}/notify`, {start, end});
    };

    const isForgotten = function (start_date): boolean {
        return moment().isAfter(moment(start_date).add(15, 'm'));
    };

    loadCourses();
}]);