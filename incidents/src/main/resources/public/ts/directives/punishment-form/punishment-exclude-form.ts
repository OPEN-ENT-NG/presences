import {idiom, model, moment, ng} from 'entcore';
import {IPExcludeField, IPunishment, IPunishmentAbsence, IPunishmentBody, Student} from "@incidents/models";
import {Reason} from '@presences/models/Reason';
import {IAbsence} from '@presences/models/Event/Absence';
import {DateUtils, UsersSearch} from "@common/utils";
import {User} from "@common/model/User";
import {SearchService} from "@common/services";
import {presenceService} from "@incidents/services/PresenceService";
import {punishmentService} from "@incidents/services";
import {Idiom} from "@common/interfaces";

declare let window: any;

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    form: IPunishmentBody;
    punishment: IPunishment;
    students: Array<Student>;
    usersSearch: UsersSearch;
    owner: User;
    ownerSearch: string;
    start_date: string;
    end_date: string;
    oldStartAt: string;
    oldEndAt: string;
    isAddingAbsence: boolean;
    reasons: Array<Reason>;
    absencesByStudentIds: Map<string, Array<IAbsence>>;
    lang: Idiom;

    formatStartDate(): void;

    formatEndDate(): void;

    searchOwner(value: string): Promise<void>;

    selectOwner(model, owner: User): void;

    getDisplayOwnerName(): string;

    addAbsence(): Promise<void>;

    isAbsenceMatchingExcludedDates(absence: IAbsence, startAt: string, endAt: string): boolean;

    getStudentsAbsences(): Promise<void>;

    getUpdateMatchingAbsence(studentId: string): IAbsence;

    getAnomalyStudents(): Array<Student>;

    isStudentAnomaly(studentId: string): boolean;

    disableAbsence(): boolean;
}

export const PunishmentExcludeForm = ng.directive('punishmentExcludeForm', ['SearchService',
    (SearchService: SearchService) => {
        return {
            restrict: 'E',
            transclude: true,
            scope: {
                form: '=',
                punishment: '=',
                students: '='
            },
            template: `
         <div class="punishment-exclude-form">
             <!-- Date -->
             <div class="punishment-exclude-form-date twelve cell">
                <i18n>presences.from</i18n>&nbsp;&#58;&nbsp;
                <span class="card date-picker"><date-picker ng-model="vm.start_date"></date-picker></span>

                <i18n>presences.to</i18n>&nbsp;&#58;&nbsp;
                <span class="card date-picker"><date-picker ng-model="vm.end_date"></date-picker></span>
             </div>
           
            <!-- mandatory -->
            <div class="punishment-exclude-form-mandatory">
                <i18n>incidents.presences.mandatory.inside.structure</i18n>&nbsp;
                 <switch ng-model="vm.form.fields.mandatory_presence">
                    <label class="switch"></label>
                 </switch>
            </div>
            
            <!-- absence -->
            <div class="punishment-exclude-form-absence" ng-class="{'add-absence': vm.isAddingAbsence}">
                <i18n>incidents.punishment.declare.absence</i18n>&nbsp;
                <!-- !!! tricks used because of the ng-disabled in directive is a string props, and so always true -->
                <switch ng-show="vm.disableAbsence()" ng-model="vm.isAddingAbsence" ng-change="vm.addAbsence()" ng-disabled="true">
                    <label class="switch"></label>
                 </switch>
                 <switch ng-show="!vm.disableAbsence()" ng-model="vm.isAddingAbsence" ng-change="vm.addAbsence()">
                    <label class="switch"></label>
                 </switch>
            </div>
            
            <div class="punishment-exclude-form-absence-anomaly" ng-show="vm.getAnomalyStudents().length > 0 && (vm.punishment.id || vm.isAddingAbsence)">
                    [[vm.lang.translate('incidents.punishment.declare.absence.anomaly')]]: 
                    <span ng-repeat="student in vm.getAnomalyStudents()">[[(student.displayName || student.name) + (!$last ? ', ' : '')]]</span>
            </div>
            
            <!-- absence reason -->
            <div class="punishment-exclude-form-absence-reason" ng-show="vm.isAddingAbsence">
                <select data-ng-model="vm.form.absence.reason_id"
                        data-ng-change="vm.selectReason()"
                        ng-options="reason.id as reason.label for reason in vm.reasons"
                        options-disabled="reason.hidden for reason in vm.reasons">
                    <option value="">[[vm.lang.translate('incidents.absence.select.empty')]]</option>
                </select>
                
                <!-- follow event area -->
                <label class="checkbox punishment-exclude-form-absence-reason-follow">
                    <input type="checkbox"
                       data-ng-model="vm.form.absence.followed"/>
                    <span class="presenceLightbox-body-info-checkbox">
                        <i18n>incidents.punishment.declare.absence.followed</i18n>
                    </span>
                </label>
               
            </div>
            
            <!-- responsible -->
            <label class="twelve cell twelve-mobile">
                <div class="two cell twelve-mobile">
                    <i18n>presences.responsible</i18n>:
                </div>
                <div class="seven cell twelve-mobile">
                    <div class="incident-lightbox-body-responsible-autocomplete search-input">
                        <async-autocomplete data-ng-disabled="false"
                                        data-ng-model="vm.ownerSearch"
                                        data-ng-change="vm.selectOwner"
                                        data-on-search="vm.searchOwner"
                                        data-options="vm.usersSearch.users"
                                        data-placeholder="incidents.search.personal"
                                        data-search="vm.ownerSearch">
                        </async-autocomplete>
                    </div>
                    <div ng-show="vm.owner" class="margin-top-sm">
                        <span class="font-bold">[[vm.getDisplayOwnerName()]]</span>
                    </div>
                </div>
            </label>
        </div>
        `,
            controllerAs: 'vm',
            bindToController: true,
            replace: true,
            controller: function () {
                const vm: IViewModel = <IViewModel>this;
                vm.$onInit = () => {
                    vm.lang = idiom;
                    vm.isAddingAbsence = false;
                    if (!vm.punishment || !vm.punishment.id) {
                        vm.form.owner_id = model.me.userId;
                        vm.start_date = moment().startOf('day');
                        vm.end_date = moment().endOf('day');
                        vm.form.fields = {
                            start_date: DateUtils.format(vm.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                            end_date: DateUtils.format(vm.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                            mandatory_presence: false,
                        } as IPExcludeField;
                        vm.owner = model.me;
                    } else {
                        vm.form.owner_id = vm.punishment.owner.id;
                        vm.form.fields = vm.punishment.fields;
                        if (!(Object.keys(vm.form.fields).length > 0)) {
                            vm.form.fields = {
                                start_date: DateUtils.format(vm.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                                end_date: DateUtils.format(vm.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                                mandatory_presence: false,
                            } as IPExcludeField;
                        }
                        vm.start_date = moment((<IPExcludeField>vm.form.fields).start_at).startOf('day');
                        vm.end_date = moment((<IPExcludeField>vm.form.fields).end_at).endOf('day');
                        vm.owner = vm.punishment.owner;
                    }

                    vm.oldStartAt = moment(vm.start_date);
                    vm.oldEndAt = vm.end_date;
                };
            },
            link: function ($scope, $element: HTMLDivElement) {
                const vm: IViewModel = $scope.vm;
                vm.usersSearch = new UsersSearch(window.structure.id, SearchService);

                vm.formatStartDate = (): void => {
                    if (vm.form && vm.form.fields) {
                        (<IPExcludeField>vm.form.fields).start_at =
                            DateUtils.format(moment(vm.start_date).startOf('day'), DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                    }
                };

                vm.formatEndDate = (): void => {
                    if (vm.form && vm.form.fields) (<IPExcludeField>vm.form.fields).end_at =
                        DateUtils.format(moment(vm.end_date).endOf('day'), DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                };

                vm.getDisplayOwnerName = (): string => {
                    if (vm && vm.owner) return vm.owner.displayName || vm.owner.lastName + " " + vm.owner.firstName;
                    return "";
                };

                vm.searchOwner = async (value: string): Promise<void> => {
                    await vm.usersSearch.searchUsers(value);
                    $scope.$apply();
                };

                vm.selectOwner = function (model, owner: User): void {
                    vm.owner = owner;
                    vm.form.owner_id = owner.id;
                    vm.ownerSearch = '';
                    $scope.$apply();
                };

                vm.addAbsence = async function (): Promise<void> {
                    vm.form.absence = null;
                    if (vm.isAddingAbsence || vm.punishment.id) {
                        vm.form.absence = {reason_id: null, followed: true} as IPunishmentAbsence
                        if (vm.isAddingAbsence && !vm.punishment.id) await vm.getStudentsAbsences();
                        if (vm.punishment.id && vm.punishment.student && vm.absencesByStudentIds && vm.start_date && vm.end_date) {
                            if (vm.isStudentAnomaly(vm.punishment.student.id)) {
                                vm.isAddingAbsence = false; // we can't check because of the absence not matching, so we force it to keep unchecked
                                vm.form.absence = null;
                            } else {
                                let absence: IAbsence = vm.getUpdateMatchingAbsence(vm.punishment.student.id)
                                if (absence) {
                                    vm.isAddingAbsence = true; // if we found absence matching, we can't uncheck, so we force it to keep checked
                                    vm.form.absence.reason_id = absence.reason_id;
                                    vm.form.absence.followed = absence.followed;
                                }
                            }
                        }
                    }
                }

                vm.isAbsenceMatchingExcludedDates = function (absence: any, startAt: string, endAt: string): boolean {
                    return moment(absence.start_date).isSame(startAt, 'second') && moment(absence.end_date).isSame(endAt, 'second')
                }

                vm.getAnomalyStudents = function (): Array<Student> {
                    if (vm.punishment.id) {
                        let absences: Array<IAbsence> = vm.punishment.student && vm.absencesByStudentIds ?
                            vm.absencesByStudentIds[vm.punishment.student.id] : [];
                            // get element when get more than 1 absence or absences dates are not matching initial excluded dates
                        if(absences.length > 1 || (absences[0] && !vm.isAbsenceMatchingExcludedDates(absences[0], vm.oldStartAt, vm.oldEndAt))) {
                            return [vm.punishment.student];
                        }
                        return [];
                    }

                    return vm.students && vm.absencesByStudentIds ? vm.students.filter((student: Student) => {
                        let studentAbsences: Array<IAbsence> = vm.absencesByStudentIds[student.id];
                        return studentAbsences && studentAbsences.length > 0;
                    }) : [];
                };

                vm.isStudentAnomaly = function (studentId: string): boolean {
                    return vm.getAnomalyStudents().map((student: Student) => student.id).indexOf(studentId) >= 0;
                }

                vm.getStudentsAbsences = async function (): Promise<void> {
                    if (vm.start_date && vm.end_date) {
                        if (vm.punishment && vm.punishment.id && vm.punishment.student) {
                            if (!vm.getUpdateMatchingAbsence(vm.punishment.student.id)) {
                                // we only update list in case that student have no absence matching this period.
                                vm.absencesByStudentIds = await punishmentService.getStudentsAbsences([vm.punishment.student], vm.start_date, vm.end_date)
                            }
                        }
                        else if (vm.isAddingAbsence && vm.students && vm.students.length > 0)
                            vm.absencesByStudentIds = await punishmentService.getStudentsAbsences(vm.students, vm.start_date, vm.end_date)
                    }
                    $scope.$apply();
                };

                vm.getUpdateMatchingAbsence = function (studentId: string): IAbsence {
                    return vm.absencesByStudentIds && vm.absencesByStudentIds[studentId] ? vm.absencesByStudentIds[studentId]
                        .find((absence: any) => vm.isAbsenceMatchingExcludedDates(absence, vm.oldStartAt, vm.oldEndAt)) : null;
                }

                vm.disableAbsence = function (): boolean {
                    return !!vm.punishment.id && (vm.isStudentAnomaly(vm.punishment.student.id) || !!vm.getUpdateMatchingAbsence(vm.punishment.student.id));
                }

                $scope.$watch(() => vm.start_date, async () => {
                    vm.formatStartDate();
                    await vm.getStudentsAbsences();
                    $scope.$apply();
                });

                $scope.$watch(() => vm.end_date, async () => {
                    vm.formatEndDate();
                    await vm.getStudentsAbsences();
                    $scope.$apply();
                });

                $scope.$watchCollection(() => vm.students, async () => {
                    await vm.getStudentsAbsences();
                    $scope.$apply();
                });

                // tricks used to initialize data from promises (played only one time)
                const unwatch = $scope.$watchCollection(() => vm.punishment, async function(newVal, oldVal){
                    if (newVal) {
                        vm.reasons = await presenceService.getReasons(window.structure.id);
                        if (vm.punishment.id) {
                            await vm.getStudentsAbsences();
                            await vm.addAbsence();
                        }
                        $scope.$apply();
                        unwatch();
                    }
                });

                vm.$onDestroy = () => {
                    vm.form = {} as IPunishmentBody;
                    vm.owner = null;
                    vm.ownerSearch = '';
                    vm.absencesByStudentIds = null;
                };
            }
        };
    }]);