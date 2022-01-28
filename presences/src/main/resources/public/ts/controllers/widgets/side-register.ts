import {Me, model, ng, notify} from 'entcore';
import {Absence, Course, Courses, Register, RegisterStatus, RegisterStudent} from '../../models';
import {DateUtils} from '@common/utils/date';
import {CourseUtils, PreferencesUtils, PresencesPreferenceUtils} from '@common/utils';
import {RegisterUtils} from '../../utilities';
import {COURSE_EVENTS} from '@common/model';
import {IAngularEvent} from 'angular';
import {Setting, settingService} from '../../services';
import http, {AxiosResponse} from "axios";

interface ViewModel {
    courses: Courses;
    register: Register;

    load(): Promise<void>;

    toggleAbsence(student: RegisterStudent): Promise<void>;

    isAbsenceDisabled(student: RegisterStudent): boolean;

    validRegister(): Promise<void>;

    isEmpty(): boolean;

    openRegister(course: Course): void;
}

declare let window: any;

export const sideRegisterController = ng.controller('SideRegisterController', ['$scope', 'RegisterService',
    function ($scope) {
        const vm: ViewModel = this;

        vm.courses = new Courses();
        vm.register = undefined;

        vm.load = async (): Promise<void> => {
            try {
                let start_date: string = DateUtils.format(new Date(), DateUtils.FORMAT['YEAR-MONTH-DAY']);
                let end_date: string = DateUtils.format(new Date(), DateUtils.FORMAT['YEAR-MONTH-DAY']);

                let isMultipleSlot: boolean;
                try {
                    if (model.me.profiles.some(profile => profile === 'Personnel')) {
                        isMultipleSlot = true;
                    } else {
                        isMultipleSlot = await settingService.retrieveMultipleSlotSetting(window.structure.id);

                        // When multiple slot setting is activated, fetch the user preference
                        if (isMultipleSlot) {
                            let registerTimeSlot: any = await getPreference();
                            isMultipleSlot = ('multipleSlot' in registerTimeSlot) ?
                                registerTimeSlot.multipleSlot : await initMultipleSlotPreference();
                        }
                    }
                }
                catch (e) {
                    isMultipleSlot = true;
                }
                await vm.courses.sync([model.me.userId], [], window.structure.id, start_date, end_date,
                    null, null, false, isMultipleSlot);
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

        vm.isAbsenceDisabled = function (student: RegisterStudent): boolean {
            return RegisterUtils.isAbsenceDisabled(student, vm.register);
        };

        vm.validRegister = async (): Promise<void> => {
            if (vm.register.id) {
                await vm.register.setStatus(RegisterStatus.DONE);
            } else {
                notify.error('presences.register.validation.error');
            }
        };

        vm.openRegister = async function (course: Course) {
            window.filter = {course: course};
            $scope.redirectTo(`/registers/${course.registerId}`);
            $scope.safeApply();
        };

        const loadRegister = async (): Promise<void> => {
            if (vm.courses.all.length > 0) {
                let currentCourse: Course = vm.courses.all.find(
                    (course: Course): boolean => CourseUtils.isCurrentCourse(course) && course.allowRegister);
                if (currentCourse) {
                    vm.register = RegisterUtils.createRegisterFromCourse(currentCourse);
                    /* create or sync register on current course*/
                    if (!currentCourse.registerId) {
                        vm.register.create().then(async (): Promise<void> => {
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
        };

        vm.isEmpty = function (): boolean {
            return vm.register === undefined || vm.register.id === undefined;
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
                let o: Absence;
                o = new Absence(vm.register.id, student.id, start_date, end_date);
                student[event] = o;
                try {
                    await student[event].create();
                } catch (err) {
                    notify.error('presences.event.creation.err');
                    throw err;
                }
            }
            await vm.register.setStatus(RegisterStatus.IN_PROGRESS);
            $scope.safeApply();
        }

        async function initMultipleSlotPreference(): Promise<boolean> {
            await PresencesPreferenceUtils.updatePresencesRegisterPreference(true);
            return true;
        }

        async function getPreference(): Promise<any> {
            let response: AxiosResponse = await http.get(`userbook/preference/presences.register`);
            if (response.status === 200 || response.status === 201) {
                return JSON.parse(response.data.preference);
            } else {
                return {};
            }
        }
        
        /* Events handler */
        $scope.$watch(() => window.structure, async () => {
            if (window.structure) {
                vm.load();
            }
        });

        /* on (watch) */
        $scope.$on(COURSE_EVENTS.OPEN_REGISTER, (event: IAngularEvent, args) => vm.openRegister(args));
    }]);