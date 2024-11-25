import {idiom, Me, ng} from 'entcore';
import {ILocationService, IScope, IWindowService} from "angular";
import {Group, GroupService, reasonService, SearchService, ViescolaireService} from "../../services";
import {IStructureSlot, ITimeSlot, Reason, Student, TimeSlotHourPeriod} from "../../models";
import {EventsFilter, EventsFormFilter, GroupsSearch, PresencesPreferenceUtils, StudentsSearch} from "../../utilities";
import {safeApply} from "@common/utils";
import {EVENTS_FORM} from "@common/core/enum/presences-event";
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";
import {ROOTS} from "../../core/enum/roots";

declare let window: any;

interface IViewModel {
    display: boolean;
    absencesOnly: boolean;
    reasons: Array<Reason>;
    filter: EventsFormFilter;
    formFilter: EventsFormFilter;
    structureTimeSlot: IStructureSlot;

    studentsSearch: StudentsSearch;

    translate(key: string): string;

    initData(): Promise<void>;

    resetFilter(): void;

    loadReasons(): Promise<void>;

    updateReasonsFromIds(): void;

    getStructureTimeSlots(): Promise<void>;

    submitForm(): Promise<void>;

    /*  switch event type */
    switchAbsencesFilter(): void;

    switchLateFilter(): void;

    switchDepartureFilter(): void;

    switchFollowedFilter(): void;

    switchNotFollowedFilter(): void;

    switchReason(reason: Reason): void;

    switchAllAbsenceReasons(isAllAbsenceReason?: boolean): void;

    switchAllLatenessReasons(isAllLatenessReason?: boolean): void;

    adaptAllReasonsFilters(isSelected: boolean): void;

    getAbsencesReasons(): Array<Reason>;

    getLatenessReasons(): Array<Reason>;

    switchNoReasonsFilter(): void;

    switchNotRegularizedFilter(): void;

    switchRegularizedFilter(): void;

    adaptEvent(): void;

    adaptReason(): void;

    searchStudent(searchText: string): Promise<void>;

    selectStudent(): (value: string, student: Student) => void;

    removeSelectedStudent(student: Student): void;

    searchGroup(groupForm: string): Promise<void>;

    selectGroup(): (valueInput: string, groupForm: Group) => void;

    removeSelectedGroup(groupForm: Group): void;

    updateFilterSlot(hourPeriod: TimeSlotHourPeriod): void;
}

class Controller implements ng.IController, IViewModel {
    $parent: any;

    /**
     * If true, the form will be displayed.
     */
    display: boolean;
    /**
     * If true, the filter form is adapted to the planned absences view (otherwise,
     * to the event list view).
     */
    absencesOnly: boolean;
    reasons: Array<Reason>;
    noReason: Reason;
    filter: EventsFormFilter;
    formFilter: EventsFormFilter;
    structureTimeSlot: IStructureSlot;

    studentsSearch: StudentsSearch;
    groupsSearch: GroupsSearch;

    constructor(private $scope: IScope,
                private $location: ILocationService,
                private $window: IWindowService,
                private searchService: SearchService,
                private groupService: GroupService) {

        // On display, load search bars data
        this.$scope.$watch(() => this.display, async () => {
            if (this.studentsSearch && this.groupsSearch && this.studentsSearch.structureId &&
                this.groupsSearch.structureId) {
                this.studentsSearch.setSelectedStudents(this.filter.students);
                this.groupsSearch.setSelectedGroups(this.filter.classes);
            }
            await this.initData();
            safeApply(this.$scope);
        });

        this.noReason = {
            id: 0,
            label: this.translate('presences.absence.no.reason'),
            structure_id: '',
            reason_type_id: REASON_TYPE_ID.LATENESS,
            comment: '',
            default: false,
            proving: false,
            group: false
        } as Reason;

        this.$parent = undefined;
    }

    translate = (key: string): string => idiom.translate(key);

    async $onInit(): Promise<void> {
        this.display = false;
        await this.initData();
    }

    async initData(): Promise<void> {

        if (!this.structureTimeSlot || !this.structureTimeSlot._id) {
            this.structureTimeSlot = {} as IStructureSlot;
        }

        let formFilterPref: EventsFormFilter = this.absencesOnly ?
            JSON.parse(JSON.stringify(await Me.preference(PresencesPreferenceUtils.PREFERENCE_KEYS.PRESENCE_PLANNED_ABSENCES_FILTER))) :
            JSON.parse(JSON.stringify(await Me.preference(PresencesPreferenceUtils.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_FILTER)));
        this.formFilter = this.filter ? JSON.parse(JSON.stringify(this.filter)) : {};

        if (!this.structureTimeSlot.slots) {
            await Promise.all([this.loadReasons(), this.getStructureTimeSlots()]);
        }

        if (!this.absencesOnly) {
            // Filter is reset if user is loading with old preferences
            if (formFilterPref[window.structure.id] !== undefined && formFilterPref[window.structure.id].regularized !== undefined) {
                let filterPref: EventsFormFilter = formFilterPref ? formFilterPref[window.structure.id] : null;
                this.formFilter = filterPref ? filterPref : JSON.parse(JSON.stringify(this.filter));
                let hasOnlyNoReason: boolean = this.formFilter.noReasons && !this.formFilter.regularized && !this.formFilter.notRegularized;
                this.updateReasonsFromIds(hasOnlyNoReason ? REASON_TYPE_ID.LATENESS : null);
            } else {
                this.resetFilter();
            }
        }

        const absenceReasons: Reason[] =  this.getAbsencesReasons();
        if (formFilterPref[window.structure.id] === undefined) this.resetFilter();
        else {
            if((this.formFilter.regularized || this.formFilter.notRegularized) &&
                !this.formFilter.reasonIds.filter((reasonId: number) =>  this.getAbsencesReasons().find((reason: Reason) => reason.id === reasonId)).length)
                this.switchAllAbsenceReasons(true);
            this.updateReasonsFromIds();
        }

        this.formFilter.allAbsenceReasons = this.formFilter.reasonIds &&
            (this.formFilter.reasonIds.filter((reasonId: number)  => absenceReasons.find((reason: Reason) => reason.id === reasonId)).length
        === this.getAbsencesReasons().length);

        if (!this.studentsSearch || !this.studentsSearch.structureId || !this.groupsSearch || !this.groupsSearch.structureId) {
            this.studentsSearch = new StudentsSearch(window.structure.id, this.searchService);
            this.groupsSearch = new GroupsSearch(window.structure.id, this.searchService, this.groupService);
        }

        if (this.formFilter.timeslots && this.formFilter.timeslots.start && this.formFilter.timeslots.end) {
            this.formFilter.timeslots = {
                start: this.structureTimeSlot.slots.find((slot: ITimeSlot) =>
                    slot._id === this.formFilter.timeslots.start._id),
                end: this.structureTimeSlot.slots.find((slot: ITimeSlot) =>
                    slot._id === this.formFilter.timeslots.end._id)
            };
        }
    }

    resetFilter(): void {
        this.formFilter = {
            timeslots: {
                start: {name: '', startHour: '', endHour: '', id: ''},
                end: {name: '', startHour: '', endHour: '', id: ''}
            },
            students: [],
            classes: [],
            absences: true,
            departure: true,
            late: true,
            regularized: true,
            allAbsenceReasons: true,
            allLatenessReasons: true,
            noReasons: true,
            noReasonsLateness: true,
            reasonIds: [],
            notRegularized: true,
            followed: true,
            notFollowed: true,
            interns: false,
            halfBoarders: false
        };

        this.switchAllAbsenceReasons(true);
        this.switchAllLatenessReasons(true);
    }

    async loadReasons(): Promise<void> {
        this.reasons = await reasonService.getReasons(window.structure.id, REASON_TYPE_ID.ALL);
        this.reasons = [this.noReason].concat(this.reasons);
        this.updateReasonsFromIds();
    }

    updateReasonsFromIds(reasonTypeId?: number): void {
        if (this.reasons && this.formFilter.reasonIds !== undefined) {
            this.reasons.forEach((reason: Reason) => {
                if (this.formFilter.reasonIds.find((rId: number) => rId === reason.id &&
                    ((reasonTypeId && reason.reason_type_id === reasonTypeId) || !reasonTypeId)) !== undefined) {
                    reason.isSelected = true;
                }
            });
        }
        safeApply(this.$scope);
    }

    async getStructureTimeSlots(): Promise<void> {
        try {
            this.structureTimeSlot = await ViescolaireService.getSlotProfile(window.structure.id);
        } catch (e) {
            this.structureTimeSlot = null;
        }
    }

    async submitForm(): Promise<void> {
        let selectedReasons: Array<Reason> = this.reasons.filter((r: Reason): boolean => r.isSelected);
        let filterResult: EventsFilter = (<EventsFilter>this.formFilter);

        filterResult.reasonIds = (
            ((this.absencesOnly || this.formFilter.absences) && (this.formFilter.regularized || this.formFilter.notRegularized)) || this.formFilter.late
        ) ? selectedReasons.map((r: Reason): number => r.id) : [];
        filterResult.regularized = this.formFilter.regularized;
        filterResult.notRegularized = this.formFilter.notRegularized;
        filterResult.followed = this.formFilter.followed;
        filterResult.notFollowed = this.formFilter.notFollowed;
        filterResult.students = this.studentsSearch.getSelectedStudents();
        filterResult.classes = this.groupsSearch.getSelectedGroups();

        let saveFilter: EventsFilter = Object.assign({}, filterResult);
        saveFilter.students = [];
        saveFilter.classes = [];

        if (this.absencesOnly) {
            await PresencesPreferenceUtils.updatePresencesPlannedAbsencesFilter(saveFilter, window.structure.id);
        } else {
            await PresencesPreferenceUtils.updatePresencesEventListFilter(saveFilter, window.structure.id);
        }
        this.display = false;
        this.$scope.$emit(EVENTS_FORM.SUBMIT, filterResult);
    }

    switchAbsencesFilter(): void {
        this.formFilter.absences = this.absencesOnly ? true : !this.formFilter.absences;
        if (!this.formFilter.absences) {
            this.adaptAllReasonsFilters(false);
            this.formFilter.notRegularized = false;
            this.formFilter.regularized = false;
        }
        this.adaptReason();
    }

    switchLateFilter(): void {
        this.formFilter.late = !this.formFilter.late;
        this.formFilter.noReasonsLateness = this.formFilter.late;
    }

    switchDepartureFilter(): void {
        this.formFilter.departure = !this.formFilter.departure;
    }

    switchNoReasonsFilter(): void {
        this.formFilter.noReasons = !this.formFilter.noReasons;
    }

    switchNotRegularizedFilter(): void {
        this.formFilter.notRegularized = !this.formFilter.notRegularized;
        if (!this.formFilter.notRegularized && !this.formFilter.regularized) this.adaptAllReasonsFilters(false);
        else if (this.formFilter.notRegularized && !this.formFilter.regularized) this.adaptAllReasonsFilters(true);
    }

    switchRegularizedFilter(): void {
        this.formFilter.regularized = !this.formFilter.regularized;
        if (!this.formFilter.notRegularized && !this.formFilter.regularized) this.adaptAllReasonsFilters(false);
        else if (!this.formFilter.notRegularized && this.formFilter.regularized) this.adaptAllReasonsFilters(true);
    }

    switchFollowedFilter(): void {
        if (this.formFilter.followed === true) {
            this.formFilter.notFollowed = true;
        }
        this.formFilter.followed = !this.formFilter.followed;
    }

    switchNotFollowedFilter(): void {
        if (this.formFilter.notFollowed === true) {
            this.formFilter.followed = true;
        }
        this.formFilter.notFollowed = !this.formFilter.notFollowed;
    }

    switchReason(reason: Reason): void {
        if (this.formFilter.notRegularized || this.formFilter.regularized || reason.reason_type_id === REASON_TYPE_ID.LATENESS)
            reason.isSelected = !reason.isSelected;

        if (!this.formFilter.notRegularized && !this.formFilter.regularized && reason.reason_type_id !== REASON_TYPE_ID.LATENESS) {
            reason.isSelected = !reason.isSelected;
            this.formFilter.regularized = true;
            this.formFilter.notRegularized = true;
        }

        if (this.formFilter.late && reason.id === this.noReason.id && reason.reason_type_id === REASON_TYPE_ID.LATENESS)
            this.formFilter.noReasonsLateness = reason.isSelected;

        //If all reasons with reason_type_id = absences are unselected then formFilter.regularized get false and formFilter.notRegularized get false
        if (!this.reasons.some((reason: Reason) => reason.reason_type_id === REASON_TYPE_ID.ABSENCE && reason.isSelected)) {
            this.formFilter.regularized = false;
            this.formFilter.notRegularized = false;
        }
    }

    switchAllAbsenceReasons(isAllAbsenceReason?: boolean): void {
        this.adaptAllReasonsFilters(isAllAbsenceReason != null ? isAllAbsenceReason : !this.formFilter.allAbsenceReasons);
        this.formFilter.notRegularized = this.formFilter.allAbsenceReasons;
        this.formFilter.regularized = this.formFilter.allAbsenceReasons;
        if (!this.formFilter.allAbsenceReasons && this.absencesOnly) this.formFilter.noReasons = true;
        this.adaptEvent();
    }

    adaptAllReasonsFilters(isSelected: boolean): void {
        this.formFilter.allAbsenceReasons = isSelected;
        this.getAbsencesReasons().forEach((reason: Reason) => reason.isSelected = isSelected);
        if (isSelected) this.formFilter.reasonIds = [...this.formFilter.reasonIds, ...this.getAbsencesReasons().map(reason => reason.id)];
        else this.formFilter.reasonIds.filter((reasonId: number) => this.getAbsencesReasons().find((reason: Reason) => reason.id === reasonId));
    }

    switchAllLatenessReasons(isAllLatenessReason?: boolean): void {
        this.formFilter.allLatenessReasons = isAllLatenessReason != null ? isAllLatenessReason : !this.formFilter.allLatenessReasons;
        this.formFilter.noReasonsLateness = this.formFilter.allLatenessReasons;
        this.getLatenessReasons()
            .forEach((reason: Reason) => reason.isSelected = this.formFilter.allLatenessReasons);
    }

    getAbsencesReasons(): Array<Reason> {
        return this.reasons ? this.reasons.filter((reason: Reason): boolean => reason.reason_type_id === REASON_TYPE_ID.ABSENCE) : [];
    }

    getLatenessReasons(): Array<Reason> {
        return this.reasons ? this.reasons.filter((reason: Reason): boolean => reason.reason_type_id === REASON_TYPE_ID.LATENESS) : [];
    }

    adaptEvent(): void {
        if (!this.formFilter.noReasons && !this.formFilter.notRegularized
            && !this.formFilter.regularized) {
            this.switchAbsencesFilter();
        }
    }

    adaptReason(): void {
        if (this.formFilter.absences) {
            this.adaptAllReasonsFilters(true);
            this.formFilter.notRegularized = true;
            this.formFilter.regularized = true;
            this.formFilter.followed = true;
            this.formFilter.notFollowed = true;
        }
    }

    async searchStudent(searchText: string): Promise<void> {
        await this.$parent.vm.studentsSearch.searchStudents(searchText);
        safeApply(this.$parent);
    }

    selectStudent(): (value: string, student: Student) => void {
        let self: Controller = this;
        return (value: string, student: Student): void => {
            self.studentsSearch.selectStudents(value, student);
            self.studentsSearch.student = "";
        };
    }

    removeSelectedStudent(student: Student): void {
        this.studentsSearch.removeSelectedStudents(student);
    }

    async searchGroup(groupForm: string): Promise<void> {
        await this.$parent.vm.groupsSearch.searchGroups(groupForm);
        safeApply(this.$parent);
    }

    selectGroup(): (valueInput: string, groupForm: Group) => void {
        let self: Controller = this;
        return (valueInput: string, groupForm: Group): void => {
            self.groupsSearch.selectGroups(valueInput, groupForm);
            self.groupsSearch.group = "";
        };
    }

    removeSelectedGroup(groupForm: Group): void {
        this.groupsSearch.removeSelectedGroups(groupForm);
    }

    updateFilterSlot = (hourPeriod: TimeSlotHourPeriod): void => {
        if (!this.formFilter.timeslots.start || !this.formFilter.timeslots.end) {
            switch (hourPeriod) {
                case TimeSlotHourPeriod.START_HOUR:
                    this.formFilter.timeslots.end = this.formFilter.timeslots.start;
                    break;
                case TimeSlotHourPeriod.END_HOUR:
                    this.formFilter.timeslots.start = this.formFilter.timeslots.end;
                    break;
                default:
                    return;
            }
        }
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}events-filter-form/events-filter-form.html`,
        scope: {
            display: '=',
            filter: '=',
            absencesOnly: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', 'SearchService', 'GroupService', Controller],
        link: function (scope: ng.IScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: ng.IController) {
        }
    };
}

export const EventsFilterForm = ng.directive('eventsFilterForm', directive);