import {
    incidentsTypeService,
    IncidentTypeRequest,
    PartnerRequest,
    partnerService,
    PlaceRequest,
    placeService,
    ProtagonistTypeRequest,
    protagonistTypeService,
    punishmentsTypeService,
    SeriousnessRequest,
    seriousnessService
} from "@incidents/services";
import {
    INCIDENTS_PARTNER_EVENT,
    INCIDENTS_PLACE_EVENT,
    INCIDENTS_PROTAGONIST_TYPE_EVENT,
    INCIDENTS_PUNISHMENT_TYPE_EVENT,
    INCIDENTS_SERIOUSNESS_EVENT,
    INCIDENTS_TYPE_EVENT
} from "@common/core/enum/incidents-event";
import {AxiosResponse} from "axios";
import {IPunishmentTypeBody} from "../../models/PunishmentType";
import {IPunishmentCategory} from "@incidents/models/PunishmentCategory";
import {IAngularEvent} from "angular";
import {toasts} from "entcore";

console.log("Incident manage sniplet");

interface ViewModel {
    safeApply(fn?: () => void): void;

    header: string;
    description: string;
    event: any;
    seriousnessMode: boolean;
    punishmentMode: boolean;
    form: IncidentTypeRequest | PlaceRequest | PartnerRequest | ProtagonistTypeRequest | SeriousnessRequest | IPunishmentTypeBody;
    punishmentCategory: Array<IPunishmentCategory>;
    editIncidentsManageLightbox: boolean;

    openIncidentsManageLightbox(event, args): void;

    closeIncidentsManageLightbox(): void;

    updateIncidentsManageLightbox(): Promise<void>;

    proceedAfterAction(response: AxiosResponse): void;

    chooseLevel(level: number): void;
}

const vm: ViewModel = {
    safeApply: null,
    header: '',
    description: '',
    seriousnessMode: false,
    punishmentMode: false,
    event: null,
    editIncidentsManageLightbox: null,
    form: {} as IncidentTypeRequest | PlaceRequest | PartnerRequest | ProtagonistTypeRequest | SeriousnessRequest | IPunishmentTypeBody | IPunishmentCategory,
    punishmentCategory: [],

    openIncidentsManageLightbox(event, args): void {
        vm.editIncidentsManageLightbox = true;
        vm.form = {} as IncidentTypeRequest | PlaceRequest | PartnerRequest | ProtagonistTypeRequest | SeriousnessRequest | IPunishmentTypeBody | IPunishmentCategory;
        vm.seriousnessMode = false;
        vm.punishmentMode = false;
        switch (event.name) {
            case INCIDENTS_TYPE_EVENT.TRANSMIT:
                vm.header = 'incident.type.form.input.edit';
                vm.description = 'incident.type.form.input.edit.warning';
                break;
            case INCIDENTS_PARTNER_EVENT.TRANSMIT:
                vm.header = 'incident.partner.form.input.edit';
                vm.description = 'incident.partner.form.input.edit.warning';
                break;
            case INCIDENTS_PLACE_EVENT.TRANSMIT:
                vm.header = 'incident.place.form.input.edit';
                vm.description = 'incident.place.form.input.edit.warning';
                break;
            case INCIDENTS_PROTAGONIST_TYPE_EVENT.TRANSMIT:
                vm.header = 'incident.protagonist.type.form.input.edit';
                vm.description = 'incident.protagonist.type.form.input.edit.warning';
                break;
            case INCIDENTS_SERIOUSNESS_EVENT.TRANSMIT:
                vm.header = 'incident.seriousness.form.input.edit';
                vm.description = 'incident.seriousness.form.input.edit.warning';
                (<SeriousnessRequest>vm.form).level = args.level;
                (<SeriousnessRequest>vm.form).excludeAlertSeriousness = args.exclude_alert_seriousness;
                vm.seriousnessMode = true;
                break;
            case INCIDENTS_PUNISHMENT_TYPE_EVENT.TRANSMIT:
                vm.header = 'presences.punishments.form.input.edit';
                vm.description = 'presences.punishments.form.input.edit.warning';
                vm.punishmentCategory = args.punishmentCategory;
                (<IPunishmentTypeBody>vm.form).type = args.punishmentType.type;
                (<IPunishmentTypeBody>vm.form).punishment_category_id = args.punishmentType.punishment_category_id;
                (<IPunishmentTypeBody>vm.form).id = args.punishmentType.id;
                (<IPunishmentTypeBody>vm.form).label = args.punishmentType.label;
                (<IPunishmentTypeBody>vm.form).hidden = args.punishmentType.hidden;
                vm.punishmentMode = true;
                vm.event = event;
                console.log("EDIT: ", args);
                return;
        }
        /* Assign form to current data */
        vm.form.id = args.id;
        vm.form.label = args.label;
        vm.form.hidden = args.hidden;
        vm.event = event;
    },

    closeIncidentsManageLightbox(): void {
        vm.editIncidentsManageLightbox = false;
    },

    async updateIncidentsManageLightbox(): Promise<void> {
        switch (vm.event.name) {
            case INCIDENTS_TYPE_EVENT.TRANSMIT: {
                let response = await incidentsTypeService.update(vm.form);
                vm.proceedAfterAction(response);
                incidentsManageLightbox.that.$emit(INCIDENTS_TYPE_EVENT.SEND_BACK);
                break;
            }
            case INCIDENTS_PARTNER_EVENT.TRANSMIT: {
                let response = await partnerService.update(vm.form);
                vm.proceedAfterAction(response);
                incidentsManageLightbox.that.$emit(INCIDENTS_PARTNER_EVENT.SEND_BACK);
                break;
            }
            case INCIDENTS_PLACE_EVENT.TRANSMIT: {
                let response = await placeService.update(vm.form);
                vm.proceedAfterAction(response);
                incidentsManageLightbox.that.$emit(INCIDENTS_PLACE_EVENT.SEND_BACK);
                break;
            }
            case INCIDENTS_PROTAGONIST_TYPE_EVENT.TRANSMIT: {
                let response = await protagonistTypeService.update(vm.form);
                vm.proceedAfterAction(response);
                incidentsManageLightbox.that.$emit(INCIDENTS_PROTAGONIST_TYPE_EVENT.SEND_BACK);
                break;
            }
            case INCIDENTS_SERIOUSNESS_EVENT.TRANSMIT: {
                let response = await seriousnessService.update(vm.form);
                vm.proceedAfterAction(response);
                incidentsManageLightbox.that.$emit(INCIDENTS_SERIOUSNESS_EVENT.SEND_BACK);
                break;
            }
            case INCIDENTS_PUNISHMENT_TYPE_EVENT.TRANSMIT: {
                let response = await punishmentsTypeService.update(vm.form);
                vm.proceedAfterAction(response);
                incidentsManageLightbox.that.$emit(INCIDENTS_PUNISHMENT_TYPE_EVENT.SEND_BACK);
                break;
            }

        }
    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            switch (vm.event.name) {
                case INCIDENTS_PUNISHMENT_TYPE_EVENT.TRANSMIT: {
                    toasts.confirm('presences.punishments.type.setting.method.update.confirm');
                    break;
                }
            }
            vm.editIncidentsManageLightbox = false;
        } else {
            switch (vm.event.name) {
                case INCIDENTS_PUNISHMENT_TYPE_EVENT.TRANSMIT: {
                    toasts.warning('presences.punishments.type.setting.method.update.error');
                    break;
                }
            }
        }
    },

    chooseLevel(level: number): void {
        (<SeriousnessRequest>vm.form).level = level;
    },
};

export const incidentsManageLightbox = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            incidentsManageLightbox.that = this;
            vm.safeApply = this.safeApply;
        },
        setHandler: function () {
            this.$on(INCIDENTS_TYPE_EVENT.TRANSMIT, (event: IAngularEvent, args) => vm.openIncidentsManageLightbox(event, args));
            this.$on(INCIDENTS_PARTNER_EVENT.TRANSMIT, (event: IAngularEvent, args) => vm.openIncidentsManageLightbox(event, args));
            this.$on(INCIDENTS_PLACE_EVENT.TRANSMIT, (event: IAngularEvent, args) => vm.openIncidentsManageLightbox(event, args));
            this.$on(INCIDENTS_PROTAGONIST_TYPE_EVENT.TRANSMIT, (event: IAngularEvent, args) => vm.openIncidentsManageLightbox(event, args));
            this.$on(INCIDENTS_SERIOUSNESS_EVENT.TRANSMIT, (event: IAngularEvent, args) => vm.openIncidentsManageLightbox(event, args));
            this.$on(INCIDENTS_PUNISHMENT_TYPE_EVENT.TRANSMIT, (event: IAngularEvent, args) => vm.openIncidentsManageLightbox(event, args));
        }
    }
};