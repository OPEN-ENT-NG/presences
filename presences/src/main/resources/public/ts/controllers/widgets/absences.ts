import {_, moment, ng, notify} from 'entcore';
import {AbsenceService, ReasonService} from '../../services';
import {CounsellorAbsence, Reason, Student, Students} from '../../models';
import {DateUtils} from '@common/utils/date';

interface ViewModel {
    absences: Array<CounsellorAbsence>
    reasons: Array<Reason>
    students: Students
    params: {
        start: Date,
        end: Date,
        student: {
            search: string,
            selection: Array<Student>
        }
    },
    provingReasonMap: any

    load(): Promise<void>

    selectStudent(model: Student, option: Student): void

    searchStudent(string: string): void

    selectStudent(model: Student, option: Student): void

    removeStudent(student: Student): void

    setAbsenceRegularisation(absence: CounsellorAbsence): void

    regularizeAbsence(absence): void

    showAbsenceRange(absence: CounsellorAbsence): String
}

declare let window: any;

export const absencesController = ng.controller('AbsenceController', ['$scope', 'AbsenceService', 'ReasonService',
    function ($scope, AbsenceService: AbsenceService, ReasonService: ReasonService) {
        const vm: ViewModel = this;
        vm.absences = [];
        vm.reasons = [];
        vm.provingReasonMap = {};
        vm.params = {
            start: moment().add(-5, 'days').toDate(),
            end: new Date(),
            student: {
                search: null,
                selection: []
            }
        };

        vm.students = new Students();

        vm.load = async function () {
            try {
                let start = moment(vm.params.start).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                let end = moment(vm.params.end).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                let students = [];
                vm.params.student.selection.forEach(student => students.push(student.id));
                vm.absences = await AbsenceService.getCounsellorAbsence(window.structure.id, students, start, end, null, false, null);
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

        vm.searchStudent = async (searchText: string) => {
            await vm.students.search(window.structure.id, searchText);
            $scope.safeApply();
        };

        vm.selectStudent = function (model: Student, option: Student) {
            if (!_.find(option, vm.params.student.selection)) {
                vm.params.student.selection.push(option);
            }

            vm.students.all = null;
            vm.params.student.search = '';
            vm.load();
        };

        vm.removeStudent = function (student) {
            vm.params.student.selection = _.without(vm.params.student.selection, student);
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