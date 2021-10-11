import {angular, idiom as lang, moment, toasts} from "entcore";
import {DateUtils} from "@common/utils";
import {Incident, IPunishment, IPunishmentRequest, ISchoolYearPeriod, ProtagonistForm} from "@incidents/models";
import {ViescolaireService} from "@common/services/ViescolaireService";
import {IAngularEvent} from "angular";
import {incidentService, punishmentService, punishmentsTypeService} from "@incidents/services";
import {IPunishmentType} from "@incidents/models/PunishmentType";
import {EVENT_TYPES} from "@common/model";
import {User} from "@common/model/User";
import {PunishmentsUtils} from "@incidents/utilities/punishments";

console.log("memento incidents/punishment");

declare let window: any;

interface IMementoIncident {
    type: string,
    item: Incident | IPunishment
}

interface IViewModel {
    startDate: Date;
    endDate: Date;
    schoolYears: Array<any>;
    disabled: boolean,
    student: string,
    punishments: IPunishment;
    punishmentsRequest: IPunishmentRequest;
    punishmentsTypes: Array<IPunishmentType>;
    mementoIncidents: Array<IMementoIncident>;
    filter: { incident: boolean, punishment: boolean, sanction: boolean };

    apply(): void;

    init(student: string): Promise<void>;

    loadStudentYearIncidents(): Promise<void>;

    formatDate(mementoItem: IMementoIncident): string;

    setIPunishmentRequest(): void;

    redirectTo(mementoIncident: IMementoIncident): void;

    isPunishment(type: string): boolean;

    getPunishmentType(): Promise<void>;

    filterPunishments(): Array<number>;

    getCategory(type: string, mementoIncident: IMementoIncident): string;

    getType(type: string, mementoIncident: IMementoIncident): string;
}

const vm: IViewModel = {
    startDate: null,
    endDate: null,
    schoolYears: [],
    student: null,
    punishments: null,
    punishmentsRequest: {} as IPunishmentRequest,
    punishmentsTypes: [],
    disabled: false,
    apply: null,
    mementoIncidents: [],
    filter: {incident: true, punishment: true, sanction: true},

    async init(student: string): Promise<void> {
        try {
            vm.student = student;
            vm.getPunishmentType();
            const schoolYears: ISchoolYearPeriod = await ViescolaireService.getSchoolYearDates(window.structure.id);
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

    setIPunishmentRequest(): void {
        vm.punishmentsRequest.structure_id = window.structure.id;
        vm.punishmentsRequest.start_at = vm.startDate.format(DateUtils.FORMAT["YEAR-MONTH-DAY"]) + ' 00:00:00';
        vm.punishmentsRequest.end_at = vm.endDate.format(DateUtils.FORMAT["YEAR-MONTH-DAY"]) + ' 23:59:59';
        vm.punishmentsRequest.students_ids = [vm.student];
        vm.punishmentsRequest.groups_ids = null;
        vm.punishmentsRequest.type_ids = vm.filterPunishments();
        vm.punishmentsRequest.process_state = null;
        vm.punishmentsRequest.page = 0;
    },

    async loadStudentYearIncidents(): Promise<void> {
        const promises: Promise<any>[] = [];
        if (vm.filter.incident) {
            promises.push(incidentService.getStudentIncidentsSummary(vm.student, window.structure.id,
                vm.startDate.format(DateUtils.FORMAT["YEAR-MONTH-DAY"]), vm.endDate.format(DateUtils.FORMAT["YEAR-MONTH-DAY"])
            ));
        }
        if (vm.filter.punishment || vm.filter.sanction) {
            vm.setIPunishmentRequest();
            promises.push(punishmentService.get(vm.punishmentsRequest));
        }
        if (promises.length === 0) {
            vm.mementoIncidents = [];
        }
        Promise.all(promises).then((mementoResponses: any[]) => {
            let mementos: any[] = [];

            let mementoIncidents: Array<IMementoIncident> = [];

            mementoResponses.forEach(mementoResponse => mementos = [...mementos, ...mementoResponse.all]);
            mementos.forEach(memento => {
                // Case if it's a punishment
                if ('fields' in memento) {
                    let punishment: IMementoIncident = {
                        type: EVENT_TYPES.PUNISHMENT,
                        item: memento
                    }
                    mementoIncidents.push(punishment);
                } else {
                    // Case if it's an incident
                    let incident: IMementoIncident = {type: EVENT_TYPES.INCIDENT, item: memento}
                    mementoIncidents.push(incident);
                }
            });
            vm.mementoIncidents = mementoIncidents;
            vm.apply();
        })
    },

    formatDate: (mementoItem: IMementoIncident): string => mementoItem.type === EVENT_TYPES.INCIDENT ?
        moment((<Incident>mementoItem.item).date).format(DateUtils.FORMAT["DAY-MONTH-YEAR"]) :
        PunishmentsUtils.getPunishmentDate(<IPunishment>mementoItem.item),

    redirectTo(mementoIncident: IMementoIncident): void {
        if (mementoIncident.type === EVENT_TYPES.INCIDENT) {
            let protagonists: ProtagonistForm[] = (<Incident>mementoIncident.item).protagonists;
            const protagonist: ProtagonistForm = protagonists.find(student => student.user_id === vm.student);
            window.location.href = `/incidents#/incidents?mementoStudentId=${protagonist.student.idEleve}&mementoStudentName=${protagonist.student.displayName}`;
        } else if (mementoIncident.type === (EVENT_TYPES.PUNISHMENT || EVENT_TYPES.SANCTION)) {
            const student: User = {
                displayName: (<IPunishment>mementoIncident.item).student.name,
                id: (<IPunishment>mementoIncident.item).student.id
            };
            window.location.href = `/incidents#/punishment/sanction?mementoStudentId=${student.id}&mementoStudentName=${student.displayName}`;
        }
        if (window.location.href === `/incidents#/incidents` || `/incidents#/punishment/sanction`) {
            window.location.reload();
        }
        window.memento.close();
        vm.apply();
    },

    isPunishment(type: string): boolean {
        return type === (EVENT_TYPES.PUNISHMENT || EVENT_TYPES.SANCTION);
    },

    async getPunishmentType(): Promise<void> {
        vm.punishmentsTypes = await punishmentsTypeService.get(window.structure.id);
    },

    filterPunishments(): Array<number> {
        let punishment: Array<number> = [];
        let sanction: Array<number> = [];
        if (vm.filter.punishment) {
            punishment = vm.punishmentsTypes.filter((t: IPunishmentType) => t.type === EVENT_TYPES.PUNISHMENT).map((p: IPunishmentType) => p.id);
        }
        if (vm.filter.sanction) {
            sanction = vm.punishmentsTypes.filter((t: IPunishmentType) => t.type === EVENT_TYPES.SANCTION).map((p: IPunishmentType) => p.id);
        }
        return [...punishment, ...sanction];
    },

    getCategory(type: string, mementoIncident: IMementoIncident): string {
        if (type === EVENT_TYPES.INCIDENT) {
            return lang.translate("incidents.incident");
        } else if (type === EVENT_TYPES.PUNISHMENT) {
            if ((<IPunishment>mementoIncident.item).type.type === EVENT_TYPES.PUNISHMENT) {
                return lang.translate("presences.punishment");
            }
            if ((<IPunishment>mementoIncident.item).type.type === EVENT_TYPES.SANCTION) {
                return lang.translate("presences.sanction");
            }
        }
    },

    getType(type: string, mementoIncident: IMementoIncident): string {
        switch (type) {
            case EVENT_TYPES.INCIDENT:
                return (<Incident>mementoIncident.item).incident_type.label;
            case EVENT_TYPES.PUNISHMENT || EVENT_TYPES.SANCTION:
                return (<IPunishment>mementoIncident.item).type.label;
        }
    }
};

export const incidentsMementoWidget = {
    title: 'presences.memento.incidents.title',
    public: false,
    that: null,
    controller: {
        init: function () {
            this.vm = vm;
            incidentsMementoWidget.that = this;
            this.setHandler();
        },
        setHandler: function () {
            if (!window.memento) return;
            this.$on('memento:init', (evt: IAngularEvent, {student}) => {
                const sniplet = document.getElementById('memento-incidents-sniplet');
                vm.apply = angular.element(sniplet).scope().$apply;
                vm.init(student);
            });
        }
    }
};