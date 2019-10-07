import {Partner, PartnerRequest, partnerService} from "@incidents/services";
import {AxiosResponse} from "axios";
import {INCIDENTS_PARTNER_EVENT} from "@common/enum/incidents-event";

declare let window: any;

interface ViewModel {
    safeApply(fn?: () => void): void;
    form: PartnerRequest;
    partners: Partner[];

    hasPartners(): boolean;
    proceedAfterAction(response: AxiosResponse): void;

    get(): Promise<void>;
    create(): Promise<void>;
    toggleVisibility(partner: Partner): Promise<void>;
    delete(partner: Partner): Promise<void>

    openIncidentsManageLightbox(partner: Partner): void;
}

function safeApply() {
    let that = incidentsPartnersManage.that;
    return new Promise((resolve, reject) => {
        var phase = that.$root.$$phase;
        if(phase === '$apply' || phase === '$digest') {
            if(resolve && (typeof(resolve) === 'function')) resolve();
        } else {
            if (resolve && (typeof(resolve) === 'function')) that.$apply(resolve);
            else that.$apply();
        }
    });
}

const vm: ViewModel = {
    safeApply: null,
    partners: [],
    form: {} as PartnerRequest,

    async create(): Promise<void> {
        vm.form.structureId = window.model.vieScolaire.structure.id;
        let response = await partnerService.create(vm.form);
        vm.proceedAfterAction(response);
        vm.form.label = '';
    },

    async delete(partner: Partner): Promise<void> {
        let response = await partnerService.delete(partner.id);
        vm.proceedAfterAction(response);
    },

    hasPartners(): boolean {
        return vm.partners && vm.partners.length !== 0;
    },

    async get(): Promise<void> {
        vm.partners = await partnerService.get(window.model.vieScolaire.structure.id);
        vm.safeApply();
    },

    async toggleVisibility(partner: Partner): Promise<void> {
        partner.hidden = !partner.hidden;
        let form = {} as PartnerRequest;
        form.id = partner.id;
        form.hidden = partner.hidden;
        form.label = partner.label;
        await partnerService.update(form);
    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.get();
        }
    },

    openIncidentsManageLightbox(partner: Partner): void {
        incidentsPartnersManage.that.$emit(INCIDENTS_PARTNER_EVENT.SEND, partner);
    },
};

export const incidentsPartnersManage = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            incidentsPartnersManage.that = this;
            vm.safeApply = safeApply;
        },
        setHandler: function () {
            // using vieScolaire.structure to update current structure from viescolaire
            this.$watch(() =>  window.model.vieScolaire.structure, async () => vm.get());
            this.$on(INCIDENTS_PARTNER_EVENT.RESPOND, () => vm.get());
        }
    }
};