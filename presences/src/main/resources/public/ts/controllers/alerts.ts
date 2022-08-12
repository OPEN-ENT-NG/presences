import {_, init, Me, model, moment, ng, toasts} from 'entcore';
import {AlertService, alertService, Group, IViescolaireService} from "../services";
import {AlertType, ISchoolYearPeriod, Student} from "../models";
import {SearchService} from "@common/services/SearchService";
import {GroupService} from "@common/services/GroupService";
import rights from "../rights";
import {Alert, DeleteAlertRequest, StudentAlert} from "@presences/models/Alert";
import {DateUtils, PreferencesUtils, safeApply} from "@common/utils";
import {ILocationService, IScope} from "angular";
import {IRouting} from "@common/model/Route";

declare let window: any;

interface Filter {
    types: {
        ABSENCE?: boolean;
        LATENESS?: boolean;
        FORGOTTEN_NOTEBOOK?: boolean;
        INCIDENT?: boolean;
    };
    student: string;
    students: any[];
    class: string;
    classes: any[];
    selected: { students: any[], classes: any[] }
    startDate?: Date,
    endDate?: Date;
}

interface ViewModel {
    alerts: Alert;
    filters: string[];
    alertType: string[];
    filter: Filter;
    listAlert: Array<StudentAlert>;
    selection: { all: boolean };

    params: {
        loading: boolean,
        type: string
    };

    getStudentAlert(students?: string[], classes?: string[]): Promise<void>;

    searchStudent(value: string): Promise<void>;

    selectStudent(): (model: any, student: any) => Promise<void>;

    searchClass(value: string): Promise<void>;

    selectClass(): (model: any, classObject: any) => Promise<void>;

    dropFilter(object: any, list: any): void;

    /*  switch alert type */
    switchFilter(filter: string): Promise<void>;

    someSelectedAlert(): boolean;

    selectAll(): void;

    toggleAlert(alert: StudentAlert): void;

    reset(): Promise<void>;

    exportAlertCSV(): void;
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

    constructor(private $scope: IScope,
                private $route: IRouting,
                private $location: ILocationService,
                private searchService: SearchService,
                private viescolaireService: IViescolaireService,
                private alertService: AlertService,
                private groupService: GroupService) {
        this.$scope['vm'] = this;
        console.log('AlertsController');
        this.filters =
            [AlertType[AlertType.ABSENCE], AlertType[AlertType.LATENESS], AlertType[AlertType.INCIDENT], AlertType[AlertType.FORGOTTEN_NOTEBOOK]];
        this.filter = {
            student: '',
            class: '',
            students: undefined,
            classes: undefined,
            selected: {
                students: [],
                classes: [],
            },
            types: {},
            startDate: undefined,
            endDate: new Date(),
        };
        this.alertType = [];
        this.selection = {all: false};

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
            window.structure = await Me.preference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_STRUCTURE);
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
            await this.getStudentAlert();
        }
    }

    initFilter(value: boolean): void {
        this.filters.forEach((filter: string) => this.filter.types[filter] = value);
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
                const start_at: string = moment(this.filter.startDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                const end_at: string = moment(this.filter.endDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                this.listAlert = await this.alertService.getStudentsAlerts(window.structure.id, this.alertType, students, classes, start_at, end_at);
            } else {
                this.listAlert = [];
            }
        } catch (e) {
            toasts.warning('presences.error.get.alert');
            throw e;
        }
        this.params.loading = false;
        safeApply(this.$scope);
    }

    async searchStudent(value: string): Promise<void> {
        const that: Controller = this.$parent.vm
        const structureId = window.structure.id;
        try {
            that.filter.students = await that.searchService.searchUser(structureId, value, 'Student');
            safeApply(that.$scope);
        } catch (err) {
            that.filter.students = [];
            throw err;
        }
    }

    selectStudent(): (model: any, student: any) => Promise<void> {
        const that: Controller = this
        return async (model: any, student: any): Promise<void> => {
            if (_.findWhere(that.filter.selected.students, {id: student.id})) {
                return;
            }
            that.filter.selected.students.push(student);
            that.filter.student = '';
            that.filter.students = undefined;
            await that.getStudentAlert(that.extractSelectedStudentIds(), that.extractSelectedGroupsName());
            safeApply(that.$scope);
        }
    }

    async searchClass(value: string): Promise<void> {
        const that: Controller = this.$parent.vm
        const structureId = window.structure.id;
        try {
            that.filter.classes = await that.groupService.search(structureId, value);
            that.filter.classes.map((obj) => obj.toString = () => obj.name);
            safeApply(that.$scope);
        } catch (err) {
            that.filter.classes = [];
            throw err;
        }
        return;
    }

    selectClass(): (model: any, classObject: any) => Promise<void> {
        const that: Controller = this;
        return  async (model: any, classObject: any): Promise<void> => {
            if (_.findWhere(that.filter.selected.students, {id: classObject.id})) {
                return;
            }
            that.filter.selected.classes.push(classObject);
            that.filter.class = '';
            that.filter.classes = undefined;
            await that.getStudentAlert(that.extractSelectedStudentIds(), that.extractSelectedGroupsName());
            safeApply(that.$scope);
        }

    }

    dropFilter(object: any, list: any): void {
        this.filter.selected[list] = _.without(this.filter.selected[list], object);
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
        this.filter.selected.students.map((student: Student) => ids.push(student.id));
        return ids;
    };

    extractSelectedGroupsName(): string[] {
        const ids: string[] = [];
        if (model.me.hasWorkflow(rights.workflow.search)) {
            this.filter.selected.classes.map((group: Group) => ids.push(group.id));
        }
        return ids;
    };
}

export const alertsController = ng.controller('AlertsController',
    ['$scope', '$route', '$location',
        'SearchService', 'ViescolaireService', 'AlertService', "GroupService", Controller]);