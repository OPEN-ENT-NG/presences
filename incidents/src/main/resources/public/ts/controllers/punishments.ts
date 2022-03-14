import {moment, ng, notify, toasts} from 'entcore';

import {DateUtils, StudentsSearch} from "@common/utils";
import {GroupsSearch} from "@common/utils/autocomplete/groupsSearch";
import {PunishmentsUtils} from "@incidents/utilities/punishments";
import {
    IPDetentionField,
    IPunishment,
    IPunishmentBody,
    IPunishmentRequest,
    ISchoolYearPeriod,
    Punishments
} from "@incidents/models";
import {
    GroupService,
    IPunishmentService,
    IPunishmentsTypeService,
    IViescolaireService, PunishmentsCategoryService,
    SearchService
} from "@incidents/services";
import {SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS} from "@common/model";
import {IPunishmentType} from "@incidents/models/PunishmentType";
import {User} from "@common/model/User";
import {PunishmentCategoryType} from "@incidents/models/PunishmentCategory";

declare let window: any;

interface IFilter {
    start_date: string;
    end_date: string;
    students: Array<string>;
    groups: Array<string>;
    punishmentsRules: Array<{ label: string, value: string, isSelected: boolean, type: string }>;
    punishmentsStates: Array<{ label: string, value: string, isSelected: boolean }>;
    massmaillingsPunishments: Array<{ label: string, isSelected: boolean }>;
    page: number;
}

interface IFilterForm {
    isOpen: boolean;
    students: Array<{}>;
    groups: Array<{}>;
    isAllPunishmentTypeSelected: boolean;
    punishmentsRules: Array<{ label: string, value: string, isSelected: boolean, type: string }>;
    punishmentsStates: Array<{ label: string, value: string, isSelected: boolean }>;
    massmaillingsPunishments: Array<{ label: string, isSelected: boolean }>;
    isUpdated?: boolean;
}

interface IViewModel {

    filter: IFilter;
    punishments: Punishments;

    punishmentsRequest: IPunishmentRequest;

    studentsSearch: StudentsSearch;
    groupsSearch: GroupsSearch;

    studentsSearchLightbox: StudentsSearch;
    groupsSearchLightbox: GroupsSearch;

    punishmentsTypes: IPunishmentType[];

    filterForm: IFilterForm;

    updateFilter(): Promise<void>;

    getPunishmentDate(punishment: IPunishment): string;

    getPunishmentTime(punishment: IPunishment): string;

    stopPropagation($event): void;

    updateProcessPunishment(punishment: IPunishment): Promise<void>;

    openPunishment(punishment: IPunishment): void;

    changePagination(): void;

    /* lightbox methods */
    openFilterLightbox(): void;

    getSelectedPunishmentType(): number;

    toggleAllPunishmentType(): void;

    togglePunishmentType(punishmentRule: { label: string, value: string, isSelected: boolean }): void;

    togglePunishmentState(punishmentState: { label: string, value: string, isSelected: boolean, type: string }): void;

    setSelectedPunishmentType(punishmentType: IPunishmentType): void;

    validFilterLightboxForm(): Promise<void>;

    /* search bar methods */
    searchStudent(studentForm: string): Promise<void>;

    selectStudent(valueInput, studentItem): void;

    removeSelectedStudents(studentItem): void;

    searchGroup(groupForm: string): Promise<void>;

    selectGroup(valueInput, groupItem): void;

    removeSelectedGroups(groupItem): void;

    /* search bar methods from LIGHTBOX */
    searchStudentLightbox(studentForm: string): Promise<void>;

    selectStudentLightbox(valueInput, studentItem): void;

    removeSelectedStudentsLightbox(studentItem): void;

    searchGroupLightbox(groupForm: string): Promise<void>;

    selectGroupLightbox(valueInput, groupItem): void;

    removeSelectedGroupsLightbox(groupItem): void;

    // CSV
    exportCsv(): void;
}

export const punishmentController = ng.controller('PunishmentController',
    ['$scope', '$route', '$location', 'SearchService', 'GroupService', 'PunishmentService',
        'PunishmentsTypeService', 'ViescolaireService',
        function ($scope, $route, $location, searchService: SearchService,
                  groupService: GroupService, punishmentService: IPunishmentService,
                  punishmentTypeService: IPunishmentsTypeService, viescolaireService: IViescolaireService) {
            const vm: IViewModel = this;

            /* Init search bar */
            vm.studentsSearch = undefined;
            vm.groupsSearch = undefined;

            /* Init search bar filter form */
            vm.studentsSearchLightbox = undefined;
            vm.groupsSearchLightbox = undefined;

            vm.punishmentsTypes = [];

            /* Init filter */
            vm.filter = {
                start_date: null,
                end_date: moment().endOf('day'),
                students: [],
                groups: [],
                punishmentsRules: PunishmentsUtils.initPunishmentRules(),
                punishmentsStates: PunishmentsUtils.initPunishmentStates(),
                massmaillingsPunishments: PunishmentsUtils.initMassmailingsPunishments(),
                page: 0
            };


            /* Init lightbox */
            vm.filterForm = {
                isOpen: false,
                students: [],
                groups: [],
                isAllPunishmentTypeSelected: false,
                punishmentsRules: PunishmentsUtils.initPunishmentRules(),
                punishmentsStates: PunishmentsUtils.initPunishmentStates(),
                massmaillingsPunishments: PunishmentsUtils.initMassmailingsPunishments(),
                isUpdated: false,
            };

            vm.punishments = undefined;
            vm.punishmentsRequest = {} as IPunishmentRequest;

            /* init filter form with current filter info when lightbox is clicked */
            const initFilterForm = (): void => {
                vm.studentsSearchLightbox.setSelectedStudents(JSON.parse(JSON.stringify(vm.studentsSearch.getSelectedStudents())));
                vm.groupsSearchLightbox.setSelectedGroups(JSON.parse(JSON.stringify(vm.groupsSearch.getSelectedGroups())));
                vm.studentsSearchLightbox.getSelectedStudents().map(student => student.toString = () => student["displayName"]);
                vm.groupsSearchLightbox.getSelectedGroups().map(group => group.toString = () => group["name"]);
                vm.filterForm.punishmentsRules = JSON.parse(JSON.stringify(vm.filter.punishmentsRules));
                vm.filterForm.punishmentsStates = JSON.parse(JSON.stringify(vm.filter.punishmentsStates));
                vm.filterForm.massmaillingsPunishments = JSON.parse(JSON.stringify(vm.filter.massmaillingsPunishments));
                initPunishmentTypeFilter();
            };

            const load = async () => {
                vm.punishments = new Punishments(window.structure.id);

                /* Init search bar */
                vm.studentsSearch = new StudentsSearch(window.structure.id, searchService);
                vm.groupsSearch = new GroupsSearch(window.structure.id, searchService, groupService);

                setDataFromMemento();

                /* Init search bar lightbox */
                vm.studentsSearchLightbox = new StudentsSearch(window.structure.id, searchService);
                vm.groupsSearchLightbox = new GroupsSearch(window.structure.id, searchService, groupService);

                /* Init date beginning */
                if (vm.filter.start_date === null) {
                    const schoolYears: ISchoolYearPeriod = await viescolaireService.getSchoolYearDates(window.structure.id);
                    if (schoolYears) {
                        vm.filter.start_date = moment(schoolYears.start_date);
                    } else {
                        vm.filter.start_date = moment().startOf('day');
                    }
                }

                /* get punishmentType */
                if (!vm.punishmentsTypes || vm.punishmentsTypes.length <= 0) {
                    vm.punishmentsTypes = await punishmentTypeService.get(vm.punishments.structure_id);
                    vm.punishmentsTypes = await punishmentTypeService.get(vm.punishments.structure_id);

                    if (PunishmentsUtils.canCreatePunishmentOnly()) {
                        vm.punishmentsTypes = vm.punishmentsTypes.filter((punishmentType: IPunishmentType) =>
                            punishmentType.type === PunishmentsUtils.RULES.punishment);
                    }
                    vm.punishmentsTypes.map((punishmentType: IPunishmentType) => punishmentType.isSelected = false);
                }

                /* event */
                vm.punishments.eventer.on('loading::true', () => $scope.safeApply());
                vm.punishments.eventer.on('loading::false', () => $scope.safeApply());
                getPunishments();
            };

            const setDataFromMemento = (): void => {
                const fetchedParam: { mementoStudentId: string, mementoStudentName: string } = $location.search();
                // if there is nothing to fetch since this is only from memento
                if (!fetchedParam.mementoStudentId && !fetchedParam.mementoStudentName) {
                    return;
                }
                let student: User = {
                    id: fetchedParam.mementoStudentId,
                    displayName: fetchedParam.mementoStudentName,
                }
                vm.filter.students.push(student.id);
                vm.studentsSearch.selectStudent("", student);
                $location.search({});
            };

            const getPunishments = async (): Promise<void> => {
                vm.punishments.loading = true;
                prepareRequest();
                await vm.punishments.build(await punishmentService.get(vm.punishmentsRequest));
                vm.punishments.loading = false;
                $scope.safeApply();
            };

            const prepareRequest = (): void => {
                vm.punishmentsRequest.structure_id = vm.punishments.structure_id;
                vm.punishmentsRequest.start_at = DateUtils.format(vm.filter.start_date, DateUtils.FORMAT['YEAR-MONTH-DAY']) + ' 00:00:00';
                vm.punishmentsRequest.end_at = DateUtils.format(vm.filter.end_date, DateUtils.FORMAT['YEAR-MONTH-DAY']) + ' 23:59:59';
                vm.punishmentsRequest.students_ids = vm.filter.students;
                vm.punishmentsRequest.groups_ids = vm.filter.groups;
                vm.punishmentsRequest.type_ids = vm.punishmentsTypes
                    .filter(punishmentType => punishmentType.isSelected)
                    .map(punishmentType => punishmentType.id);
                vm.punishmentsRequest.massmailed = undefined;
                vm.punishmentsRequest.process_state = vm.filter.punishmentsStates;
                vm.punishmentsRequest.page = vm.filter.page;

            };

            vm.getPunishmentDate = (punishment: IPunishment): string => PunishmentsUtils.getPunishmentDate(punishment);

            vm.getPunishmentTime = (punishment: IPunishment): string => {
                if (punishment.type && punishment.type.punishment_category_id === PunishmentCategoryType.DETENTION && (<Array<IPDetentionField>>punishment.fields).length > 0) { // DETENTION
                    const slot: IPDetentionField = (<Array<IPDetentionField>>punishment.fields)[0];
                    if (slot.start_at && slot.end_at) {
                        let startDetentionDate: string = DateUtils.format(slot.start_at, DateUtils.FORMAT['HOUR-MIN']);
                        let endDetentionDate: string = DateUtils.format(slot.end_at, DateUtils.FORMAT['HOUR-MIN']);
                        if (startDetentionDate && endDetentionDate) {
                            return startDetentionDate + ' - ' + endDetentionDate;
                        } else {
                            return ' ';
                        }
                    }
                }
                return '';
            };

            vm.updateFilter = async (): Promise<void> => {
                /* Retrieving our search bar info */
                vm.filter.students = vm.studentsSearch.getSelectedStudents().map(student => student["id"]);
                vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group["id"]);

                await getPunishments();
            };

            vm.stopPropagation = ($event): void => {
                $event.stopPropagation();
            };

            vm.updateProcessPunishment = async (punishment: IPunishment) => {
                try {
                    let form = {
                        id: punishment.id,
                        grouped_punishment_id: punishment.grouped_punishment_id,
                        structure_id: punishment.structure_id,
                        description: punishment.description,
                        fields: punishment.fields,
                        type_id: punishment.type.id,
                        type: punishment.type,
                        owner_id: punishment.owner.id,
                        student_id: punishment.student.id,
                        processed: punishment.processed,
                    } as IPunishmentBody;

                    let response = await punishmentService.update(form);
                    if (response.status == 200 || response.status == 201) {
                        if (punishment.processed) {
                            toasts.confirm('incidents.punishment.processed.done');
                        } else {
                            toasts.confirm('incidents.punishment.processed.undone');
                        }
                    } else {
                        toasts.warning(response.data.toString());
                    }
                    $scope.safeApply();
                } catch (err) {
                    notify.error('incidents.punishment.edit.form.err');
                    $scope.safeApply();
                    throw err;
                }
            };

            vm.openPunishment = (punishment: IPunishment): void => {
                if (punishment.type &&
                    ((punishment.type.type === PunishmentsUtils.RULES.sanction) && $scope.hasRight('createSanction')) ||
                    ((punishment.type.type === PunishmentsUtils.RULES.punishment) && $scope.hasRight('createPunishment'))) {
                    $scope.$broadcast(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.OPEN, JSON.parse(JSON.stringify(punishment)));
                }
            };

            vm.changePagination = async (): Promise<void> => {
                vm.filter.page = vm.punishments.punishmentResponse.page;
                await getPunishments();
            };

            /**
             * ⚠ filter form methods
             */

            vm.openFilterLightbox = (): void => {
                vm.filterForm.isOpen = true;
                initFilterForm();
            };

            vm.getSelectedPunishmentType = (): number => {
                return vm.punishmentsTypes.filter(punishmentType => punishmentType.isSelected).length;
            };

            vm.toggleAllPunishmentType = (): void => {
                vm.filterForm.isAllPunishmentTypeSelected = !vm.filterForm.isAllPunishmentTypeSelected;
                if (vm.filterForm.isAllPunishmentTypeSelected) {
                    vm.punishmentsTypes.map(punishmentType => punishmentType.isSelected = true);
                } else {
                    vm.punishmentsTypes.map(punishmentType => punishmentType.isSelected = false);
                }
                updatePunishmentRuleOnChangeType();
            };

            vm.togglePunishmentType = (punishmentRule: { label: string, value: string, isSelected: boolean, type: string }): void => {
                punishmentRule.isSelected = !punishmentRule.isSelected;
                applyPunishmentTypeFilterOnRule(punishmentRule);
                updateAllNoneOption();
            };

            /**
             * Activate or deactivate the processed value of the punishment.
             * @param punishmentState   the punishment processed state.
             */
            vm.togglePunishmentState = (punishmentState: { label: string, value: string, isSelected: boolean, type: string }): void => {
                punishmentState.isSelected = !punishmentState.isSelected;
                vm.filterForm.punishmentsStates.filter(state => state.value === punishmentState.value)
                    .forEach(state => state.isSelected = punishmentState.isSelected);
            };

            const initPunishmentTypeFilter = () => {
                if (!vm.filterForm.isUpdated) {
                    vm.filterForm.punishmentsRules.forEach(punishmentRule => {
                        applyPunishmentTypeFilterOnRule(punishmentRule);
                    });
                }
                updateAllNoneOption();
            };

            const applyPunishmentTypeFilterOnRule = (punishmentRule: { label: string, value: string, isSelected: boolean, type: string }) => {
                // checking all selected to true
                if (punishmentRule.isSelected) {
                    if (punishmentRule.type === PunishmentsUtils.RULES.punishment) {
                        vm.punishmentsTypes
                            .filter(punishmentType => punishmentType.type === PunishmentsUtils.RULES.punishment)
                            .map(punishmentType => punishmentType.isSelected = true);
                    } else if (punishmentRule.type === PunishmentsUtils.RULES.sanction) {
                        vm.punishmentsTypes
                            .filter(punishmentType => punishmentType.type === PunishmentsUtils.RULES.sanction)
                            .map(punishmentType => punishmentType.isSelected = true);
                    }
                }

                // checking all selected to false
                if (!punishmentRule.isSelected) {
                    if (punishmentRule.type === PunishmentsUtils.RULES.punishment) {
                        vm.punishmentsTypes
                            .filter(punishmentType => punishmentType.type === PunishmentsUtils.RULES.punishment)
                            .map(punishmentType => punishmentType.isSelected = false);
                    } else if (punishmentRule.type === PunishmentsUtils.RULES.sanction) {
                        vm.punishmentsTypes
                            .filter(punishmentType => punishmentType.type === PunishmentsUtils.RULES.sanction)
                            .map(punishmentType => punishmentType.isSelected = false);
                    }
                }
            };

            const updatePunishmentRuleOnChangeType = () => {
                vm.filterForm.punishmentsRules.map(punishmentRule => {
                    updatePunishmentRule(punishmentRule, PunishmentsUtils.RULES.punishment);
                    updatePunishmentRule(punishmentRule, PunishmentsUtils.RULES.sanction);
                });
            };

            const updateAllNoneOption = () => {
                vm.filterForm.isAllPunishmentTypeSelected = vm.getSelectedPunishmentType() === vm.punishmentsTypes.length;
            };

            const updatePunishmentRule = (punishmentRule: { label: string, value: string, isSelected: boolean, type: string }, rule) => {
                if (punishmentRule.type === rule) {
                    let isAllSelected: boolean = false;
                    vm.punishmentsTypes
                        .filter(punishmentType => punishmentType.type === rule)
                        .forEach(punishmentType => {
                            if (punishmentType.isSelected) {
                                isAllSelected = true;
                            }
                        });
                    punishmentRule.isSelected = isAllSelected;
                }
            };

            vm.setSelectedPunishmentType = (punishmentType: IPunishmentType): void => {
                punishmentType.isSelected = !punishmentType.isSelected;
                vm.filterForm.isAllPunishmentTypeSelected = vm.punishmentsTypes
                    .every(punishmentType => punishmentType === vm.punishmentsTypes[0]);
                updatePunishmentRuleOnChangeType();
                updateAllNoneOption();
            };

            vm.validFilterLightboxForm = async (): Promise<void> => {
                // sending back information from our search lightbox to our "normal" search bar from main view
                vm.filterForm.isUpdated = true;
                vm.studentsSearch.setSelectedStudents(JSON.parse(JSON.stringify(vm.studentsSearchLightbox.getSelectedStudents())));
                vm.studentsSearch.getSelectedStudents().map(student => student.toString = () => student["displayName"]);

                vm.groupsSearch.setSelectedGroups(JSON.parse(JSON.stringify(vm.groupsSearchLightbox.getSelectedGroups())));
                vm.groupsSearch.getSelectedGroups().map(group => group.toString = () => group["name"]);

                vm.filter.punishmentsRules = vm.filterForm.punishmentsRules;
                vm.filter.punishmentsStates = vm.filterForm.punishmentsStates;
                vm.filter.massmaillingsPunishments = vm.filterForm.massmaillingsPunishments;

                vm.updateFilter();
                vm.filterForm.isOpen = false;
            };

            /**
             * ⚠ Autocomplete classes/methods for students and groups
             */

            /* Search bar student section */
            vm.searchStudent = async (studentForm: string): Promise<void> => {
                await vm.studentsSearch.searchStudents(studentForm);
                $scope.safeApply();
            };

            vm.selectStudent = (valueInput, studentItem): void => {
                vm.studentsSearch.selectStudents(valueInput, studentItem);
                vm.filter.students = vm.studentsSearch.getSelectedStudents().map(student => student["id"]);
                vm.studentsSearch.student = "";
                vm.updateFilter();
            };

            vm.removeSelectedStudents = (studentItem): void => {
                vm.studentsSearch.removeSelectedStudents(studentItem);
                vm.filter.students = vm.studentsSearch.getSelectedStudents().map(student => student["id"]);
                vm.updateFilter();
            };

            /* Search bar groups section */
            vm.searchGroup = async (groupForm: string): Promise<void> => {
                await vm.groupsSearch.searchGroups(groupForm);
                $scope.safeApply();
            };

            vm.selectGroup = (valueInput, groupForm): void => {
                vm.groupsSearch.selectGroups(valueInput, groupForm);
                vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group["id"]);
                vm.groupsSearch.group = "";
                vm.updateFilter();
            };

            vm.removeSelectedGroups = (groupForm): void => {
                vm.groupsSearch.removeSelectedGroups(groupForm);
                vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group["id"]);
                vm.updateFilter();
            };

            /* Search bar LIGHTBOX student section */
            vm.searchStudentLightbox = async (studentForm: string): Promise<void> => {
                await vm.studentsSearchLightbox.searchStudents(studentForm);
                $scope.safeApply();
            };

            vm.selectStudentLightbox = (valueInput, studentItem): void => {
                vm.studentsSearchLightbox.selectStudents(valueInput, studentItem);
                vm.studentsSearchLightbox.student = "";
            };

            vm.removeSelectedStudentsLightbox = (studentItem): void => {
                vm.studentsSearchLightbox.removeSelectedStudents(studentItem);
            };

            /* Search bar LIGHTBOX groups section */
            vm.searchGroupLightbox = async (groupForm: string): Promise<void> => {
                await vm.groupsSearchLightbox.searchGroups(groupForm);
                $scope.safeApply();
            };

            vm.selectGroupLightbox = (valueInput, groupForm): void => {
                vm.groupsSearchLightbox.selectGroups(valueInput, groupForm);
                vm.groupsSearchLightbox.group = "";
            };

            vm.removeSelectedGroupsLightbox = (groupForm): void => {
                vm.groupsSearchLightbox.removeSelectedGroups(groupForm);
            };

            /* CSV  */
            vm.exportCsv = (): void => {
                if (vm.punishments.punishmentResponse.page_count > 50) {
                    toasts.warning('incidents.csv.full');
                } else {
                    if (vm.punishments.punishmentResponse.all.length === 0) {
                        toasts.info('incidents.csv.empty');
                    } else {
                        punishmentService.exportCSV(vm.punishmentsRequest);
                    }
                }
            };

            /* ----------------------------
             Punishments events emitted from punishment form sniplet
            ---------------------------- */
            $scope.$on(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.CREATION, load);
            $scope.$on(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.EDIT, load);
            $scope.$on(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.DELETE, load);
            $scope.$on(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.CLOSE, load);

            /* on  (watch) */
            $scope.$watch(() => window.structure, () => {
                if ('structure' in window) {
                    load();
                }
            });
        }]);