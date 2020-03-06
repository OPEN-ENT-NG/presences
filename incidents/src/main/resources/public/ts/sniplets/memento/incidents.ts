import {angular, moment, toasts} from "entcore";
import {DateUtils} from "@common/utils";
import {Incidents} from "@incidents/models";

declare let window: any;

interface IViewModel {
    startDate: Date;
    endDate: Date;
    schoolYears: Array<any>;
    disabled: boolean,
    student: string,
    incidents: Incidents;

    apply(): void;

    init(student: string): Promise<void>;

    loadStudentYearIncidents(): Promise<void>;

    formatIncident(date: string): void;
}

const vm: IViewModel = {
    startDate: null,
    endDate: null,
    schoolYears: [],
    student: null,
    incidents: undefined,
    disabled: false,

    apply: null,

    async init(student: string): Promise<void> {
        try {
            vm.incidents = new Incidents();
            vm.incidents.eventer.on('loading::true', vm.apply);
            vm.incidents.eventer.on('loading::false', vm.apply);
            vm.incidents.userId = student;
            vm.incidents.structureId = window.structure.id;
            const schoolYears = await DateUtils.getSchoolYearDates(window.structure.id);
            vm.startDate = moment(schoolYears.start_date);
            vm.endDate = moment(schoolYears.end_date);
            await vm.loadStudentYearIncidents();
            if (!schoolYears.id) {
                vm.disabled = true;
                vm.apply();
                return;
            }
            vm.disabled = false;
        } catch (e) {
            toasts.warning('presences.memento.incident.init.failed');
            throw e
        }
    },
    async loadStudentYearIncidents(): Promise<void> {
        try {
            vm.incidents.startDate = vm.startDate.format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            vm.incidents.endDate = vm.endDate.format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            vm.incidents.page = 0;
        } catch (err) {
            throw err;
        }
    },

    formatIncident(date: string): string {
        return moment(date).format('DD/MM/YYYY');
    }
};


export const incidentsMementoWidget = {
    title: 'presences.memento.incidents.title',
    public: false,
    controller: {
        init: function () {
            this.vm = vm;
            this.setHandler();
        },
        setHandler: function () {
            console.log('MEMENTO incidents');
            if (!window.memento) return;
            this.$on('memento:init', (evt, {student}) => {
                const sniplet = document.getElementById('memento-incidents-sniplet');
                vm.apply = angular.element(sniplet).scope().$apply;
                vm.init(student);
            });
        }
    }
};