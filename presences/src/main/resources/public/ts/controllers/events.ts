import {_, Me, ng} from 'entcore';
import {IAngularEvent, ILocationService, IScope, IWindowService} from "angular";
import {Student, Students} from "@common/model/Student";
import {GroupsSearch, PresencesPreferenceUtils, safeApply, StudentsSearch} from "@common/utils";
import {Group, GroupService, SearchService} from "@common/services";
import {EVENTS_DATE, EVENTS_FORM, EVENTS_SEARCH} from "@common/core/enum/presences-event";
import {EventsFilter} from "@presences/utilities";
import {EventListCalendarFilter} from "@presences/models";

declare let window: any;

interface IViewModel {
    $onInit(): void;

    homeState(): boolean;

    plannedAbsencesState(): boolean;

    searchStudent(searchText: string): Promise<void>;

    selectStudent(): (model: string, option: Student) => void;

    removeSelectedStudent(student: Student): void;

    searchGroup(searchText: string): Promise<void>;

    selectGroup(): (model: string, option: Group) => void;

    removeSelectedGroup(group: Group): void;

    students: Students;
    studentsSearch: StudentsSearch;
    groups: Array<Group>;
    groupsSearch: GroupsSearch;
}

class Controller implements ng.IController, IViewModel {

    $parent: any;

    students: Students;
    studentsSearch: StudentsSearch;
    groups: Array<Group>;
    groupsSearch: GroupsSearch;

    // List for saving dates filters of event list and planned absence list
    eventListDates: {startDate: Date, endDate: Date};
    absenceListDates: {startDate: Date, endDate: Date};

    viewModel: IViewModel;


    constructor(private $scope: IScope,
                private $location: ILocationService,
                private $window: IWindowService,
                private searchService: SearchService,
                private groupService: GroupService) {
        this.$scope['vm'] = this;
        this.studentsSearch = null;
        this.groupsSearch = null;
        this.eventListDates = {startDate: null, endDate: null};
        this.absenceListDates = {startDate: null, endDate: null};

        /* on (watch) */
        this.$scope.$watch(() => window.structure, async () => {
            if (window.structure) {
                this.studentsSearch = new StudentsSearch(window.structure.id,
                    this.searchService);
                this.groupsSearch = new GroupsSearch(window.structure.id,
                    this.searchService, this.groupService);
            }
        });

        this.$scope.$on(EVENTS_FORM.SUBMIT, async (event: IAngularEvent, filter: EventsFilter) => {
            this.studentsSearch.setSelectedStudents(filter.students);
            this.groupsSearch.setSelectedGroups(filter.classes);
        });


        this.$scope.$on(EVENTS_DATE.EVENT_LIST_SAVE, async (event: IAngularEvent, dates: {startDate: Date, endDate: Date}) => {
            this.eventListDates = dates;
        });

        this.$scope.$on(EVENTS_DATE.ABSENCES_SAVE, async (event: IAngularEvent, dates: {startDate: Date, endDate: Date}) => {
            this.absenceListDates = dates;
        });

        this.$scope.$on(EVENTS_DATE.ABSENCES_REQUEST, () => {
            this.$scope.$broadcast(EVENTS_DATE.ABSENCES_SEND, this.absenceListDates);
        });

        this.$scope.$on(EVENTS_DATE.EVENT_LIST_REQUEST, () => {
            this.$scope.$broadcast(EVENTS_DATE.EVENT_LIST_SEND, this.eventListDates);
        });
    }

    async $onInit(): Promise<void> {
        this.students = new Students();
        let calendarFilter: EventListCalendarFilter = JSON.parse(JSON.stringify(await Me.preference(PresencesPreferenceUtils.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_CALENDAR_FILTER)));
        if (this.studentsSearch) this.studentsSearch.setSelectedStudents(calendarFilter && calendarFilter.students ? calendarFilter.students : []);
        if (this.groupsSearch) this.groupsSearch.setSelectedGroups(calendarFilter && calendarFilter.classes ? calendarFilter.classes : []);
    }

    homeState(): boolean {
        return this.$location.path() === '/events';
    }

    plannedAbsencesState(): boolean {
        return this.$location.path() === '/planned-absences';
    }

    /* ----------------------------
         Student methods
       ---------------------------- */
    async searchStudent(searchText: string): Promise<void> {
        await this.$parent.vm.studentsSearch.searchStudents(searchText);
        safeApply(this.$parent.vm.$scope);
    }


    selectStudent(): (model: string, option: Student) => void {
        let self: Controller = this;
        return (model: string, option: Student): void => {
            self.studentsSearch.selectStudents(model, option);
            self.studentsSearch.student = "";
            this.$scope.$broadcast(EVENTS_SEARCH.STUDENT, self.studentsSearch.getSelectedStudents());
        };
    }

    removeSelectedStudent(student: Student): void {
        this.studentsSearch.removeSelectedStudents(student);
        this.$scope.$broadcast(EVENTS_SEARCH.STUDENT, this.studentsSearch.getSelectedStudents());
    }

    async searchGroup(searchText: string): Promise<void> {
        await this.$parent.vm.groupsSearch.searchGroups(searchText);
        safeApply(this.$parent.vm.$scope);
    }

    selectGroup(): (model: string, option: Group) => void {
        let self: Controller = this;
        return (model: string, option: Group): void => {
            self.groupsSearch.selectGroups(model, option);
            self.groupsSearch.group = "";
            this.$scope.$broadcast(EVENTS_SEARCH.GROUP, self.groupsSearch.getSelectedGroups());
        };
    }

    removeSelectedGroup(group: Group): void {
        this.groupsSearch.removeSelectedGroups(group);
        this.$scope.$broadcast(EVENTS_SEARCH.GROUP, this.groupsSearch.getSelectedGroups());
    }

}

export const eventsController = ng.controller('EventsController',
    ['$scope', '$location', '$window', 'SearchService', 'GroupService', Controller]);