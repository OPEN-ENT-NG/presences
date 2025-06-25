import {idiom, model, moment, ng} from 'entcore';
import {IUserService, IViescolaireService} from "@common/services";
import {Student} from "@common/model/Student";
import {DateUtils, PreferencesUtils, safeApply, UserUtils} from "@common/utils";
import {IPeriod, IPeriodService} from "@presences/services/PeriodService";
import {ForgottenNotebookService, IInitService, IInitStatusResponse, IncidentService} from "../services";
import {IColor} from "@common/model/Color";
import {COLOR_TYPE} from "@common/core/constants/ColorType";
import {IAngularEvent} from "angular";
import {DASHBOARD_STUDENT_EVENTS} from "../core/enum/dashboard-student-events";
import {EventService} from "@presences/services";
import {
    EVENT_TYPES,
    IForgottenNotebookResponse,
    ISchoolYearPeriod,
    IStructure,
    IStructureSlot,
    IStudentEventRequest,
    IStudentEventResponse,
    IStudentIncidentResponse
} from "../models";
import {EVENT_RECOVERY_METHOD} from "@common/core/enum/event-recovery-method";
import {IRouting} from "@common/model/Route";
import {ROUTING_KEYS} from "@common/core/enum/routing-keys";
import {MobileUtils} from "@common/utils/mobile";
import {UPDATE_STUDENTS_EVENTS} from "@common/core/enum/select-children-events";

declare let window: any;

interface IEvent {
    absencesNoReason: string;
    absencesUnregularized: string;
    absencesRegularized: string;
    lateness: string;
    incidents: string;
    punishments: string;
    forgottenNotebook: string;
    departure: string;
}

interface IFilter {
    children: Array<Student>;
    selectedChildren: Student;
    periods: Array<IPeriod>;
    selectedPeriod: IPeriod;
    selectedStructure: IStructure;
}

interface IViewModel {
    filter: IFilter;
    routing: Array<IRouting>;
    color: IColor;
    types: typeof EVENT_TYPES;
    eventsTitle: IEvent;
    presenceEvents: IStudentEventResponse;
    incidentsEvents: IStudentIncidentResponse;
    forgottenNotebook: IForgottenNotebookResponse;
    structureTimeSlot: IStructureSlot;

    isPresencesInitialized: boolean;

    isMobile(): boolean;

    isCurrentMobileRoute(routing: IRouting): boolean;

    getRecoveryMethodLabel(recoveryMethod: string): string;

    isChild(): boolean;

    isRelative(): boolean;

    switchPeriod(): void;

    hasInitializedStructure(): boolean;
    isLoading: boolean;
    isChildrenEmpty: boolean;
}

export const dashboardStudentController = ng.controller('DashboardStudentController',
    ['$scope', '$route', '$location', 'UserService', 'PeriodService', 'EventService', 'IncidentService',
        'ForgottenNotebookService', 'ViescolaireService', 'InitService',
        function ($scope, $route, $location, userService: IUserService, periodService: IPeriodService,
                  eventService: EventService, incidentService: IncidentService, forgottenNotebookService: ForgottenNotebookService,
                  viescolaireService: IViescolaireService, initService: IInitService) {
            const vm: IViewModel = this;
            vm.isLoading = true;
            vm.isChildrenEmpty = false;
            vm.isPresencesInitialized = true;
            vm.filter = {
                children: [],
                selectedChildren: null,
                periods: [],
                selectedPeriod: null,
                selectedStructure: null
            };

            /* Mobile featuring only */
            vm.routing = [
                {
                    label: `${idiom.translate(`presences.dashboard`).toUpperCase()}`,
                    key: ROUTING_KEYS.DASHBOARD_STUDENT_MAIN, isSelected: true
                },
                {
                    label: `${idiom.translate(`presences.statements`).toUpperCase()}`,
                    key: ROUTING_KEYS.DASHBOARD_STUDENT_STATEMENT_FORM, isSelected: false
                }
            ];


            vm.structureTimeSlot = null;
            vm.color = COLOR_TYPE;
            vm.types = EVENT_TYPES;

            vm.eventsTitle = {
                absencesNoReason: `${idiom.translate(`presences.absence.unjustified`).toUpperCase()}`,
                absencesUnregularized: `${idiom.translate(`presences.absence.types.justified.not.regularized`).toUpperCase()}`,
                absencesRegularized: `${idiom.translate(`presences.absence.types.justified.regularized`).toUpperCase()}`,
                departure: `${idiom.translate(`presences.register.event_type.departure.early`).toUpperCase()}`,
                forgottenNotebook: `${idiom.translate(`presences.forgotten.notebook`).toUpperCase()}`,
                incidents: `${idiom.translate(`presences.alerts.type.INCIDENT`).toUpperCase()}`,
                punishments: `${idiom.translate(`presences.punishments.sanctions`).toUpperCase()}`,
                lateness: `${idiom.translate(`presences.alerts.type.LATENESS`).toUpperCase()}`
            };

            const load = async (): Promise<void> => {
                const studentData: Student = await getStudentData();

                // If user is a Relative but has no children
                // then show empty screen and stop execution
                if (vm.isRelative() && !studentData) {
                    vm.isChildrenEmpty = true;
                    vm.isLoading = false;
                    $scope.safeApply();
                    throw Error("User has no children");
                }

                await getPeriods();
            };

            const loadEvents = () => {
                vm.isLoading = true;
                const promises: Promise<any>[] = [];
                let eventsType: Array<string> = [EVENT_TYPES.NO_REASON, EVENT_TYPES.UNREGULARIZED, EVENT_TYPES.REGULARIZED, EVENT_TYPES.LATENESS, EVENT_TYPES.DEPARTURE];
                let incidentsEventsType: Array<string> = [EVENT_TYPES.INCIDENT, EVENT_TYPES.PUNISHMENT];
                promises.push(eventService.getStudentEvent(prepareEventRequest(eventsType, null, 0)));
                promises.push(incidentService.getStudentEvents(prepareEventRequest(incidentsEventsType, null, 0,
                    DateUtils.START_DAY_TIME, DateUtils.END_DAY_TIME)));
                promises.push(forgottenNotebookService.getStudentNotebooks(prepareEventRequest([], null, 0)));
                Promise.all(promises)
                    .then((values: any[]) => {
                        vm.presenceEvents = (<IStudentEventResponse>values[0]); // presences events
                        vm.incidentsEvents = (<IStudentIncidentResponse>values[1]); // incidents events
                        vm.forgottenNotebook = (<IForgottenNotebookResponse>values[2]); // forgotten notebook events
                        vm.isLoading = false;
                        $scope.safeApply();
                    })
                    .catch(err => console.log(err));
                $scope.safeApply();
            };

            const prepareEventRequest = (eventsType: Array<string>, limit: number, offset: number,
                                         additionalStartTime?: string, additionalEndTime?: string): IStudentEventRequest => {
                let startResult: string = moment(vm.filter.selectedPeriod.timestamp_dt).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                let endResult: string = moment(vm.filter.selectedPeriod.timestamp_fn).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);

                const start: string = startResult +
                    ((additionalStartTime !== undefined && additionalStartTime !== null && additionalStartTime !== "")
                        ? " " + additionalStartTime : "");

                const end: string = endResult +
                    ((additionalEndTime !== undefined && additionalEndTime !== null && additionalEndTime !== "")
                        ? " " + additionalEndTime : "");

                return {
                    structure_id: vm.filter.selectedChildren.structure.id,
                    student_id: vm.filter.selectedChildren.id,
                    start_at: start,
                    end_at: end,
                    ...(eventsType ? {type: eventsType} : {}),
                    limit: limit,
                    offset: offset,
                }
            };

            const getPresencesInitStatus = async (structureId: string): Promise<void> => {
                vm.isPresencesInitialized = await initService.getPresencesInitStatus(structureId);
            }

            const getStudentData = async (): Promise<Student> => {
                // If the user is a relative, get child info
                if (vm.isRelative() && vm.filter.children.length === 0) {
                    vm.filter.children = await userService.getChildrenUser(model.me.userId);
                    vm.filter.children.forEach((child: Student) => child.structure = child.structures[0]);
                    vm.filter.selectedChildren = vm.filter.children[0];
                } else {
                    // if the user is a student, get info about the student itself
                    vm.filter.selectedChildren = await userService.getChildUser(model.me.userId);
                    vm.filter.selectedChildren.structure = vm.filter.selectedChildren.structures[0];
                }
                return vm.filter.selectedChildren;
            };

            const getPeriods = (): Promise<void> => {
                return new Promise((resolve) => {
                    Promise.all([
                        periodService.get(vm.filter.selectedChildren.structure.id, vm.filter.selectedChildren.structure.classes[0].id),
                        viescolaireService.getSchoolYearDates(vm.filter.selectedChildren.structure.id)
                    ])
                        .then((values: any[]) => {
                            let periods: Array<IPeriod> = (<Array<IPeriod>>values[0]);
                            let schoolyear: ISchoolYearPeriod = (<ISchoolYearPeriod>values[1]);

                            if (periods.length !== 0) {
                                periods.forEach((period: IPeriod) => {
                                    period.label = `${idiom.translate(`viescolaire.periode.${period.type}`)}  ${period.ordre}`
                                });
                                vm.filter.periods = periods;
                                let currentPeriod: IPeriod = periods.find((period: IPeriod) =>
                                    moment().isBetween(period.timestamp_dt, period.timestamp_fn)
                                );
                                vm.filter.selectedPeriod = currentPeriod != undefined ? currentPeriod : vm.filter.periods[0];
                            } else {
                                vm.filter.periods = [];
                                vm.filter.selectedPeriod = {
                                    timestamp_dt: schoolyear.start_date,
                                    timestamp_fn: schoolyear.end_date
                                };
                            }
                            $scope.safeApply();
                            resolve(undefined);
                        })
                        .catch(err => console.error(err));
                });
            };

            const getTimeSlots = async (structure_id: string): Promise<void> => {
                vm.structureTimeSlot = await viescolaireService.getSlotProfile(structure_id);
                $scope.safeApply();
            };

            const reloadType = (type: string): void => {
                switch (type) {
                    case EVENT_TYPES.NO_REASON:
                    case EVENT_TYPES.UNREGULARIZED:
                    case EVENT_TYPES.REGULARIZED:
                    case EVENT_TYPES.LATENESS:
                    case EVENT_TYPES.DEPARTURE: {
                        eventService.getStudentEvent(prepareEventRequest([type], 0, 0))
                            .then((value: IStudentEventResponse) => {
                                vm.presenceEvents.all[type] = value.all[type];
                                $scope.safeApply();
                            })
                            .catch(err => console.error(err));
                        break;
                    }
                    case EVENT_TYPES.NOTEBOOK:
                        forgottenNotebookService.getStudentNotebooks(prepareEventRequest([], 0, 0))
                            .then((value: IForgottenNotebookResponse) => {
                                vm.forgottenNotebook.all = value.all;
                                $scope.safeApply();
                            })
                            .catch(err => console.error(err));
                        break;
                    case EVENT_TYPES.PUNISHMENT:
                    case EVENT_TYPES.INCIDENT:
                        incidentService.getStudentEvents(prepareEventRequest([type], 0, 0,
                            DateUtils.START_DAY_TIME, DateUtils.END_DAY_TIME))
                            .then((value) => {
                                // @ts-ignore
                                vm.incidentsEvents.all[type] = value.all[type];
                                $scope.safeApply();
                            })
                            .catch(err => console.error(err));
                        break;
                }
            };

            vm.isCurrentMobileRoute = (routing: IRouting): boolean => {
                if (MobileUtils.isMobile()) {
                    return routing.isSelected;
                }
                return true;
            };

            vm.isMobile = (): boolean => {
                return MobileUtils.isMobile();
            };

            vm.getRecoveryMethodLabel = (recoveryMethod: string): string => {
                switch (recoveryMethod) {
                    case EVENT_RECOVERY_METHOD.HALF_DAY:
                        return idiom.translate('presences.recovery.method.half_day');
                    case EVENT_RECOVERY_METHOD.DAY:
                        return idiom.translate('presences.recovery.method.day');
                    case EVENT_RECOVERY_METHOD.HOUR:
                        return idiom.translate('presences.recovery.method.method.hour');
                }
            };

            vm.isRelative = (): boolean => {
                return UserUtils.isRelative(model.me.type)
            };

            vm.isChild = (): boolean => {
                return UserUtils.isChild(model.me.type);
            };

            vm.switchPeriod = (): void => {
                loadEvents();
            };

            vm.hasInitializedStructure = (): boolean => {
                return window.structure !== undefined && vm.isPresencesInitialized;
            }

            /* event handler */
            $scope.$watch(() => window.structure, () => {
                getPresencesInitStatus(window.structure.id)
                    .then(() => load())
                    .then(() => {
                        if (vm.isPresencesInitialized) {
                            getTimeSlots((vm.filter.selectedChildren && vm.filter.selectedChildren.structure)
                                ? vm.filter.selectedChildren.structure.id : window.structure.id)
                                .then(() => loadEvents())
                                .catch(err => console.error(err))
                        } else {
                            vm.isLoading = false;
                        }
                    })
                    .then(() => safeApply($scope))
                    .catch(err => console.error(err))
            });

            $scope.$on(DASHBOARD_STUDENT_EVENTS.SEND_TOGGLE, (event: IAngularEvent, isToggleOnce: boolean, type: string) => {
                // if toggle has already been applied once, no need to reload once again (unless we switch period/student)
                if (!isToggleOnce) {
                    reloadType(type);
                }
            });

            $scope.$on(UPDATE_STUDENTS_EVENTS.UPDATE, (event: IAngularEvent, child: Student) => {
                vm.filter.selectedChildren = child;
                getTimeSlots((vm.filter.selectedChildren && vm.filter.selectedChildren.structure)
                    ? vm.filter.selectedChildren.structure.id : window.structure.id)
                    .then(() => getPeriods())
                    .then(() => loadEvents())
                    .catch(err => console.error(err))
            });
        }]);
