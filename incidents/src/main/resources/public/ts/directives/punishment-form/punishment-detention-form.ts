import {idiom as lang, model, moment, ng, toasts} from 'entcore';
import {
    IPDetentionField,
    IPunishment,
    IPunishmentBody,
    IPunishmentResponse,
    ITimeSlot,
    TimeSlotHourPeriod
} from "@incidents/models";
import {DateUtils, safeApply, UsersSearch} from "@common/utils";
import {User} from "@common/model/User";
import {SearchService} from "@common/services";
import {PeriodFormUtils} from "@common/utils/periodForm";
import {ROOTS} from "@incidents/core/const/roots";
import {AxiosError} from "axios";
import {punishmentService} from "@incidents/services";

declare let window: any;

interface IDetentionSlot {
    date: string,
    start_time: Date,
    end_time: Date,
    isFreeSelection: boolean,
    timeSlotTimePeriod?: {
        start: ITimeSlot;
        end: ITimeSlot;
    },
    detentionField: IPDetentionField,
}

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    form: IPunishmentBody;
    punishment: IPunishment;
    usersSearch: UsersSearch;
    timeSlots: Array<ITimeSlot>;
    owner: User;
    ownerSearch: string;
    detentionSlots: Array<IDetentionSlot>;
    timeSlotHourPeriod: typeof TimeSlotHourPeriod;
    selectTimeSlotText: string;
    editmode: boolean

    selectTimeSlot(hourPeriod: TimeSlotHourPeriod, detentionSlot: IDetentionSlot): void;

    changeTimeInput(detentionSlot: IDetentionSlot): void;

    changeDateInput(detentionSlot: IDetentionSlot): void;

    searchOwner(value: string): Promise<void>;

    selectOwner(model, owner: User): void;

    getDisplayOwnerName(): string;

    createSlot(): void;

    deleteSlot(detentionSlot: IDetentionSlot): void;

    setTimeSlot(detentionSlot: IDetentionSlot): void;
}

export const PunishmentDetentionForm = ng.directive('punishmentDetentionForm', ['SearchService',
    (SearchService: SearchService) => {
        return {
            restrict: 'E',
            transclude: true,
            scope: {
                form: '=',
                timeSlots: '=',
                punishment: '='
            },
            templateUrl: `${ROOTS.directive}punishment-form/punishment-detention-form.html`,
            controllerAs: 'vm',
            bindToController: true,
            replace: true,
            controller: ['$scope', function ($scope) {
                const vm: IViewModel = <IViewModel>this;


                vm.$onInit = async () => {
                    vm.timeSlotHourPeriod = TimeSlotHourPeriod;
                    vm.selectTimeSlotText = lang.translate('presences.pick.timeslot');
                    vm.detentionSlots = [];
                    vm.editmode = vm.punishment !== undefined && vm.punishment.id !== undefined;
                    if (vm.editmode) {
                        vm.form.owner_id = vm.punishment.owner.id;

                        //vm.punishment.fields is an IPDetentionField that corresponds to the punishment that we are modifying.
                        //vm.form.fields will contain all punishments that have the same grouped_punishment_id as the punishment being modified
                        vm.form.fields = [vm.punishment.fields];
                        (<Array<IPDetentionField>>vm.form.fields)[0].id = vm.punishment.id;

                        //Get and add other punishment
                        try {
                            const listPunishment: IPunishmentResponse = await punishmentService.getGroupedPunishmentId(vm.punishment.grouped_punishment_id, vm.punishment.structure_id);
                            vm.form.fields = listPunishment.all
                                .sort((punishment1: IPunishment, punishment2: IPunishment) => moment((<IPDetentionField>punishment1.fields).start_at).unix() - moment((<IPDetentionField>punishment2.fields).start_at).unix())
                                .map((punishment: IPunishment) => {
                                    const detentionField: IPDetentionField = (<IPDetentionField>punishment.fields);
                                    detentionField.id = punishment.id;

                                    const detentionSlot: IDetentionSlot = {
                                        date: moment(detentionField.start_at),
                                        start_time: moment(detentionField.start_at).set({
                                            second: 0,
                                            millisecond: 0
                                        }).toDate(),
                                        end_time: moment(detentionField.end_at).set({
                                            second: 0,
                                            millisecond: 0
                                        }).toDate(),
                                        isFreeSelection: true,
                                        timeSlotTimePeriod: undefined,
                                        detentionField: undefined,
                                    }
                                    detentionSlot.detentionField = detentionField;
                                    vm.detentionSlots.push(detentionSlot);
                                    let start: string = DateUtils.format(detentionSlot.detentionField.start_at, DateUtils.FORMAT["HOUR-MINUTES"]);
                                    let end: string = DateUtils.format(detentionSlot.detentionField.end_at, DateUtils.FORMAT["HOUR-MINUTES"]);
                                    detentionSlot.timeSlotTimePeriod = {
                                        start: undefined,
                                        end: undefined
                                    };
                                    vm.timeSlots.forEach((slot: ITimeSlot) => {
                                        if (slot.startHour === start) {
                                            detentionSlot.timeSlotTimePeriod.start = slot;
                                        }
                                        if (slot.endHour === end) {
                                            detentionSlot.timeSlotTimePeriod.end = slot;
                                        }
                                    });
                                    if (detentionSlot.timeSlotTimePeriod.start && detentionSlot.timeSlotTimePeriod.end) {
                                        detentionSlot.isFreeSelection = false;
                                    }

                                    // on switch category (in case we reset)
                                    if (!(Object.keys(detentionSlot.detentionField).length > 0)) {
                                        detentionSlot.detentionField = {
                                            start_at: DateUtils.format(detentionSlot.start_time, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                                            end_at: DateUtils.format(detentionSlot.end_time, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                                            place: ""
                                        } as IPDetentionField;
                                        detentionSlot.isFreeSelection = false;
                                        detentionSlot.timeSlotTimePeriod = {
                                            start: null,
                                            end: null
                                        };
                                    }

                                    return punishment.fields;
                                });
                        }
                        catch (e) {
                            vm.form.fields = [];
                            toasts.warning('incidents.punishments.get.grouped.err');
                        }
                        vm.owner = vm.punishment.owner;
                    } else {
                        vm.form.owner_id = model.me.userId;
                        (<Array<IPDetentionField>>vm.form.fields) = [];
                        vm.createSlot();
                        vm.owner = model.me;
                    }
                    safeApply($scope);
                };

                vm.createSlot = (): void => {
                    const detentionSlot: IDetentionSlot = {
                        date: moment(),
                        start_time: moment().set({second: 0, millisecond: 0}).toDate(),
                        end_time: moment().add(1, 'h').set({second: 0, millisecond: 0}).toDate(),
                        isFreeSelection: false,
                        timeSlotTimePeriod: {
                            start: null,
                            end: null
                        },
                        detentionField: undefined,
                    }
                    vm.detentionSlots.push(detentionSlot)
                    const slot: IPDetentionField = {
                        start_at: DateUtils.format(vm.detentionSlots[0].start_time, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                        end_at: DateUtils.format(vm.detentionSlots[0].end_time, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                        place: ""
                    };
                    detentionSlot.detentionField = slot;
                    (<Array<IPDetentionField>>vm.form.fields).push(slot);
                };
            }],
            link: function ($scope, $element: HTMLDivElement) {
                const vm: IViewModel = $scope.vm;
                vm.usersSearch = new UsersSearch(window.structure.id, SearchService);

                vm.selectTimeSlot = (hourPeriod: TimeSlotHourPeriod, detentionSlot: IDetentionSlot): void => {
                    PeriodFormUtils.setHourSelectorsFromTimeSlots(detentionSlot.date, hourPeriod, detentionSlot.timeSlotTimePeriod,
                        detentionSlot.detentionField, "start_at", "end_at");
                };

                vm.changeTimeInput = (detentionSlot: IDetentionSlot): void => {
                    detentionSlot.detentionField.start_at = DateUtils.getDateFormat(moment(detentionSlot.date), detentionSlot.start_time);
                    detentionSlot.detentionField.end_at = DateUtils.getDateFormat(moment(detentionSlot.date), detentionSlot.end_time);
                };

                vm.changeDateInput = (detentionSlot: IDetentionSlot): void => {
                    detentionSlot.detentionField.start_at = DateUtils.getDateFormat(moment(detentionSlot.date),
                        moment(detentionSlot.detentionField.start_at));
                    detentionSlot.detentionField.end_at = DateUtils.getDateFormat(moment(detentionSlot.date),
                        moment(detentionSlot.detentionField.end_at));
                };

                vm.deleteSlot = (detentionSlotToDelete: IDetentionSlot): void => {
                    (<Array<IPDetentionField>>vm.form.fields) = (<Array<IPDetentionField>>vm.form.fields).filter((slot: IPDetentionField) => slot != detentionSlotToDelete.detentionField);
                    vm.detentionSlots = vm.detentionSlots.filter((detentionSlot: IDetentionSlot) => detentionSlot != detentionSlotToDelete);
                    if (vm.editmode && detentionSlotToDelete.detentionField.id) {
                        punishmentService.delete(detentionSlotToDelete.detentionField.id, window.structure.id)
                            .then(() => toasts.confirm(lang.translate('incidents.punishment.delete.succeed')))
                            .catch((e: AxiosError) => toasts.warning(e.response ? e.response.data : e.message));
                    }
                }

                vm.getDisplayOwnerName = (): string => {
                    if (vm && vm.owner) {
                        return vm.owner.displayName || vm.owner.lastName + " " + vm.owner.firstName;
                    }
                    return "";
                };

                vm.searchOwner = async (value: string): Promise<void> => {
                    await vm.usersSearch.searchUsers(value);
                    $scope.$apply();
                };

                vm.selectOwner = (model, owner: User): void => {
                    vm.owner = owner;
                    vm.form.owner_id = owner.id;
                    vm.ownerSearch = '';
                    $scope.$apply();
                };

                vm.$onDestroy = () => {
                    vm.form = {} as IPunishmentBody;
                    vm.owner = null;
                    vm.ownerSearch = '';
                };
            }
        };
    }]);