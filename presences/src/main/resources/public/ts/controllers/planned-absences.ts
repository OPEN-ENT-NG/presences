import {Me, moment, ng} from 'entcore';
import {IAngularEvent, ILocationService, IScope, IWindowService} from "angular";
import {Absence, IStructureSlot, Reason, Student} from "@presences/models";
import {DateUtils, PresencesPreferenceUtils, safeApply} from "@common/utils";
import {AbsenceService, Group, ReasonService} from "../services";
import {EventsFilter, EventsFormFilter} from "@presences/utilities";
import {EventAbsenceSummary} from "@presences/models/Event/EventAbsenceSummary";
import {EXPORT_TYPE, ExportType} from "@common/core/enum/export-type.enum";
import {EVENTS_DATE, EVENTS_FORM, EVENTS_SEARCH} from "@common/core/enum/presences-event";
import {INFINITE_SCROLL_EVENTER} from '@common/core/enum/infinite-scroll-eventer';
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";

declare let window: any;

interface IViewModel {
    filter: EventsFilter;
    exportType: typeof EXPORT_TYPE;

    initFilter(): Promise<void>;

    $onInit(): any;

    onScroll(): Promise<void>;

    getPlannedAbsences(): Promise<void>;

    getAbsenceMarkers(): Promise<void>;

    updateDate(): Promise<void>;

    export(exportType: ExportType): void;
}

class Controller implements ng.IController, IViewModel {
    loading: boolean;
    plannedAbsences: Array<Absence>;
    filter: EventsFilter;
    formFilter: EventsFormFilter;
    structureTimeSlot: IStructureSlot;
    reasons: Array<Reason>;
    exportType: typeof EXPORT_TYPE;

    absencesMarkers: EventAbsenceSummary;
    lightbox: {
        filter: boolean;
    };

    constructor(private $scope: IScope,
                private $location: ILocationService,
                private $window: IWindowService,
                private absenceService: AbsenceService,
                private reasonService: ReasonService) {
        this.$scope['vm'] = this;
        this.exportType = EXPORT_TYPE;
        this.plannedAbsences = [];
        this.reasons = [];
        this.absencesMarkers = {
            nb_day_students: 0,
            nb_internals: 0
        };

        this.structureTimeSlot = {} as IStructureSlot;

        this.lightbox = {
            filter: false,
        };
    }

    async $onInit(): Promise<void> {
        this.loading = false;
        this.reasons = await this.reasonService.getReasons(window.structure.id, REASON_TYPE_ID.ALL);
        await this.initFilter();
        this.$scope.$on(EVENTS_DATE.ABSENCES_SEND, (evt: IAngularEvent, dates: {startDate: Date, endDate: Date}) => {
            if (dates.startDate !== null && dates.endDate !== null) {
                this.filter.startDate = dates.startDate;
                this.filter.endDate = dates.endDate;
            }
        });

        /* on (watch) */
        this.$scope.$watch(() => window.structure, async () => {
            if (window.structure) {
                this.reasons = await this.reasonService.getReasons(window.structure.id, REASON_TYPE_ID.ABSENCE);
                await this.initFilter();
                this.$scope.$emit(EVENTS_DATE.ABSENCES_REQUEST);
                await Promise.all([this.getAbsenceMarkers(), this.getPlannedAbsences()]);
                safeApply(this.$scope);
            }
        });

        this.$scope.$on(EVENTS_FORM.SUBMIT, async (event: IAngularEvent, filter: EventsFilter) => {
            this.filter = {
                startDate: this.filter.startDate,
                endDate: this.filter.endDate,
                timeslots: filter.timeslots,
                students: filter.students,
                classes: filter.classes,
                halfBoarders: filter.halfBoarders,
                interns: filter.interns,
                regularized: filter.regularized,
                notRegularized: filter.notRegularized,
                noReasons: filter.noReasons,
                allAbsenceReasons: filter.allAbsenceReasons,
                reasonIds: filter.reasonIds,
                followed: filter.followed,
                notFollowed: filter.notFollowed,
                page: 0
            };

            await Promise.all([this.getAbsenceMarkers(), this.getPlannedAbsences()]);
            safeApply(this.$scope);
        });

        this.$scope.$on(EVENTS_SEARCH.STUDENT, (event: IAngularEvent, students: Array<Student>) => {
            this.filter.students = students;
            this.getPlannedAbsences();
            safeApply(this.$scope);
        });

        this.$scope.$on(EVENTS_SEARCH.GROUP, (event: IAngularEvent, groups: Array<Group>) => {
            this.filter.classes = groups;
            this.getPlannedAbsences();
            safeApply(this.$scope);
        });

        safeApply(this.$scope);
    }

    $onDestroy(): void {
        this.$scope.$emit(EVENTS_DATE.ABSENCES_SAVE,
                {startDate: this.filter.startDate, endDate: this.filter.endDate});
    }

    async onScroll(): Promise<void> {
        this.filter.page++;
        let absences: Array<Absence> = await this.fetchAbsences();

        if (absences && absences.length !== 0) {
            this.plannedAbsences = this.plannedAbsences.concat(absences);
            this.$scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
        }
        safeApply(this.$scope);
    }

    async getAbsenceMarkers(): Promise<void> {
        this.absencesMarkers = await this.absenceService.getAbsenceMarkers(window.structure.id,
            moment(DateUtils.getCurrentDate(DateUtils.FORMAT['YEAR-MONTH-DAY'])).startOf('day')
                .format(DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']),
            moment(DateUtils.getCurrentDate(DateUtils.FORMAT['YEAR-MONTH-DAY'])).endOf('day')
                .format(DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC'])
        );
    }

    async initFilter(): Promise<void> {
        let formFilterPref: EventsFormFilter = await Me.preference(PresencesPreferenceUtils.PREFERENCE_KEYS.PRESENCE_PLANNED_ABSENCES_FILTER);
        formFilterPref = formFilterPref ? formFilterPref[window.structure.id] : null;
        this.filter = {
            startDate: moment().startOf('day').toDate(),
            endDate: DateUtils.add(new Date(), 1, 'M'),
            timeslots: {
                start: {name: '', startHour: '', endHour: '', id: ''},
                end: {name: '', startHour: '', endHour: '', id: ''}
            },
            students: formFilterPref ? formFilterPref.students : [],
            classes: formFilterPref ? formFilterPref.classes : [],
            halfBoarders: formFilterPref ? formFilterPref.halfBoarders : false,
            interns: formFilterPref ? formFilterPref.interns : false,
            regularized: formFilterPref ? formFilterPref.regularized : true,
            allAbsenceReasons: formFilterPref ? formFilterPref.allAbsenceReasons : true,
            noReasons: formFilterPref ? formFilterPref.noReasons : true,
            reasonIds: formFilterPref ? formFilterPref.reasonIds : [],
            notRegularized: formFilterPref ? formFilterPref.notRegularized : true,
            followed: formFilterPref ? formFilterPref.followed : true,
            notFollowed: formFilterPref ? formFilterPref.notFollowed : true,
            page: 0
        };

        if((this.filter.regularized || this.filter.notRegularized) && !this.filter.reasonIds.length) {
            this.reasons.forEach((reasons: Reason) => reasons.isSelected)
            this.filter.reasonIds = [...this.reasons.map((reason: Reason) => reason.id)]
        }

        let parentVm: any = this.$scope.$parent.$parent['vm'];
        if (parentVm && parentVm.groupsSearch && parentVm.studentsSearch
            && parentVm.groupsSearch.structureId && parentVm.studentsSearch.structureId) {
            this.filter.classes = parentVm.groupsSearch.getSelectedGroups();
            this.filter.students = parentVm.studentsSearch.getSelectedStudents();
        }
    }

    fetchAbsences(): Promise<Array<Absence>> {
        let studentIds: Array<string> = this.filter.students.map((student: Student) => student.id);
        let groupIds: Array<string> = this.filter.classes.map((group: Group) => group.id);
        let regularized: boolean = (this.filter.noReasons && !this.filter.regularized
            && !this.filter.notRegularized) || (this.filter.regularized && this.filter.notRegularized) ?
            null : this.filter.regularized;
        let followed: boolean = (this.filter.followed && this.filter.notFollowed) ? null : this.filter.followed;
        let startAt: string = DateUtils.format(this.filter.startDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]) + " " +
            (!!this.filter.timeslots && !!this.filter.timeslots.start && !!this.filter.timeslots.start.startHour ?
                this.filter.timeslots.start.startHour + ":00" : DateUtils.START_DAY_TIME);
        let endAt: string = DateUtils.format(this.filter.endDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]) + " " +
            (!!this.filter.timeslots && !!this.filter.timeslots.end && !!this.filter.timeslots.end.endHour ?
                this.filter.timeslots.end.endHour + ":00" : DateUtils.END_DAY_TIME);


        return this.absenceService.getAbsencesPaginated(window.structure.id, studentIds, groupIds,
            startAt,
            endAt,
            null, regularized, followed,
            this.filter.reasonIds, this.filter.noReasons, this.filter.halfBoarders,
            this.filter.interns, this.filter.page);
    }

    async getPlannedAbsences(): Promise<void> {
        this.loading = true;
        try {
            this.filter.page = 0;
            this.plannedAbsences = await this.fetchAbsences();
        } catch (err) {
            throw err;
        } finally {
            this.loading = false;
            safeApply(this.$scope);
        }
    }

    async updateDate(): Promise<void> {
        if (this.filter.startDate && this.filter.endDate) {
            await Promise.all([this.getAbsenceMarkers(), this.getPlannedAbsences()]);
            safeApply(this.$scope);
        }
    }

    export(exportType: ExportType): void {
        let studentIds: Array<string> = this.filter.students.map((student: Student) => student.id);
        let groupIds: Array<string> = this.filter.classes.map((group: Group) => group.id);
        let regularized: boolean = (this.filter.noReasons && !this.filter.regularized
            && !this.filter.notRegularized) || (this.filter.regularized && this.filter.notRegularized) ?
            null : this.filter.regularized;
        let followed: boolean = (this.filter.followed && this.filter.notFollowed) ? null : this.filter.followed;
        let startAt: string = DateUtils.format(this.filter.startDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]) + " " +
            (!!this.filter.timeslots && !!this.filter.timeslots.start && !!this.filter.timeslots.start.startHour ?
                this.filter.timeslots.start.startHour + ":00" : DateUtils.START_DAY_TIME);
        let endAt: string = DateUtils.format(this.filter.endDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]) + " " +
            (!!this.filter.timeslots && !!this.filter.timeslots.end && !!this.filter.timeslots.end.endHour ?
                this.filter.timeslots.end.endHour + ":00" : DateUtils.END_DAY_TIME);

        this.absenceService.export(exportType, window.structure.id,
            startAt,
            endAt, studentIds,
            groupIds, regularized, followed, this.filter.reasonIds, this.filter.noReasons,
            this.filter.halfBoarders, this.filter.interns);
    }

}

export const plannedAbsencesController = ng.controller('PlannedAbsencesController',
    ['$scope', '$location', '$window', 'AbsenceService', 'ReasonService', Controller]);