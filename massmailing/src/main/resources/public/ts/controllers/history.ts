import {moment, ng} from 'entcore';
import {GroupService, MailingService, SearchService} from "../services";
import {Mailing, MailingRequest, Mailings, MailingType, MassmailingStatus} from "../model";
import {DateUtils, StudentsSearch} from "@common/utils";
import {GroupsSearch} from "@common/utils/autocomplete/groupsSearch";
import {EventType} from "@common/model";
import {GroupingService} from "@common/services";

declare let window: any;

interface Filter {
    start_date: string;
    end_date: string;
    students: Array<String>;
    groups: Array<String>;
    mailTypes: Array<{ label: string, isSelected: boolean }>;
    event_types: Array<{ label: string, value: string, isSelected: boolean }>;
    page: number;
}

interface MailingLightbox {
    isOpen: boolean;
    students: Array<{}>;
    groups: Array<{}>;
    mailTypes: Array<{ label: string, isSelected: boolean }>;
    event_types: Array<{ label: string, value: string, isSelected: boolean }>;
}

interface ViewModel {
    filter: Filter;
    mailings: Mailings;

    mailingsRequest: MailingRequest;

    studentsSearch: StudentsSearch;
    groupsSearch: GroupsSearch;

    studentsSearchLightbox: StudentsSearch;
    groupsSearchLightbox: GroupsSearch;

    mailingLightbox: MailingLightbox;

    selectedMailing: Mailing;

    $sidebar: JQuery;

    startSidebarAnimation(): void;

    formatDayDate(date: string): string;

    switchMailingHistory(mailing: Mailing): void;

    getIcons(iconValue: string): string;

    getMailingTypeText(selectedMailing: Mailing): string;

    changePagination(): Promise<void>;

    updateFilter(): Promise<void>;

    /* lightbox methods */
    openMailingLightbox(): void;

    validMailingLightboxForm(): Promise<void>;

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

    startSidebarAnimation(): void;

    isSelectedMailingPdf(): boolean;

    downloadSelectedMailingFile(): void;
}

export const historyController = ng.controller('HistoryController',
    ['$scope', '$timeout', '$route', '$location', 'SearchService', 'GroupService', 'GroupingService', 'MailingService',
        function ($scope, $timeout, $route, $location, searchService: SearchService,
                  groupService: GroupService, groupingService: GroupingService, mailingService: MailingService) {
            const vm: ViewModel = this;

            /* Init mailings */
            vm.mailingsRequest = {} as MailingRequest;
            vm.selectedMailing = undefined;

            vm.$sidebar = null;

            /* init mailing type lightbox to interact */
            const initMailingTypes = (): Array<{ label: string, isSelected: boolean }> => {
                let mailTypes = [];
                // looping only key string (in our case PDF, Mail, SMS)
                Object.keys(MailingType).filter(type => !parseInt(type) && type !== '0').forEach(mailingType => {
                    mailTypes.push({label: mailingType, isSelected: true});
                });
                return mailTypes;
            };

            /* init event type lightbox to interact */
            const initEventTypes = (): Array<{ label: string, value: string, isSelected: boolean }> => {
                let eventTypes: Array<{ label: string, value: string, isSelected: boolean }> = [];
                // looping only key string (in our case REGULARIZED, UNREGULARIZED, LATENESS...)
                Object.keys(MassmailingStatus).filter((type: string) => !parseInt(type) && type !== '0').forEach(mailingType => {
                    switch (mailingType) {
                        case MassmailingStatus[MassmailingStatus.NO_REASON]: {
                            let i18n: string = 'massmailing.summary.ABSENCE';
                            eventTypes.push({label: i18n, value: EventType[EventType.ABSENCE], isSelected: true});
                            break;
                        }
                        case MassmailingStatus[MassmailingStatus.LATENESS]: {
                            let i18n: string = 'massmailing.types.LATENESS';
                            eventTypes.push({label: i18n, value: mailingType, isSelected: true});
                            break;
                        }
                        case MassmailingStatus[MassmailingStatus.PUNISHMENT]: {
                            let i18n: string = 'massmailing.types.PUNISHMENT';
                            eventTypes.push({label: i18n, value: mailingType, isSelected: true});
                            break;
                        }
                        case MassmailingStatus[MassmailingStatus.SANCTION]: {
                            let i18n: string = 'massmailing.types.SANCTION';
                            eventTypes.push({label: i18n, value: mailingType, isSelected: true});
                            break;
                        }
                    }
                });
                return eventTypes;
            };

            /* Init filter */
            vm.filter = {
                start_date: moment().startOf('day'),
                end_date: moment().endOf('day'),
                students: [],
                groups: [],
                mailTypes: initMailingTypes(),
                event_types: initEventTypes(),
                page: 0,
            };

            /* Init lightbox */
            vm.mailingLightbox = {
                isOpen: false,
                students: [],
                groups: [],
                mailTypes: initMailingTypes(),
                event_types: initEventTypes()
            };

            /* init mailing lightbox filter when lightbox is clicked */
            const initMailingLightboxFilter = (): void => {
                vm.studentsSearchLightbox.setSelectedStudents(JSON.parse(JSON.stringify(vm.studentsSearch.getSelectedStudents())));
                vm.groupsSearchLightbox.setSelectedGroups(JSON.parse(JSON.stringify(vm.groupsSearch.getSelectedGroups())));
                vm.studentsSearchLightbox.getSelectedStudents().map(student => student.toString = () => student["displayName"]);
                vm.groupsSearchLightbox.getSelectedGroups().map(group => group.toString = () => group["name"]);
                vm.mailingLightbox.mailTypes = JSON.parse(JSON.stringify(vm.filter.mailTypes));
                vm.mailingLightbox.event_types = JSON.parse(JSON.stringify(vm.filter.event_types));
            };

            const load = (): void => {
                /* Init mailings */
                vm.mailings = new Mailings(window.structure.id);
                /* Init search bar */
                vm.studentsSearch = new StudentsSearch(window.structure.id, searchService);
                vm.groupsSearch = new GroupsSearch(window.structure.id, searchService, groupService, groupingService);
                /* Init search bar lightbox */
                vm.studentsSearchLightbox = new StudentsSearch(window.structure.id, searchService);
                vm.groupsSearchLightbox = new GroupsSearch(window.structure.id, searchService, groupService, groupingService);
            };

            const getMailingsHistory = async (): Promise<void> => {
                vm.mailings.loading = true;
                prepareRequest();
                await vm.mailings.build(await mailingService.get(vm.mailingsRequest));
                vm.mailings.loading = false;
                vm.selectedMailing = undefined;
                $scope.safeApply();
            };

            const prepareRequest = (): void => {
                vm.mailingsRequest.structure = vm.mailings.structure_id;
                vm.mailingsRequest.start = DateUtils.format(vm.filter.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.mailingsRequest.end = DateUtils.format(vm.filter.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.mailingsRequest.mailTypes = vm.filter.mailTypes.filter(mailType => mailType.isSelected).map(mailType => mailType.label);
                vm.mailingsRequest.event_types = vm.filter.event_types.filter(eventType => eventType.isSelected).map(eventType => eventType.value);
                vm.mailingsRequest.students = vm.filter.students;
                vm.mailingsRequest.groups = vm.filter.groups;
                vm.mailingsRequest.page = vm.filter.page;
            };

            vm.formatDayDate = (date: string): string => DateUtils.format(parseInt(date), DateUtils.FORMAT["DAY-MONTH-YEAR"]);

            vm.switchMailingHistory = (mailing: Mailing): void => {
                mailing.isSelected = !mailing.isSelected;
                let selectedMailing = mailing;
                vm.mailings.mailingResponse.all.forEach(mailing => {
                    if (mailing != selectedMailing) {
                        mailing.isSelected = false
                    }
                });
                /* set our selectedMailing */
                vm.selectedMailing = vm.mailings.mailingResponse.all.find(mailing => mailing.isSelected);
                // if (vm.mailings.mailingResponse.all.length > 0) startSidebarAnimation();

                $scope.safeApply();
            };

            vm.getIcons = (iconValue: string): string => {
                switch (iconValue) {
                    case MailingType[MailingType.MAIL]:
                        return 'presences-email';
                    case MailingType[MailingType.PDF]:
                        return 'pdf';
                    case MailingType[MailingType.SMS]:
                        return 'phone-android';
                    default:
                        return '';
                }
            };

            vm.getMailingTypeText = (selectedMailing: Mailing): string => {
                if (!selectedMailing) return '';
                switch (selectedMailing.type) {
                    case MailingType[MailingType.MAIL]:
                        return 'massmailing.message.sent.to.email';
                    case MailingType[MailingType.SMS]:
                        return 'massmailing.message.sent.to.phone';
                    default:
                        return '';
                }
            };

            vm.downloadSelectedMailingFile = (): void => {
                mailingService.downloadFile(vm.selectedMailing);
            };

            vm.isSelectedMailingPdf = (): boolean => vm.selectedMailing && vm.selectedMailing.type === MailingType[MailingType.PDF];

            vm.changePagination = async (): Promise<void> => {
                vm.filter.page = vm.mailings.mailingResponse.page;
                await getMailingsHistory();
            };

            vm.updateFilter = async (): Promise<void> => {
                /* Retrieving our search bar info */
                vm.filter.students = vm.studentsSearch.getSelectedStudents().map(student => student["id"]);
                vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group["id"]);

                await getMailingsHistory();
            };

            /* lightbox methods */

            vm.openMailingLightbox = (): void => {
                vm.mailingLightbox.isOpen = true;
                initMailingLightboxFilter();
            };

            vm.validMailingLightboxForm = async (): Promise<void> => {
                // sending back information from our search lightbox to our "normal" search bar from main view
                vm.studentsSearch.setSelectedStudents(JSON.parse(JSON.stringify(vm.studentsSearchLightbox.getSelectedStudents())));
                vm.studentsSearch.getSelectedStudents().map(student => student.toString = () => student["displayName"]);

                vm.groupsSearch.setSelectedGroups(JSON.parse(JSON.stringify(vm.groupsSearchLightbox.getSelectedGroups())));
                vm.groupsSearch.getSelectedGroups().map(group => group.toString = () => group["name"]);

                vm.filter.event_types = vm.mailingLightbox.event_types;
                vm.filter.mailTypes = vm.mailingLightbox.mailTypes;

                vm.updateFilter();
                vm.mailingLightbox.isOpen = false;
            };

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

            /* Animation sidebar card */
            vm.startSidebarAnimation = () => {
                if (vm.$sidebar && vm.$sidebar.length > 0) return;
                vm.$sidebar = $("#sidebar");
                let $window = $(window);

                let top = 344;
                let navbarHeight = 96;

                // while scrolling
                $window.scroll(() => {
                    if ($window.scrollTop() > top) {
                        vm.$sidebar.stop().animate({marginTop: $window.scrollTop() - top + navbarHeight});
                    } else {
                        vm.$sidebar.stop().animate({marginTop: 0});
                    }
                });
            };


            /* on  (watch) */
            $scope.$watch(() => window.structure, () => {
                load();
                getMailingsHistory();
            });

        }]);