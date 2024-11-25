import {moment, ng, toasts} from 'entcore';
import {DateUtils, StudentsSearch} from "@common/utils";
import {SearchService} from "@common/services/SearchService";
import {
    ISchoolYearPeriod,
    IStatementAbsenceBody,
    IStatementsAbsences,
    IStatementsAbsencesRequest,
    StatementsAbsences
} from "../models";
import {IStatementsAbsencesService} from "../services";
import {IViescolaireService, ViescolaireService} from "@common/services/ViescolaireService";

declare let window: any;

interface IFilter {
    start_at: Date;
    end_at: Date;
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

    redirectCalendar($event, statementAbsence: IStatementsAbsences): void;

    updateTreatStatement(statementAbsence: IStatementsAbsences): Promise<void>;

    downloadFile(statementAbsence: IStatementAbsenceBody): void

    /* search bar methods */
    searchStudent(studentForm: string): Promise<void>;

    selectStudent(valueInput, studentItem): void;

    removeSelectedStudents(studentItem): void;

    getSchoolYear(): void;

    exportCsv(): void;
}

export const statementsAbsencesController = ng.controller('StatementsAbsencesController',
    ['$scope', 'route', '$location', 'SearchService', 'StatementsAbsencesService', 'ViescolaireService',
        function ($scope, route, $location, searchService: SearchService,
                  statementAbsenceService: IStatementsAbsencesService, viescolaireService: IViescolaireService) {

            const vm: IViewModel = this;

            /* Init filter */
            vm.filter = {
                start_at: DateUtils.add(new Date(), -7, 'd'),
                end_at: moment().endOf('day'),
                student_ids: [],
                isTreated: false,
                page: 0
            };

            /* Init search bar */
            vm.studentsSearch = undefined;

            vm.statementsAbsences = undefined;
            vm.statementsAbsencesRequest = {} as IStatementsAbsencesRequest;

            const load = async (): Promise<void> => {
                vm.statementsAbsences = new StatementsAbsences(window.structure.id);

                /* Init search bar */
                vm.studentsSearch = new StudentsSearch(window.structure.id, searchService);

                /* event */
                vm.statementsAbsences.eventer.on('loading::true', () => $scope.safeApply());
                vm.statementsAbsences.eventer.on('loading::false', () => $scope.safeApply());

                /* Init filter end_date */
                getSchoolYear();

                getStatementsAbsences();
            };

            const getSchoolYear = async (): Promise<void> => {
                const schoolYears: ISchoolYearPeriod = await viescolaireService.getSchoolYearDates(window.structure.id);
                vm.filter.end_at = moment(schoolYears.end_date);
            };

            const getStatementsAbsences = async (): Promise<void> => {
                vm.statementsAbsences.loading = true;
                prepareRequest();
                vm.statementsAbsences.build(await statementAbsenceService.get(vm.statementsAbsencesRequest));
                vm.statementsAbsences.loading = false;
            };

            const prepareRequest = (): void => {
                vm.statementsAbsencesRequest.structure_id = vm.statementsAbsences.structure_id;
                vm.statementsAbsencesRequest.start_at = DateUtils.format(vm.filter.start_at, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.statementsAbsencesRequest.end_at = DateUtils.format(DateUtils.setLastTime(vm.filter.end_at), DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                vm.statementsAbsencesRequest.student_ids = vm.filter.student_ids;
                vm.statementsAbsencesRequest.isTreated = vm.filter.isTreated;
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
                let form = {
                    isTreated: statementAbsence.isTreated,
                } as IStatementAbsenceBody;
                let response = await statementAbsenceService.validate(statementAbsence.id, form);
                if (response.status == 200 || response.status == 201) {
                    if (statementAbsence.isTreated) {
                        toasts.confirm('presences.statements.treated.true');
                    } else {
                        toasts.confirm('presences.statements.treated.false');
                    }
                    vm.updateFilter();
                } else {
                    toasts.warning(response.data.toString());
                }
                $scope.safeApply();
            };

            vm.downloadFile = (statementAbsence: IStatementsAbsences): void => {
                statementAbsenceService.download(statementAbsence);
            };

            vm.redirectCalendar = ($event, statementsAbsence: IStatementsAbsences): void => {
                $event.stopPropagation();
                const date: string = DateUtils.format(statementsAbsence.start_at, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                window.item = {
                    id: statementsAbsence.student.id,
                    date: date,
                    displayName: statementsAbsence.student.name,
                    type: 'USER',
                    groupName: statementsAbsence.student.className,
                    toString: function () {
                        return statementsAbsence.student.name;
                    }
                };
                $location.path(`/calendar/${statementsAbsence.student.id}?date=${date}`);
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

            /* CSV  */
            vm.exportCsv = (): void => {
                if (vm.statementsAbsences.statementAbsenceResponse.page_count < 50 && vm.statementsAbsences.statementAbsenceResponse.all.length > 0) {
                    window.open(statementAbsenceService.export(vm.statementsAbsencesRequest));
                    return;
                }

                if (vm.statementsAbsences.statementAbsenceResponse.page_count > 50)
                    toasts.warning('incidents.csv.full');
                else
                    toasts.warning('incidents.csv.empty');
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
