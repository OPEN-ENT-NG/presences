import {_, idiom as lang, ng} from 'entcore';
import {Exemptions, Student, Students} from "../models";
import {EXEMPTIONS_FORM_EVENTS} from '../sniplets';
import {StudentsSearch, Toast} from "../utilities";
import {DateUtils} from "@common/utils"
import {SNIPLET_FORM_EMIT_EVENTS} from '@common/model';
import {GroupService} from "@common/services/GroupService";
import {Scope} from './main'
import {SearchService} from "@common/services";

declare let window: any;

interface ViewModel {
    filter: { start_date: any, end_date: any, students: any, classes: any, className: string };
    translate: any;
    // subjects: Subjects;
    classes: any;
    notifications: any[];
    createExemptionLightBox: Boolean;
    exemptions: Exemptions;
    students: Students;
    studentsFrom: Students;
    studentsSearching: Students;
    studentsSearch: StudentsSearch;
    studentsFiltered: any[];
    classesFiltered: any[];
    form: any;
    formStudentSelected: any[];

    initData(): void;

    updateDate(): void;

    dateFormater(date): Date;

    searchByClass(value: string): Promise<void>;

    searchByStudent(string): void;

    selectFilterClass(model: any, option: any): void;

    selectFilterStudent(model: string, option: Student): void;

    filters(student?, audience?): void;

    excludeClassFromFilter(audience): void;

    excludeStudentFromFilter(audience): void;

    selectStudentForm(model: Student, student): void;

    saveExemption(): void;

    deleteExemption(): void;

    createExemption(): void;

    editExemption(obj): void;

    closeCreateExemption(): void;

    excludeStudentFromForm(student): void;

    export(): void;

    sortField(field: string);
}

export const exemptionsController = ng.controller('ExemptionsController',
    ['$scope', '$route', 'GroupService', 'SearchService',
        function ($scope: Scope, $route, GroupService: GroupService, searchService: SearchService) {
            const vm: ViewModel = this;

            console.log('ExemptionsController');
            /**
             * Init usefull var and function PART
             */
            vm.translate = lang.translate;
            const initData = () => {
                vm.notifications = [];
                vm.createExemptionLightBox = false;
                vm.filter = {
                    start_date: DateUtils.add(new Date(), -30, "d"),
                    end_date: new Date(),
                    students: [],
                    classes: [],
                    className: ''
                };

                vm.exemptions = new Exemptions();
                vm.exemptions.eventer.on('loading::false', () => $scope.safeApply());

                vm.classes = undefined;

                vm.students = new Students();
                vm.studentsFrom = new Students();
                vm.studentsFiltered = [];
                vm.classesFiltered = [];
                vm.studentsSearching = new Students();
                vm.studentsSearch = new StudentsSearch(window.structure.id, searchService);
                loadExemptions();
                $scope.safeApply();

            };
            const loadExemptions = async (): Promise<void> => {
                await vm.exemptions.prepareSyncPaginate(window.structure.id, vm.filter.start_date, vm.filter.end_date, vm.studentsFiltered, vm.classesFiltered);
                $scope.safeApply();
            };

            vm.updateDate = async () => {
                await loadExemptions();
                vm.filters();
            };

            vm.dateFormater = (date) => {
                return DateUtils.format(date, 'DD/MM/YYYY');
            };

            /**
             * Init & Manage main filter display PART
             */
            vm.searchByClass = async function (value) {
                const structureId = window.structure.id;
                try {
                    vm.classes = await GroupService.search(structureId, value);
                    vm.classes.map((obj) => obj.toString = () => obj.name);
                    $scope.safeApply();
                } catch (err) {
                    vm.classes = [];
                    throw err;
                }
                return;
            };

            vm.searchByStudent = async (searchText: string) => {
                await vm.studentsSearch.searchStudents(searchText);
                $scope.safeApply();
            };

            vm.selectFilterClass = function (model: Student, option: Student) {
                vm.classes = undefined;
                vm.filter.className = '';
                vm.filters(null, option);
            };

            vm.selectFilterStudent = (model: string, option: Student) => {
                vm.filters(option);
                vm.students.all = undefined;
                vm.students.searchValue = '';
                vm.studentsSearch.selectStudents(model, option);
                vm.studentsSearch.student = "";
            };

            vm.filters = (student?, audience?) => {
                if (audience && !_.find(vm.classesFiltered, audience)) {
                    vm.classesFiltered.push(audience);
                }
                if (student && !_.find(vm.studentsFiltered, student)) {
                    vm.studentsFiltered.push(student);
                }

                vm.exemptions.prepareSyncPaginate(window.structure.id, vm.filter.start_date, vm.filter.end_date, vm.studentsFiltered, vm.classesFiltered);
                $scope.safeApply();
            };

            vm.excludeStudentFromFilter = (student) => {
                vm.studentsFiltered = _.without(vm.studentsFiltered, _.findWhere(vm.studentsFiltered, student));
                vm.filters();
            };
            vm.excludeClassFromFilter = (audience) => {
                vm.classesFiltered = _.without(vm.classesFiltered, _.findWhere(vm.classesFiltered, audience));
                vm.filters();
            };

            vm.export = function () {
                if (vm.exemptions.all.length == 0) {
                    vm.notifications.push(new Toast(vm.translate("presences.exemptions.csv.nothing"), 'info'));
                    $scope.safeApply();
                    return;
                }
                if (vm.exemptions.pageCount > 50) {
                    vm.notifications.push(new Toast(vm.translate("presences.exemptions.csv.toMuchExemptions"), 'info'));
                    $scope.safeApply();
                    return;
                }
                vm.exemptions.export(window.structure.id, vm.filter.start_date, vm.filter.end_date, vm.studentsFiltered, vm.classesFiltered);
            };


            vm.editExemption = (obj) => $scope.$broadcast(EXEMPTIONS_FORM_EVENTS.EDIT, obj);

            vm.sortField = async (field) => {
                switch (field) {
                    case 'date':
                        vm.exemptions.order = field;
                        break;
                    case 'attendance':
                        vm.exemptions.order = field;
                        break;
                    default:
                        vm.exemptions.order = 'date';
                }
                vm.exemptions.reverse = !vm.exemptions.reverse;
                await vm.exemptions.syncPagination();
                $scope.safeApply();
            };


            $scope.$watch(() => window.structure, () => {
                if ($route.current.action === "exemptions") {
                    if ('structure' in window) {
                        initData();
                    }
                } else {
                    $scope.redirectTo('/exemptions');
                }
            });

            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.FILTER, () => vm.filters());
        }]);
