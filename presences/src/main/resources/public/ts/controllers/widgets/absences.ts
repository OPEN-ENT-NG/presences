import {_, moment, ng, notify} from 'entcore';
import {AbsenceService, ReasonService, SearchItem, SearchService} from '../../services';
import {CounsellorAbsence, Reason, Students} from '../../models';
import {DateUtils} from '@common/utils/date';

interface ViewModel {
    absences: Array<CounsellorAbsence>
    reasons: Array<Reason>
    students: Students
    searchResults: SearchItem[];
    params: {
        start: Date,
        end: Date,
        students: Array<SearchItem>,
        groups: Array<SearchItem>,
        search: string
    },
    provingReasonMap: any

    load(): Promise<void>

    searchStudentOrGroup(string: string): Promise<void>

    selectStudentOrGroup(model: SearchItem, option: SearchItem): void

    removeStudent(student : SearchItem): void

    removeGroup(group : SearchItem): void

    setAbsenceRegularisation(absence: CounsellorAbsence): void

    regularizeAbsence(absence): void

    showAbsenceRange(absence: CounsellorAbsence): String
}

declare let window: any;

export const absencesController = ng.controller('AbsenceController', ['$scope', 'AbsenceService', 'ReasonService', 'SearchService',
    function ($scope, AbsenceService: AbsenceService, ReasonService: ReasonService, SearchService: SearchService) {
        const vm: ViewModel = this;
        vm.absences = [];
        vm.reasons = [];
        vm.provingReasonMap = {};
        vm.params = {
            start: moment().add(-5, 'days').toDate(),
            end: new Date(),
            students: [],
            groups: [],
            search: null
        };

        vm.students = new Students();
        vm.searchResults = [];

        /**
         * Load all absences.
         */
        vm.load = async (): Promise<void> => {
            try {
                let start: string = moment(vm.params.start).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                let end: string = moment(vm.params.end).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                let students: string[] = [];
                let groups: string[] = [];
                vm.params.students.forEach(student => students.push(student.id));
                vm.params.groups.forEach(group => groups.push(group.id));
                vm.absences = await AbsenceService.getCounsellorAbsence(window.structure.id, students, groups, start, end, null, false, null);
                $scope.safeApply();
            } catch (err) {
                notify.error('presences.absences.load.failed');
            }
        };

        async function loadReasons() {
            vm.reasons = await ReasonService.getReasons(window.structure.id);
            vm.reasons.forEach(reason => vm.provingReasonMap[reason.id] = reason.proving);
            $scope.safeApply();
        }

        /**
         * Retrieve students and groups based on the user query.
         * @param searchText The user query
         */
        vm.searchStudentOrGroup = async (searchText: string): Promise<void> => {
            vm.searchResults = await SearchService.search(window.structure.id, searchText);
            $scope.safeApply();
        };


        /**
         * Select the item from the absence search results.
         * @param model The user query
         * @param option The selected student or group from the search results
         */
        vm.selectStudentOrGroup = (model: SearchItem, option: SearchItem): void => {
            if (!_.find(option, vm.params.students) && option.type === 'USER') {
                vm.params.students.push(option);
            } else if (!_.find(option, vm.params.groups) && option.type === 'GROUP') {
                vm.params.groups.push(option);
            }

            vm.searchResults = null;
            vm.params.search = '';
            vm.load();
        };

        /**
         * Remove a student from the search selection.
         * @param student The student to remove.
         */
        vm.removeStudent = (student: SearchItem): void => {
            vm.params.students = _.without(vm.params.students, student);
            vm.load();
        };

        /**
         * Remove a group from the search selection.
         * @param group The group to remove.
         */
        vm.removeGroup = (group: SearchItem): void => {
            vm.params.groups = _.without(vm.params.groups, group);
            vm.load();
        };

        vm.setAbsenceRegularisation = function (absence) {
            absence.counsellor_regularisation = vm.provingReasonMap[absence.reason_id];
            vm.regularizeAbsence(absence);
            $scope.safeApply();
        };

        vm.regularizeAbsence = function (absence) {
            if (absence.counsellor_regularisation) {
                vm.absences = vm.absences.filter(abs => abs.id !== absence.id);
                $scope.safeApply();
            }
        };

        vm.showAbsenceRange = (absence: CounsellorAbsence): String => {
            let result: String = `${DateUtils.format(absence.start_date, DateUtils.FORMAT['DAY-MONTH-YEAR'])} ${DateUtils.format(absence.start_date, DateUtils.FORMAT['HOUR-MINUTES'])}`;

            if (DateUtils.getDayNumberDifference(absence.start_date, absence.end_date))
                result += ` - ${DateUtils.format(absence.end_date, DateUtils.FORMAT['DAY-MONTH-YEAR'])} `;
            else
                result += "-";

            return result + DateUtils.format(absence.end_date, DateUtils.FORMAT['HOUR-MINUTES']);
        };

        loadReasons();
        vm.load();
    }]);