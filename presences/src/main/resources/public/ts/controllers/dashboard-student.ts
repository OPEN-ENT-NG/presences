import {idiom, model, moment, ng} from 'entcore';
import {IUserService, IViescolaireService} from "@common/services";
import {Student} from "@common/model/Student";
import {DateUtils, UserUtils} from "@common/utils";
import {IPeriod, IPeriodService} from "@presences/services/PeriodService";
import {ForgottenNotebookService, IncidentService} from "../services";
import {IColor} from "@common/model/Color";
import {COLOR_TYPE} from "@common/core/constants/ColorType";
import {IAngularEvent} from "angular";
import {DASHBOARD_STUDENT_EVENTS} from "../core/enum/dashboard-student-events";
import {EventService} from "@presences/services";
import {
    EVENT_TYPES,
    IForgottenNotebookResponse,
    ISchoolYearPeriod,
    IStructureSlot,
    IStudentEventRequest,
    IStudentEventResponse,
    IStudentIncidentResponse
} from "../models";
import {EVENT_RECOVERY_METHOD} from "@common/core/enum/event-recovery-method";
import {IRouting} from "@common/model/Route";
import {ROUTING_EVENTS, ROUTING_KEYS} from "@common/core/enum/routing-keys";
import {MobileUtils} from "@common/utils/mobile";

declare let window: any;

interface IEvent {
    absences: string;
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

    isMobile(): boolean;

    isCurrentMobileRoute(routing: IRouting): boolean;

    getRecoveryMethodLabel(recoveryMethod: string): string;

    isChild(): boolean;

    isRelative(): boolean;

    switchChild(): void;

    switchPeriod(): void;

    isLoading: boolean;
}

export const dashboardStudentController = ng.controller('DashboardStudentController',
    ['$scope', '$route', '$location', 'UserService', 'PeriodService', 'EventService', 'IncidentService',
        'ForgottenNotebookService', 'ViescolaireService',
        function ($scope, $route, $location, userService: IUserService, periodService: IPeriodService,
                  eventService: EventService, incidentService: IncidentService, forgottenNotebookService: ForgottenNotebookService,
                  viescolaireService: IViescolaireService) {
            const vm: IViewModel = this;
            vm.isLoading = true;
            vm.filter = {
                children: [],
                selectedChildren: null,
                periods: [],
                selectedPeriod: null
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
                absences: `${idiom.translate(`presences.alerts.type.ABSENCE`).toUpperCase()}`,
                departure: `${idiom.translate(`presences.register.event_type.departure.early`).toUpperCase()}`,
                forgottenNotebook: `${idiom.translate(`presences.forgotten.notebook`).toUpperCase()}`,
                incidents: `${idiom.translate(`presences.alerts.type.INCIDENT`).toUpperCase()}`,
                punishments: `${idiom.translate(`presences.punishments.sanctions`).toUpperCase()}`,
                lateness: `${idiom.translate(`presences.alerts.type.LATENESS`).toUpperCase()}`
            };

            const load = async (): Promise<void> => {
                await getChildrenData();
                await getPeriods();
            };

            const loadEvents = () => {
                vm.isLoading = true;
                const promises: Promise<any>[] = [];
                let eventsType: Array<string> = [EVENT_TYPES.UNJUSTIFIED, EVENT_TYPES.JUSTIFIED, EVENT_TYPES.LATENESS, EVENT_TYPES.DEPARTURE];
                let incidentsEventsType: Array<string> = [EVENT_TYPES.INCIDENT, EVENT_TYPES.PUNISHMENT];
                promises.push(eventService.getStudentEvent(prepareEventRequest(eventsType, 2, 0)));
                promises.push(incidentService.getStudentEvents(prepareEventRequest(incidentsEventsType, 2, 0)));
                promises.push(forgottenNotebookService.getStudentNotebooks(prepareEventRequest([], 2, 0)));
                Promise.all(promises)
                    .then((values: any[]) => {
                        vm.presenceEvents = (<IStudentEventResponse>values[0]); // presences events
                        vm.incidentsEvents = (<IStudentIncidentResponse>values[1]) // incidents events
                        vm.forgottenNotebook = (<IForgottenNotebookResponse>values[2]) // forgotten notebook events
                        vm.isLoading = false;
                        $scope.safeApply();
                    });
                $scope.safeApply();
            };

            const prepareEventRequest = (eventsType: Array<string>, limit: number, offset: number): IStudentEventRequest => {
                const start: string = moment(vm.filter.selectedPeriod.timestamp_dt).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                const end: string = moment(vm.filter.selectedPeriod.timestamp_fn).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                return {
                    structure_id: window.structure.id,
                    student_id: vm.filter.selectedChildren.id,
                    start_at: start,
                    end_at: end,
                    ...(eventsType ? {type: eventsType} : {}),
                    limit: limit,
                    offset: offset,
                }
            };

            const getChildrenData = async (): Promise<void> => {
                if (vm.isRelative() && vm.filter.children.length === 0) {
                    vm.filter.children = await userService.getChildrenUser(model.me.userId);
                    vm.filter.selectedChildren = vm.filter.children[0];
                } else {
                    /* Set student info */
                    vm.filter.selectedChildren = new Student({
                        idEleve: model.me.userId,
                        idClasse: model.me.classes[0],
                        classId: model.me.classes[0],
                        displayName: model.me.username ? model.me.username : model.me.lastName + " " + model.me.firstName
                    });
                }
            };

            const getPeriods = (): Promise<void> => {
                return new Promise((resolve) => {
                    Promise.all([
                        periodService.get(window.structure.id, vm.filter.selectedChildren.classId),
                        viescolaireService.getSchoolYearDates(window.structure.id)
                    ]).then((values: any[]) => {
                        let periods: Array<IPeriod> = (<Array<IPeriod>>values[0]);
                        let schoolyear: ISchoolYearPeriod = (<ISchoolYearPeriod>values[1]);

                        if (periods.length !== 0) {
                            periods.forEach((period: IPeriod) => {
                                period.label = `${idiom.translate(`viescolaire.periode.${period.type}`)}  ${period.ordre}`
                            });
                            vm.filter.periods = periods;
                            let currentPeriod = periods.find((period: IPeriod) =>
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
                    });
                });
            };

            const getTimeSlots = async (structure_id: string): Promise<void> => {
                vm.structureTimeSlot = await viescolaireService.getSlotProfile(structure_id);
                $scope.safeApply();
            };

            const reloadType = (type: string): void => {
                switch (type) {
                    case EVENT_TYPES.JUSTIFIED:
                    case EVENT_TYPES.UNJUSTIFIED:
                    case EVENT_TYPES.LATENESS:
                    case EVENT_TYPES.DEPARTURE: {
                        eventService.getStudentEvent(prepareEventRequest([type], 0, 0))
                            .then((value: IStudentEventResponse) => {
                                vm.presenceEvents.all[type] = value.all[type];
                                $scope.safeApply();
                            });
                        break;
                    }
                    case EVENT_TYPES.NOTEBOOK:
                        forgottenNotebookService.getStudentNotebooks(prepareEventRequest([], 0, 0))
                            .then((value: IForgottenNotebookResponse) => {
                                vm.forgottenNotebook.all = value.all;
                                $scope.safeApply();
                            });
                        break;
                    case EVENT_TYPES.PUNISHMENT:
                    case EVENT_TYPES.INCIDENT:
                        incidentService.getStudentEvents(prepareEventRequest([type], 0, 0))
                            .then((value) => {
                                vm.incidentsEvents.all[type] = value.all[type];
                                $scope.safeApply();
                            });
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

            vm.switchChild = (): void => {
                getPeriods();
                loadEvents();
            };

            vm.switchPeriod = (): void => {
                loadEvents();
            };

            /* event handler */
            $scope.$watch(() => window.structure, async () => {
                if ('structure' in window) {
                    await Promise.all([load(), getTimeSlots(window.structure.id)]);
                    await loadEvents();
                    $scope.safeApply();
                }
            });

            $scope.$on(DASHBOARD_STUDENT_EVENTS.SEND_TOGGLE, (event: IAngularEvent, isToggleOnce: boolean, type: string) => {
                // if toggle has already been applied once, no need to reload once again (unless we switch period/student)
                if (!isToggleOnce) {
                    reloadType(type);
                }
            });

            $scope.$on(ROUTING_EVENTS.SWITCH, (event: IAngularEvent, routerKey: string) => {
                console.log("router: ", routerKey);
                console.log("vm.router: ", vm.routing);
            });

        }]);