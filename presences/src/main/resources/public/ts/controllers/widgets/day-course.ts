import {Me, model, moment, ng, notify, idiom as lang} from 'entcore';
import {CourseUtils, DateUtils, PreferencesUtils, PresencesPreferenceUtils} from '@common/utils';
import {Course, Courses, Register, RegisterStatus} from '../../models';
import {RegisterUtils} from '../../utilities';
import http from 'axios';
import rights from '../../rights';
import {Setting, settingService} from '../../services';

export interface DayCourseVm {
    $onInit(): any;

    $onDestroy(): any;

    isMultipleSlot: boolean;
    isMultipleSlotUserPreference: boolean;
    dayCourse: Courses;
    register: Register;

    switchMultipleSlot(): Promise<void>;

    tooltipMultipleSlot(): string;

    formatHour(date: string): string;

    openRegister(course: Course, $event): Promise<void>;

    isFutureCourse(course: Course): boolean;

    isCurrentCourse(course: Course): boolean;

    canNotify(start_date: string, state: RegisterStatus): boolean;

    getGroups(classes: Array<string>, groups: Array<string>): Array<string>;
}

declare let window: any;

export const dayCourse = ng.controller('DayCourse', ['$scope', function ($scope) {
    const vm: DayCourseVm = this;
    $scope.$watch(() => window.structure, (nouvelleValeur, ancienneValeur) => {
        try {
            console.log("Watcher Ancienne valeur :", ancienneValeur);
            console.log("Watcher Nouvelle valeur :", nouvelleValeur);
            // Lancer une exception pour capturer la stack trace
            throw new Error('Capture de la stack trace');
        } catch (e) {
            console.error('Erreur:', e);
            console.error('Stack trace:', e.stack);
        }
    });
    vm.$onInit = async (): Promise<void> => {
        vm.dayCourse = new Courses();
        vm.register = new Register();
        /* on (watch) */

        try {
            console.log("1");
            console.log(window.structure);
            vm.isMultipleSlot = await settingService.retrieveMultipleSlotSetting(window.structure.id);

            let registerTimeSlot: any = await Me.preference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_REGISTER);
            vm.isMultipleSlotUserPreference = ('multipleSlot' in registerTimeSlot) ?
                registerTimeSlot.multipleSlot : await initMultipleSlotPreference();

        } catch (e) {
            console.error(e);
            notify.error(e);
            vm.isMultipleSlot = true;
        }
    };

    const initMultipleSlotPreference = async (): Promise<boolean> => {
        await PresencesPreferenceUtils.updatePresencesRegisterPreference(true);
        return true;
    };

    const loadCourses = async (): Promise<void> => {
        let start_date: string = DateUtils.format(new Date(), DateUtils.FORMAT['YEAR-MONTH-DAY']);
        let end_date: string = DateUtils.format(new Date(), DateUtils.FORMAT['YEAR-MONTH-DAY']);
        vm.dayCourse.clear();
        await vm.dayCourse.sync([model.me.userId], [], window.structure.id, start_date, end_date,
            null, null, false,
            vm.isMultipleSlot ? vm.isMultipleSlotUserPreference : false);
        $scope.safeApply();
    };

    vm.openRegister = async (course: Course, $event): Promise<void> => {
        if ($event && ($event.target as Element).className.includes('notify-bell')) {
            await notifyCourse(course);
            return;
        }
        if (vm.isFutureCourse(course) || !course.allowRegister) return;
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
        return moment().add(15, 'minutes').isSameOrBefore(course.startDate);
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

    vm.switchMultipleSlot = async (): Promise<void> => {
        await PresencesPreferenceUtils.updatePresencesRegisterPreference(vm.isMultipleSlotUserPreference);
        loadCourses();
    };

    vm.tooltipMultipleSlot = (): string => {
        let tooltipText: string = vm.isMultipleSlotUserPreference ?
            'presences.widgets.day.set.multiple.slot.toolip.disable'
            : 'presences.widgets.day.set.multiple.slot.toolip.activate';
        return lang.translate(tooltipText);
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

    vm.$onDestroy = () => {
    };
}]);