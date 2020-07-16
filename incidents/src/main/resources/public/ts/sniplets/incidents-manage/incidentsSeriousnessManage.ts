import {INCIDENTS_SERIOUSNESS_EVENT} from "@common/core/enum/incidents-event";
import {Seriousness, SeriousnessRequest, seriousnessService} from "@incidents/services";
import {AxiosResponse} from "axios";

declare let window: any;

interface ViewModel {
    safeApply(fn?: () => void): void;

    form: SeriousnessRequest;
    seriousnessLevel: number
    seriousnesses: Seriousness[];

    chooseLevel(form): void;

    hasSeriousnesses(): boolean;

    proceedAfterAction(response: AxiosResponse): void;

    get(): Promise<void>;

    create(): Promise<void>;

    toggleVisibility(seriousness: Seriousness): Promise<void>;

    delete(seriousness: Seriousness): Promise<void>

    openIncidentsManageLightbox(seriousness: Seriousness): void;
}

function safeApply() {
    let that = incidentsSeriousnessManage.that;
    return new Promise((resolve, reject) => {
        var phase = that.$root.$$phase;
        if (phase === '$apply' || phase === '$digest') {
            if (resolve && (typeof (resolve) === 'function')) resolve();
        } else {
            if (resolve && (typeof (resolve) === 'function')) that.$apply(resolve);
            else that.$apply();
        }
    });
}

const vm: ViewModel = {
    safeApply: null,
    seriousnesses: [],
    form: {} as SeriousnessRequest,
    seriousnessLevel: 0,

    async create(): Promise<void> {
        vm.form.structureId = window.model.vieScolaire.structure.id;
        vm.form.level = vm.seriousnessLevel;
        let response = await seriousnessService.create(vm.form);
        vm.proceedAfterAction(response);
        vm.form.label = '';
        vm.seriousnessLevel = 0;
    },

    async delete(incidentType: Seriousness): Promise<void> {
        let response = await seriousnessService.delete(incidentType.id);
        vm.proceedAfterAction(response);
    },

    hasSeriousnesses(): boolean {
        return vm.seriousnesses && vm.seriousnesses.length !== 0;
    },

    async get(): Promise<void> {
        vm.seriousnesses = await seriousnessService.get(window.model.vieScolaire.structure.id);
        vm.safeApply();
    },

    async toggleVisibility(seriousness: Seriousness): Promise<void> {
        seriousness.hidden = !seriousness.hidden;
        let form = {} as SeriousnessRequest;
        form.id = seriousness.id;
        form.hidden = seriousness.hidden;
        form.label = seriousness.label;
        form.level = seriousness.level;
        await seriousnessService.update(form);
    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.get();
        }
    },

    openIncidentsManageLightbox(seriousness: Seriousness): void {
        incidentsSeriousnessManage.that.$emit(INCIDENTS_SERIOUSNESS_EVENT.SEND, seriousness);
    },

    chooseLevel(level: number): void {
        vm.seriousnessLevel = level;
    }

};

export const incidentsSeriousnessManage = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            incidentsSeriousnessManage.that = this;
            vm.safeApply = safeApply;
        },
        setHandler: function () {
            // using vieScolaire.structure to update current structure from viescolaire
            this.$watch(() => window.model.vieScolaire.structure, async () => vm.get());
            this.$on(INCIDENTS_SERIOUSNESS_EVENT.RESPOND, () => vm.get());
            this.$on('reload', vm.get);
        }
    }
};