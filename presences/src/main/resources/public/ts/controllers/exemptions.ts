import {_, idiom as lang, ng} from 'entcore';
import {Exemptions, Student, Students} from "../models";
import {EXEMPTIONS_FORM_EVENTS} from '../sniplets';
import {GroupsSearch, StudentsSearch, Toast} from "../utilities";
import {DateUtils} from "@common/utils"
import {SNIPLET_FORM_EMIT_EVENTS} from '@common/model';
import {Group, GroupingService, GroupService, SearchService} from "@common/services";
import {Scope} from './main'

declare let window: any;

interface ViewModel {
    filter: { start_date: any, end_date: any, students: any, classes: any, className: string };
    translate: any;
    // subjects: Subjects;
    classes: any;
    notifications: any[];
    createExemptionLightBox: Boolean;
    exemptions: Exemptions;
    groupsSearch: GroupsSearch;
    studentsSearch: StudentsSearch;
    form: any;
    formStudentSelected: any[];

    initData(): void;

    updateDate(): void;

    dateFormater(date): Date;

    searchClass(value: string): Promise<void>;

    searchStudent(value: string): Promise<void>;

    selectClass(model: any, option: any): void;

    selectStudent(model: string, option: Student): void;

    filters(student?, audience?): void;

    selectStudentForm(model: Student, student): void;

    saveExemption(): void;

    deleteExemption(): void;

    createExemption(): void;

    editExemption(obj): void;

    closeCreateExemption(): void;

    excludeStudentFromForm(student): void;

    removeSelectedStudent(student: Student): void

    removeSelectedGroup(group: Group): void

    export(): void;

    sortField(field: string);
}

export const exemptionsController = ng.controller('ExemptionsController',
    ['$scope', '$route', 'GroupService', 'GroupingService', 'SearchService',
        function ($scope: Scope, $route, groupService: GroupService, groupingService: GroupingService, searchService: SearchService) {
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

                vm.studentsSearch = new StudentsSearch(window.structure.id, searchService);
                vm.groupsSearch = new GroupsSearch(window.structure.id, searchService, groupService, groupingService);
                loadExemptions();
                $scope.safeApply();

            };
            const loadExemptions = async (): Promise<void> => {
                await vm.exemptions.prepareSyncPaginate(window.structure.id, vm.filter.start_date, vm.filter.end_date,
                    vm.studentsSearch.getSelectedStudents(), vm.groupsSearch.getSelectedGroups());
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
            vm.searchClass = async (value: string): Promise<void> => {
                await vm.groupsSearch.searchGroups(value);
                $scope.safeApply();
            };

            vm.searchStudent = async (value: string): Promise<void> => {
                await vm.studentsSearch.searchStudents(value);
                $scope.safeApply();
            };

            vm.selectClass = async (model: string, option: Group): Promise<void> => {
                vm.classes = undefined;
                vm.filter.className = '';
                vm.groupsSearch.selectGroups(model, option);
                vm.groupsSearch.group = "";
                vm.filters(null, option);
            };

            vm.selectStudent = async (model: string, option: Student): Promise<void> => {
                vm.studentsSearch.selectStudents(model, option);
                vm.studentsSearch.student = "";
                vm.filters(option);
            };

            vm.filters = (student?, audience?) => {
                if (audience && !_.find(vm.groupsSearch.getSelectedGroups(), audience)) {
                    vm.groupsSearch.selectGroups(null, audience);
                }
                if (student && !_.find(vm.studentsSearch.getSelectedStudents(), student)) {
                    vm.studentsSearch.selectStudents(null, student)
                }

                vm.exemptions.prepareSyncPaginate(window.structure.id, vm.filter.start_date, vm.filter.end_date,
                    vm.studentsSearch.getSelectedStudents(), vm.groupsSearch.getSelectedGroups());
                $scope.safeApply();
            };

            vm.removeSelectedStudent = (student: Student): void => {
                this.studentsSearch.removeSelectedStudents(student);
                vm.filters();
            }

            vm.removeSelectedGroup = (group: Group): void => {
                this.groupsSearch.removeSelectedGroups(group);
                vm.filters();
            }

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
                vm.exemptions.export(window.structure.id, vm.filter.start_date, vm.filter.end_date,
                    vm.studentsSearch.getSelectedStudents(), vm.groupsSearch.getSelectedGroups());
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
