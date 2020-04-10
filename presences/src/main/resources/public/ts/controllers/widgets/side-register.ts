import {model, ng, notify, toasts} from 'entcore';
import {Absence, Course, Courses, Register, RegisterStatus, RegisterStudent} from '../../models';
import {DateUtils} from '@common/utils/date';
import {CourseUtils} from "@common/utils";
import {RegisterUtils} from "../../utilities";
import {COURSE_EVENTS} from "@common/model";
import {IAngularEvent} from "angular";

interface ViewModel {
    courses: Courses;
    register: Register;
    RegisterStatus: any;

    load(): Promise<void>;

    toggleAbsence(student: RegisterStudent): Promise<void>;

    validRegister(): Promise<void>;

    isEmpty(): boolean;

    openRegister(course: Course): void;
}

declare let window: any;

export const sideRegisterController = ng.controller('SideRegisterController', ['$scope',
    function ($scope) {
        const vm: ViewModel = this;

        vm.courses = new Courses();
        vm.register = undefined;
        vm.RegisterStatus = RegisterStatus;

        vm.load = async function (): Promise<void> {
            try {
                let start_date = DateUtils.format(new Date(), DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                let end_date = DateUtils.format(new Date(), DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                let isMultipleSlot: boolean;
                if (model.me.profiles.some(profile => profile === "Personnel")) {
                    isMultipleSlot = true;
                }
                await vm.courses.sync([model.me.userId], [], window.structure.id, start_date, end_date, false, isMultipleSlot);
                await loadRegister();
                $scope.safeApply();
            } catch (err) {
                notify.error('presences.current.course.failed');
            }
        };

        vm.toggleAbsence = async function (student: RegisterStudent): Promise<void> {
            if (RegisterUtils.isAbsenceDisabled(student, vm.register)) return;
            await toggleEvent(student, 'absence', vm.register.start_date, vm.register.end_date);
            student.departure = undefined;
            student.lateness = undefined;
            $scope.safeApply();
        };

        vm.validRegister = async function () {
            try {
                vm.register.setStatus(vm.RegisterStatus.DONE);
                toasts.confirm('presences.register.validation.success');
            } catch (err) {
                toasts.warning('presences.register.validation.error');
            }
        };

        vm.openRegister = async function (course: Course) {
            window.filter = {course: course};
            $scope.redirectTo(`/registers/${course.registerId}`);
            $scope.safeApply();
        };

        async function loadRegister() {
            if (vm.courses.all.length > 0) {
                let currentCourse = vm.courses.all.find(course => CourseUtils.isCurrentCourse(course));
                if (currentCourse) {
                    vm.register = RegisterUtils.createRegisterFromCourse(currentCourse);
                    /* create or sync register on current course*/
                    if (!currentCourse.registerId) {
                        vm.register.create().then(async () => {
                            await vm.register.sync();
                            currentCourse.registerId = vm.register.id;
                            $scope.$emit(COURSE_EVENTS.SEND_COURSE, currentCourse);
                            $scope.safeApply();
                        });
                    } else {
                        await vm.register.sync();
                        $scope.$emit(COURSE_EVENTS.SEND_COURSE, currentCourse);
                    }
                }
            }
        }

        vm.isEmpty = function (): boolean {
            return vm.register === undefined;
        };

        async function toggleEvent(student, event, start_date, end_date) {
            if (student[event]) {
                try {
                    await student[event].delete();
                } catch (err) {
                    notify.error('presences.event.deletion.err');
                    throw err;
                }
                delete student[event];
            } else {
                let o;
                o = new Absence(vm.register.id, student.id, start_date, end_date);
                student[event] = o;
                try {
                    await student[event].create();
                } catch (err) {
                    notify.error('presences.event.creation.err');
                    throw err;
                }
            }
            vm.register.setStatus(RegisterStatus.IN_PROGRESS);
            $scope.safeApply();
        }

        vm.load();

        $scope.$on(COURSE_EVENTS.OPEN_REGISTER, (event: IAngularEvent, args) => vm.openRegister(args));
    }]);