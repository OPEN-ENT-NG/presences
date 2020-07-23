import {idiom as lang, moment, ng, toasts} from 'entcore';
import {Student} from "@common/model/Student";
import {IStructureSlot} from "@common/model";
import {IStatementAbsenceBody} from "../models";
import {DateUtils} from "@common/utils";
import {AxiosResponse} from "axios";
import {statementsAbsencesService} from "../services";

declare let window: any;

interface IViewModel {
    title: string;
    student: Student;
    form: IStatementAbsenceBody;
    timeslots: IStructureSlot;
    date: { start_time: string, end_time: string, isComplete: boolean };

    submit(): Promise<void>;

    uploadFile(): void;

    dateFormat(date: string): void;

    isFormValid(): boolean;
}

export const StatementsAbsenceForm = ng.directive('statementsAbsenceForm', () => {
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
                                 <date-picker ng-model="vm.form.start_at"></date-picker>
                            </span>
                        </div>
                        
                       <div>
                           <span><i18n>presences.to</i18n>&#58;</span>
                           <span class="card date-picker">
                               <date-picker ng-model="vm.form.end_at"></date-picker>
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
                                <time-picker required ng-model="vm.date.start_time" ng-disabled="vm.date.isComplete"></time-picker>
                            </span>
                        </div>
        
                        <!-- end time -->
                        <div class="statements-absence-form-content-time-end">
                            <span><i18n>presences.until</i18n> &#58;</span>
                            <span class="top5 card card-timepicker">
                                <time-picker required ng-model="vm.date.end_time" ng-disabled="vm.date.isComplete"></time-picker>
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
                        <button data-ng-click="vm.submit()" data-ng-disabled="!vm.isFormValid()">
                            <i18n>presences.exemptions.form.submit</i18n>
                        </button>
                    </div>

                </form>
            </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        controller: function () {
            const vm: IViewModel = <IViewModel>this;
            vm.form = {
                start_at: moment().startOf('day'),
                end_at: moment().endOf('day'),
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
                form.structure_id = window.structure.id;
                form.start_at = date.start;
                form.end_at = date.end;
                let response: AxiosResponse = await statementsAbsencesService.create(form);
                if (response.status == 200 || response.status == 201) {
                    toasts.confirm(lang.translate('presences.statement.form.create.success'));
                } else {
                    toasts.warning('presences.statement.form.create.error');
                }
            };

            vm.dateFormat = (date: string): string => {
                return DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"])
            };

            vm.isFormValid = (): boolean => {
                return vm.form.start_at
                    && vm.form.end_at
                    && vm.form.start_at <= vm.form.end_at;
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
        }
    };
});