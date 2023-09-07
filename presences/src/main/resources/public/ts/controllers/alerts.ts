import {_, init, Me, model, moment, ng, toasts} from 'entcore';
import {AlertService, alertService, Group, GroupingService, IViescolaireService} from "../services";
import {AlertType, ISchoolYearPeriod, Student, User} from "../models";
import {SearchService} from "@common/services/SearchService";
import {GroupService} from "@common/services/GroupService";
import rights from "../rights";
import {Alert, DeleteAlertRequest, InfiniteScrollAlert, StudentAlert} from "@presences/models/Alert";
import {DateUtils, GroupsSearch, PreferencesUtils, safeApply, StudentsSearch, UsersSearch} from "@common/utils";
import {ILocationService, IScope} from "angular";
import {IRouting} from "@common/model/Route";
import {INFINITE_SCROLL_EVENTER} from '@common/core/enum/infinite-scroll-eventer';


declare let window: any;

interface Filter {
    types: {
        ABSENCE?: boolean;
        LATENESS?: boolean;
        FORGOTTEN_NOTEBOOK?: boolean;
        INCIDENT?: boolean;
    };
    startDate?: Date,
    endDate?: Date,
    page?:number;
}

interface ViewModel {
    alerts: Alert;
    filters: string[];
    alertType: string[];
    filter: Filter;
    listAlert: Array<StudentAlert>;
    selection: { all: boolean };
    groupsSearch: GroupsSearch;
    studentsSearch: StudentsSearch;

    params: {
        loading: boolean,
        type: string
    };

    getStudentAlert(students?: string[], classes?: string[]): Promise<void>;

    updateDates(dates?: { startDate: Date, endDate: Date }): Promise<void>;

    searchStudent(value: string): Promise<void>;

    selectStudent(): (model: any, student: any) => Promise<void>;

    searchClass(value: string): Promise<void>;

    selectClass(): (model: any, classObject: any) => Promise<void>;

    removeSelectedGroup(groupItem: Group): void;

    removeSelectedStudent(studentItem: User): void;

    /*  switch alert type */
    switchFilter(filter: string): Promise<void>;

    someSelectedAlert(): boolean;

    selectAll(): void;

    toggleAlert(alert: StudentAlert): void;

    reset(): Promise<void>;

    exportAlertCSV(): void;

    onScroll(): void;
}

class Controller implements ng.IController, ViewModel {
    $parent: any;
    alerts: Alert;
    filters: string[];
    alertType: string[];
    filter: Filter;
    listAlert: StudentAlert[];
    selection: { all: boolean; };
    params: { loading: boolean; type: string; };
    groupsSearch: GroupsSearch;
    studentsSearch: StudentsSearch;
    pageCount: number;
    alertsRes:InfiniteScrollAlert

    constructor(private $scope: IScope,
                private $route: IRouting,
                private $location: ILocationService,
                private searchService: SearchService,
                private viescolaireService: IViescolaireService,
                private alertService: AlertService,
                private groupService: GroupService,
                private groupingService: GroupingService) {
        this.$scope['vm'] = this;
        console.log('AlertsController');
        this.filters =
            [AlertType[AlertType.ABSENCE], AlertType[AlertType.LATENESS], AlertType[AlertType.INCIDENT], AlertType[AlertType.FORGOTTEN_NOTEBOOK]];
        this.filter = {
            types: {},
            startDate: undefined,
            endDate: new Date(),
            page: 0,
        };
        this.alertType = [];
        this.selection = {all: false};
        this.alertsRes = {
            all: [],
            page:0,
            page_count:0,
        }

        /* Fetching information from URL Param and cloning new object RegistryRequest */
        this.params = Object.assign({loading: false}, $location.search());
        this.$scope.$watch(() => window.structure, () => this.initAlert());
    }

    async $onInit(): Promise<void> {
        await this.initAlert().catch(error => console.error(error));
    };

    async initAlert(): Promise<void> {
        this.initFilter(true);
        if (!window.structure) {
            console.log("Alert Ancienne valeur :", window.structure);
            window.structure = await Me.preference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_STRUCTURE);
            console.log("Alert Nouvelle valeur :", window.structure);
        } else {
            if (this.params.type) {
                this.initFilter(false);
                this.filter.types[this.params.type] = true;
            }
            await this.viescolaireService.getSchoolYearDates(window.structure.id)
                .then((schoolYears: ISchoolYearPeriod) => this.filter.startDate = moment(schoolYears.start_date))
                .catch(error => {
                    this.filter.startDate = new Date();
                    console.error(error);
                });
            await this.getStudentAlert()
                .catch(error => console.error(error));
            this.groupsSearch = new GroupsSearch(window.structure.id, this.searchService, this.groupService, this.groupingService);
            this.studentsSearch = new StudentsSearch(window.structure.id, this.searchService);
        }
    }

    initFilter(value: boolean): void {
        this.filters.forEach((filter: string) => this.filter.types[filter] = value);
    };

    updateDates = async (dates?: { startDate: Date, endDate: Date }): Promise<void> => {
        if (dates) {
            this.filter.startDate = dates.startDate;
            this.filter.endDate = dates.endDate;
        }

        await this.getStudentAlert(this.extractSelectedStudentIds(), this.extractSelectedGroupsName());
    };


    async getStudentAlert(students?: string[], classes?: string[]): Promise<void> {
        this.params.loading = true;
        safeApply(this.$scope);
        this.alertType = [];
        Object.keys(this.filter.types).forEach(key => {
            if (this.filter.types[key]) this.alertType.push(key);
        });

        try {
            if (this.alertType.length > 0) {
                this.filter.page = 0;
                const start_at: string = moment(this.filter.startDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                const end_at: string = moment(this.filter.endDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                this.alertsRes = await this.alertService.getStudentsAlerts(window.structure.id, this.alertType, students, classes, start_at, end_at, this.filter.page);
                this.listAlert = this.alertsRes.all;
                this.pageCount = this.alertsRes.page_count;
            } else {
                this.pageCount = 0;
                this.listAlert = [];
            }
        } catch (e) {
            toasts.warning('presences.error.get.alert');
            throw e;
        }
        this.params.loading = false;
        this.$scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
        safeApply(this.$scope);
    }

    async searchStudent(value: string): Promise<void> {
        const that: Controller = this.$parent.vm
        await that.studentsSearch.searchStudents(value);
        safeApply(that.$scope);
    }

    selectStudent(): (model: any, student: any) => Promise<void> {
        const that: Controller = this
        return async (model: any, student: any): Promise<void> => {
            if (_.findWhere(that.studentsSearch.getSelectedStudents(), {id: student.id})) {
                return;
            }
            that.studentsSearch.selectStudents(model, student)
            that.studentsSearch.student = '';
            await that.getStudentAlert(that.extractSelectedStudentIds(), that.extractSelectedGroupsName());
            safeApply(that.$scope);
        }
    }

    async searchClass(value: string): Promise<void> {
        const that: Controller = this.$parent.vm
        await that.groupsSearch.searchGroups(value);
        safeApply(that.$scope);
    }

    selectClass(): (model: any, classObject: any) => Promise<void> {
        const that: Controller = this;
        return  async (model: any, classObject: any): Promise<void> => {
            if (_.findWhere(that.groupsSearch.getSelectedGroups(), {id: classObject.id})) {
                return;
            }
            that.groupsSearch.selectGroups(model, classObject);
            that.groupsSearch.group = "";
            await that.getStudentAlert(that.extractSelectedStudentIds(), that.extractSelectedGroupsName());
            safeApply(that.$scope);
        }

    }

    removeSelectedGroup(groupItem: Group): void {
        this.groupsSearch.removeSelectedGroups(groupItem);
        this.getStudentAlert(this.extractSelectedStudentIds(), this.extractSelectedGroupsName());
    }

    removeSelectedStudent(studentItem: User): void {
        this.studentsSearch.removeSelectedStudents(studentItem);
        this.getStudentAlert(this.extractSelectedStudentIds(), this.extractSelectedGroupsName());
    }

    async switchFilter(filter: string): Promise<void> {
        this.filter.types[filter] = !this.filter.types[filter];
        await this.getStudentAlert();
    }

    someSelectedAlert(): boolean {
        return this.listAlert && this.listAlert.filter((alert:StudentAlert) => alert.selected).length > 0;
    }

    selectAll(): void {
        this.listAlert.forEach((alert:StudentAlert) => alert.selected = this.selection.all);
        window.alerts_item = this.listAlert;
    }

    toggleAlert(alert: StudentAlert): void {
        alert.selected = !alert.selected;
        window.alerts_item = this.listAlert;
    }

    async reset(): Promise<void> {
        try {
            const start_at: string = moment(this.filter.startDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            const end_at: string = moment(this.filter.endDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            const body: DeleteAlertRequest = {
                start_at: start_at,
                end_at: end_at,
                deleted_alert: this.listAlert.filter((alert:StudentAlert) => alert.selected)
            }
            await alertService.reset(window.structure.id, body);
            await this.getStudentAlert();
            toasts.confirm('presences.alert.reset.success');
        } catch (e) {
            toasts.warning('presences.error.reset.alert');
            throw e;
        }
    }

    exportAlertCSV(): void {
        const structureId: string = window.structure.id;
        alertService.exportCSV(structureId, this.alertType);
    }

    extractSelectedStudentIds(): string[] {
        const ids: string[] = [];
        this.studentsSearch.getSelectedStudents().map((student: Student) => ids.push(student.id));
        return ids;
    };

    extractSelectedGroupsName(): string[] {
        const ids: string[] = [];
        if (model.me.hasWorkflow(rights.workflow.search)) {
            this.groupsSearch.getSelectedGroups().map((group: Group) => ids.push(group.id));
        }
        return ids;
    };

    async onScroll(): Promise<void> {
        this.filter.page++;
        if (this.alertType.length > 0) {
            const start_at: string = moment(this.filter.startDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            const end_at: string = moment(this.filter.endDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            this.alertsRes = await this.alertService.getStudentsAlerts(window.structure.id, this.alertType, this.extractSelectedStudentIds(), this.extractSelectedGroupsName(), start_at, end_at, this.filter.page)
            if (this.alertsRes.all.length > 0) {
                this.listAlert = this.listAlert.concat(this.alertsRes.all)
            }
        }
        safeApply(this.$scope);
    }
}

export const alertsController = ng.controller('AlertsController',
    ['$scope', '$route', '$location',
        'SearchService', 'ViescolaireService', 'AlertService', "GroupService", "GroupingService", Controller]);
