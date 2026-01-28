import {idiom as lang, moment, ng, toasts} from 'entcore';
import {Student} from "@common/model/Student";
import {ISchoolYearPeriod, IStructureSlot, TimeSlotHourPeriod} from "@common/model";
import {IStatementAbsenceBody, IStatementsAbsences, IStatementsAbsencesRequest, StatementsAbsences} from "../models";
import {DateUtils} from "@common/utils";
import {AxiosResponse} from "axios";
import {
    IStatementsAbsencesService,
    IViescolaireService,
    statementsAbsencesService} from "../services";
import {UPDATE_STUDENTS_EVENTS} from "@common/core/enum/select-children-events";
import {IAngularEvent} from "angular";

declare let window: any;

interface IViewModel {
    $onInit(): any;

    title: string;
    student: Student;
    form: IStatementAbsenceBody;
    timeslots: IStructureSlot;
    statementsAbsences: StatementsAbsences;
    filter: IStatementsAbsencesRequest;
    date: { start_time: string, end_time: string, isComplete: boolean };
    timeSlotHourPeriod: typeof TimeSlotHourPeriod;
    timeoutInput: any;


    submit(): Promise<void>;

    uploadFile(): void;

    downloadStatementFile(statementAbsence: IStatementsAbsences): void;

    dateFormat(date: string): void;

    hourFormat(date: string): void;

    isFormValid(form: IStatementAbsenceBody): boolean;

    hourInput(hourPeriod: TimeSlotHourPeriod): void;

    selectTimeSlot(hourPeriod: TimeSlotHourPeriod): void
}

export const StatementsAbsenceForm = ng.directive('statementsAbsenceForm',
    ['$timeout', 'StatementsAbsencesService', 'ViescolaireService',
        function ($timeout, statementsAbsenceService: IStatementsAbsencesService, viescolaireService: IViescolaireService) {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            student: '=',
            timeslots: '='
        },
        template: `
            <div class="statements-absence-form">
                <div class="statements-absence-form-title">
                    <div class="statements-absence-form-title-content">
                        <span class="statements-absence-form-title-content-name">[[vm.student.displayName]]</span>
                        <span>[[vm.dateFormat(vm.student.birth)]]</span>
                    </div>
                     <img src="/userbook/avatar/[[vm.student.id]]" height="95" width="95">
                </div>
                <!-- form -->
                <form class="statements-absence-form-content">
                    <!-- title -->
                    <h2 class="statements-absence-form-content-title"><i18n>presences.statements.absence.form</i18n></h2>
                    <!-- set date form -->
                    <div class="margin-bottom-lg statements-absence-form-content-date">
                        <div class="margin-bottom-xmd">
                            <span><i18n>presences.from</i18n>&#58;</span>
                            <span class="card date-picker">
                                 <date-picker ng-model="vm.form.start_at" 
                                 ng-change="vm.selectTimeSlot(vm.timeSlotHourPeriod.START_HOUR)"></date-picker>
                            </span>
                        </div>
                        
                       <div>
                           <span><i18n>presences.to</i18n>&#58;</span>
                           <span class="card date-picker">
                               <date-picker ng-model="vm.form.end_at" 
                               ng-change="vm.selectTimeSlot(vm.timeSlotHourPeriod.END_HOUR)"></date-picker>
                           </span>
                       </div>
                    </div>
                     <!-- set checkbox toggling completed day form -->
                    <div class="margin-bottom-xmd">
                        <label class="checkbox">
                            <input type="checkbox" data-ng-model="vm.date.isComplete"/>
                            <span></span>
                        </label>
                        <i18n>presences.day.complete</i18n>
                    </div>
                    <!-- set date time form -->
                    <div class="margin-bottom-lg statements-absence-form-content-time">
                        <!-- start time -->
                        <div class="statements-absence-form-content-time-start">
                            <span><i18n>presences.as.of</i18n> &#58;</span>
                            <span class="top5 card card-timepicker">
                                <time-picker required ng-model="vm.date.start_time" 
                                ng-change="vm.hourInput(vm.timeSlotHourPeriod.START_HOUR)" 
                                ng-disabled="vm.date.isComplete"></time-picker>
                            </span>
                        </div>
        
                        <!-- end time -->
                        <div class="statements-absence-form-content-time-end">
                            <span><i18n>presences.until</i18n> &#58;</span>
                            <span class="top5 card card-timepicker">
                                <time-picker required ng-model="vm.date.end_time"
                                ng-change="vm.hourInput(vm.timeSlotHourPeriod.END_HOUR)"
                                ng-disabled="vm.date.isComplete"></time-picker>
                            </span>
                        </div>
                        <div></div>
                    </div>
                    
                    <!-- set reason/description -->
                    <div class="statements-absence-form-content-description">
                        <!-- title -->
                        <span><i18n>presences.absence.reason.this</i18n></span>
                        <!-- text area -->
                        <input i18n-placeholder="incidents.write.text" data-ng-model="vm.form.description">
                        <!-- box upload file -->
                        <div class="statements-absence-form-content-description-upload">
                            <div class="statements-absence-form-content-description-upload-box">
                                <input  type="file" 
                                        id="file" name="file" 
                                        onchange="angular.element(this).scope().vm.uploadFile()"
                                        accept="image/*,.pdf">
                                <label for="file">
                                    <i18n data-ng-show="!vm.form.file">presences.add.supporting.document</i18n>
                                    <span data-ng-show="vm.form.file">[[vm.form.file.name]]</span>
                                </label>
                            </div>
                        </div>
                    </div>
                    
                    <!-- valid form -->
                    <div class="margin-top-md statements-absence-form-content-valid">
                        <button data-ng-click="vm.submit()" data-ng-disabled="!vm.isFormValid(vm.form)">
                            <i18n>presences.exemptions.form.submit</i18n>
                        </button>
                    </div>
                </form>
                
                <!-- statements history -->
                <div class="statements-history" data-ng-show="vm.statementsAbsences.statementAbsenceResponse.all.length > 0">
                    
                    <h2 class="statements-history-title"><i18n>presences.statements.absence.form.history</i18n></h2>
                
                    <div class="statements-history-list">
                        <div class="statements-history-list-item" ng-repeat="statement in vm.statementsAbsences.statementAbsenceResponse.all">
                            <div class="statements-history-list-item-content">
                                <!-- student name/audience -->
                                <div class="statements-history-list-item-content-student">
                                    <span class="statements-history-list-item-content-student-name">
                                        [[statement.student.name]]
                                    </span>
                                    &nbsp;&nbsp;&nbsp;
                                    <span class="statements-history-list-item-content-student-audience">
                                        [[statement.student.className]]
                                    </span>
                                </div>
                                
                                <div class="statements-history-list-item-content-infos">
                                    <div class="statements-history-list-item-content-infos-absence">
                                        <!-- absence -->
                                        <div class="statements-history-list-item-content-infos-absence-line">
                                            <span class="statements-history-list-item-content-infos-absence-line-title">
                                                <i18n>presences.absence</i18n>
                                            </span>
                                            <div class="statements-history-list-item-content-infos-absence-line-dates">
                                                 <span><i18n>presences.from</i18n>&nbsp;[[vm.dateFormat(statement.start_at)]]
                                                        <i18n>presences.to</i18n>&nbsp;[[vm.dateFormat(statement.end_at)]]
                                                 </span> 
                                                 <br>
                                                 <span> <i18n>presences.by</i18n>&nbsp;[[vm.hourFormat(statement.start_at)]] 
                                                        <i18n>presences.at</i18n>&nbsp;[[vm.hourFormat(statement.end_at)]]
                                                 </span>
                                            </div>
                                        </div>
                                        <!-- reason -->
                                        <div class="statements-history-list-item-content-infos-absence-line">
                                            <span class="statements-history-list-item-content-infos-absence-line-title"><i18n>presences.absence.reason</i18n></span>
                                            <div class="statements-history-list-item-content-infos-absence-line-description">
                                                 <span>[[statement.description]]</span>
                                            </div>
                                        </div>
                                        
                                    </div>
                                     <!-- attachment -->
                                    <div class="statements-history-list-item-content-infos-download">
                                        <i class="attach"
                                           data-ng-click="vm.downloadStatementFile(statement)"
                                           data-ng-show="statement.attachment_id"></i>
                                    </div> 
                                </div>
                            </div>
                    </div>
                   
                  </div>
                    
                    
                </div>
                
                <!-- Empty state -->
                <div class="nine empty-content-vertical" 
                     data-ng-show="vm.statementsAbsences.statementAbsenceResponse.all.length === 0">
                    <div class="description">
                        <span class="red-bar bar"></span>
                            <i18n>presences.presences.empty.state</i18n>
                        <span class="yellow-bar bar"></span>
                    </div>
                </div>
          
                </div>
        
            </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        controller: function ($scope) {
            const vm: IViewModel = <IViewModel>this;
            vm.$onInit = () => {
                vm.timeSlotHourPeriod = TimeSlotHourPeriod;
            };

            vm.form = {
                start_at: moment().startOf('day').add(8, "hours"),
                end_at: moment().startOf('day').add(17, 'hours'),
                description: "",
                file: null,
            };
            vm.date = {
                start_time: moment(vm.form.start_at).set({second: 0, millisecond: 0}).toDate(),
                end_time: moment(vm.form.end_at).set({second: 0, millisecond: 0}).toDate(),
                isComplete: false
            };
        },
        link: function ($scope, $element: HTMLDivElement) {
            const vm: IViewModel = $scope.vm;

            vm.uploadFile = (): void => {
                const file: File = document.getElementById('file')['files'][0];
                if (file) {
                    vm.form.file = file;
                    $scope.$apply();
                }
            };

            vm.submit = async (): Promise<void> => {
                let date: { start: string, end: string };
                date = setCorrectDateFormat();
                let form: IStatementAbsenceBody = JSON.parse(JSON.stringify(vm.form));
                form.student_id = vm.student.id;
                form.file = vm.form.file;
                form.structure_id = vm.student.structure.id;
                form.start_at = date.start;
                form.end_at = date.end;
                if (!vm.isFormValid(form)) {
                    toasts.warning(lang.translate('presences.invalid.form'));
                    return;
                }
                let response: AxiosResponse = await statementsAbsencesService.create(form);
                if (response.status == 200 || response.status == 201) {
                    toasts.confirm(lang.translate('presences.statement.form.create.success'));
                    vm.form.description = "";
                    vm.form.file = null;
                    await loadStatements();
                    $scope.$apply();
                } else {
                    toasts.warning('presences.statement.form.create.error');
                }
            };

            vm.downloadStatementFile = (statementAbsence: IStatementsAbsences): void => {
                statementsAbsencesService.download(statementAbsence);
            };

            vm.dateFormat = (date: string): string => {
                return DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
            };

            vm.hourFormat = (date: string): string => {
                return DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);
            };

            vm.isFormValid = (form: IStatementAbsenceBody): boolean => {
                let current_date: string = moment().startOf('day');
                return DateUtils.isPeriodValid(form.start_at, form.end_at)
                    && DateUtils.isPeriodValid(current_date, form.start_at)
                    && DateUtils.isPeriodValid(current_date, form.end_at)
            };

            vm.hourInput = (hourPeriod: TimeSlotHourPeriod): void => {
                if (vm.timeoutInput) $timeout.cancel(vm.timeoutInput);
                vm.timeoutInput = $timeout(() => vm.selectTimeSlot(hourPeriod), 600);
            };


            vm.selectTimeSlot = (hourPeriod: TimeSlotHourPeriod): void => {
                vm.form.start_at = moment(DateUtils.getDateFormat(new Date(vm.form.start_at), new Date(vm.date.start_time)));
                vm.form.end_at = moment(DateUtils.getDateFormat(new Date(vm.form.end_at), new Date(vm.date.end_time)));

                let startHour: Date = vm.date.start_time ? new Date(vm.date.start_time) : null;
                let endHour: Date = vm.date.end_time ? new Date(vm.date.end_time) : null;
                let start: string = vm.form.start_at && startHour ? DateUtils.getDateFormat(new Date(vm.form.start_at), startHour) : null;
                let end: string = vm.form.end_at && endHour ? DateUtils.getDateFormat(new Date(vm.form.end_at), endHour) : null;

                switch (hourPeriod) {
                    case TimeSlotHourPeriod.START_HOUR:
                        if (startHour == null) return;
                        if (!end || !endHour || !DateUtils.isPeriodValid(start, end)) {
                            vm.date.end_time = moment(vm.date.start_time).add(1, 'hours').toDate();
                            vm.form.end_at = moment(DateUtils.getDateFormat(new Date(vm.form.start_at), new Date(vm.date.end_time)));
                        }
                        break;
                    case TimeSlotHourPeriod.END_HOUR:
                        if (endHour == null) return;
                        if (!start || !startHour || !DateUtils.isPeriodValid(start, end)) {
                            vm.date.start_time = moment(vm.date.end_time).add(-1, 'hours').toDate();
                            vm.form.start_at = moment(DateUtils.getDateFormat(new Date(vm.form.end_at), new Date(vm.date.start_time)));
                        }
                        break;
                    default:
                        return;
                }
            };


            const setCorrectDateFormat = (): { start: string, end: string } => {
                const start_date: Date = moment(vm.form.start_at).toDate();
                const end_date: Date = moment(vm.form.end_at).toDate();

                if (!vm.date.isComplete) {
                    return setDefaultDate(start_date, end_date);
                } else {
                    if (!vm.timeslots) {
                        return setDefaultDate(start_date, end_date);
                    } else {
                        return setTimeSlotDate(start_date, end_date);
                    }
                }
            };

            const setDefaultDate = (start_date: Date, end_date: Date): { start: string, end: string } => {
                const start_time: Date = moment(vm.date.start_time).set({second: 0, millisecond: 0}).toDate();
                const end_time: Date = moment(vm.date.end_time).set({second: 0, millisecond: 0}).toDate();

                const start: string = DateUtils.format(DateUtils.getDateFormat(start_date, start_time),
                    DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);

                const end: string = DateUtils.format(DateUtils.getDateFormat(end_date, end_time),
                    DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);

                return {start, end};
            };

            const setTimeSlotDate = (start_date: Date, end_date: Date): { start: string, end: string } => {
                const start_time: string = vm.timeslots.slots[0].startHour;
                const end_time: string = (vm.timeslots.slots.length > 0) ?
                    vm.timeslots.slots[vm.timeslots.slots.length - 1].endHour :
                    vm.timeslots.slots[vm.timeslots.slots.length].endHour;

                const start: string = DateUtils.format(DateUtils.getDateFormat(start_date, DateUtils.getTimeFormatDate(start_time)),
                    DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);

                const end: string = DateUtils.format(DateUtils.getDateFormat(end_date, DateUtils.getTimeFormatDate(end_time)),
                    DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);

                return {start, end};
            };

            const loadStatements = async (): Promise<void> => {

                if (!window.structure) return;

                try {
                    const schoolYears: ISchoolYearPeriod = await viescolaireService.getSchoolYearDates(window.structure.id);

                    vm.statementsAbsences = new StatementsAbsences(window.structure.id);
                    vm.filter = {
                        structure_id: vm.statementsAbsences.structure_id,
                        start_at: DateUtils.format(moment(schoolYears.start_date), DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                        end_at: DateUtils.format(DateUtils.setLastTime(moment(schoolYears.end_date)),
                            DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                        student_ids: [vm.student.id],
                        isTreated: true
                    }
                    vm.statementsAbsences.build(await statementsAbsenceService.get(vm.filter));
                } catch (err) {
                    throw err;
                }

                $scope.$apply();
            }

            $scope.$watch(() => vm.student, async () => {
                await loadStatements();
            });
        }
    };
}]);