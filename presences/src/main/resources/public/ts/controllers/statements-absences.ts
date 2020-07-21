import {moment, ng, toasts} from 'entcore';
import {DateUtils, StudentsSearch} from "@common/utils";
import {SearchService} from "@common/services/SearchService";
import {IStatementAbsenceBody, IStatementsAbsences, IStatementsAbsencesRequest, StatementsAbsences} from "../models";
import {IStatementsAbsencesService} from "../services";

declare let window: any;

interface IFilter {
    start_at: string;
    end_at: string;
    student_ids: Array<string>;
    isTreated: boolean;
    page: number;
}

interface IViewModel {
    filter: IFilter;
    studentsSearch: StudentsSearch;
    statementsAbsencesRequest: IStatementsAbsencesRequest;
    statementsAbsences: StatementsAbsences;

    updateFilter(): Promise<void>;

    changePagination(): Promise<void>;

    formatHour(date: string): string;

    formatDayDate(date: string): string;

    toggleTreated(): void;

    redirectCalendar($event, event): void;

    updateTreatStatement(statementAbsence: IStatementsAbsences): Promise<void>;

    /* search bar methods */
    searchStudent(studentForm: string): Promise<void>;

    selectStudent(valueInput, studentItem): void;

    removeSelectedStudents(studentItem): void;
}

export const statementsAbsencesController = ng.controller('StatementsAbsencesController',
    ['$scope', 'route', '$location', 'SearchService', 'StatementsAbsencesService',
        function ($scope, route, $location, searchService: SearchService, statementAbsenceService: IStatementsAbsencesService) {

            const vm: IViewModel = this;

            /* Init filter */
            vm.filter = {
                start_at: moment().add(-1, 'M').startOf('day'),
                end_at: moment().endOf('day'),
                student_ids: [],
                isTreated: false,
                page: 0
            };

            /* Init search bar */
            vm.studentsSearch = undefined;

            vm.statementsAbsences = undefined;
            vm.statementsAbsencesRequest = {} as IStatementsAbsencesRequest;

            const load = (): void => {
                vm.statementsAbsences = new StatementsAbsences(window.structure.id);

                /* Init search bar */
                vm.studentsSearch = new StudentsSearch(window.structure.id, searchService);

                /* event */
                vm.statementsAbsences.eventer.on('loading::true', () => $scope.safeApply());
                vm.statementsAbsences.eventer.on('loading::false', () => $scope.safeApply());
                getStatementsAbsences();
            };

            const getStatementsAbsences = async (): Promise<void> => {
                vm.statementsAbsences.loading = true;
                prepareRequest();
                await vm.statementsAbsences.build(await statementAbsenceService.get(vm.statementsAbsencesRequest));
                // todo replace mockup by real get
                // await vm.statementsAbsences.build(mockupStatement);
                vm.statementsAbsences.loading = false;
            };

            const prepareRequest = (): void => {
                vm.statementsAbsencesRequest.structure_id = vm.statementsAbsences.structure_id;
                vm.statementsAbsencesRequest.start_at = DateUtils.format(vm.filter.start_at, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.statementsAbsencesRequest.end_at = DateUtils.format(vm.filter.end_at, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.statementsAbsencesRequest.student_ids = vm.filter.student_ids;
                vm.statementsAbsencesRequest.page = vm.filter.page;
            };

            vm.updateFilter = async (): Promise<void> => {
                /* Retrieving our search bar info */
                if (vm.studentsSearch != undefined && vm.studentsSearch) {
                    vm.filter.student_ids = vm.studentsSearch.getSelectedStudents().map(student => student["id"]);
                    await getStatementsAbsences();
                }
            };

            vm.changePagination = async (): Promise<void> => {
                vm.filter.page = vm.statementsAbsences.statementAbsenceResponse.page;
                await getStatementsAbsences();
            };

            vm.formatHour = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);
            vm.formatDayDate = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);

            vm.toggleTreated = (): void => {
                vm.filter.isTreated = !vm.filter.isTreated;
                vm.updateFilter();
            };

            vm.updateTreatStatement = async (statementAbsence: IStatementsAbsences): Promise<void> => {
                statementAbsence.treated = statementAbsence.isTreated ?
                    DateUtils.format(moment(), DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]) : null;

                let form = {
                    treated: statementAbsence.treated
                } as IStatementAbsenceBody;

                let response = await statementAbsenceService.update(statementAbsence.id, form);
                if (response.status == 200 || response.status == 201) {
                    if (statementAbsence.isTreated) {
                        toasts.confirm('presences.statements.treated.true');
                    } else {
                        toasts.confirm('presences.statements.treated.false');
                    }
                } else {
                    toasts.warning(response.data.toString());
                }
                $scope.safeApply();
            };

            vm.redirectCalendar = ($event, {studentId, date, displayName, className, classId}): void => {
                $event.stopPropagation();
                window.item = {
                    id: studentId,
                    date,
                    displayName,
                    type: 'USER',
                    groupName: className,
                    groupId: classId,
                    toString: function () {
                        return this.displayName;
                    }
                };
                $location.path(`/calendar/${studentId}?date=${date}`);
                $scope.safeApply();
            };

            /**
             * ⚠ Autocomplete classes/methods for students
             */

            /* Search bar student section */
            vm.searchStudent = async (studentForm: string): Promise<void> => {
                await vm.studentsSearch.searchStudents(studentForm);
                $scope.safeApply();
            };

            vm.selectStudent = (valueInput, studentItem): void => {
                vm.studentsSearch.selectStudents(valueInput, studentItem);
                vm.filter.student_ids = vm.studentsSearch.getSelectedStudents().map(student => student["id"]);
                vm.studentsSearch.student = "";
                vm.updateFilter();
            };

            vm.removeSelectedStudents = (studentItem): void => {
                vm.studentsSearch.removeSelectedStudents(studentItem);
                vm.filter.student_ids = vm.studentsSearch.getSelectedStudents().map(student => student["id"]);
                vm.updateFilter();
            };

            /* handling filter date event*/
            $scope.$watch(() => vm.filter.start_at, async () => vm.updateFilter());
            $scope.$watch(() => vm.filter.end_at, async () => vm.updateFilter());

            /* on  (watch) */
            $scope.$watch(() => window.structure, () => {
                if ('structure' in window) {
                    load();
                }
            });

        }]);