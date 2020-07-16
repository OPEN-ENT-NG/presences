import {INCIDENTS_PLACE_EVENT} from "@common/core/enum/incidents-event";
import {Place, PlaceRequest, placeService} from "@incidents/services";
import {AxiosResponse} from "axios";

declare let window: any;

interface ViewModel {
    safeApply(fn?: () => void): void;

    form: PlaceRequest;
    places: Place[];

    hasPlaces(): boolean;

    proceedAfterAction(response: AxiosResponse): void;

    get(): Promise<void>;

    create(): Promise<void>;

    toggleVisibility(place: Place): Promise<void>;

    delete(place: Place): Promise<void>

    openIncidentsManageLightbox(place: Place): void;
}

function safeApply() {
    let that = incidentsPlaceManage.that;
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
    places: [],
    form: {} as PlaceRequest,

    async create(): Promise<void> {
        vm.form.structureId = window.model.vieScolaire.structure.id;
        let response = await placeService.create(vm.form);
        vm.proceedAfterAction(response);
        vm.form.label = '';
    },

    async delete(incidentType: Place): Promise<void> {
        let response = await placeService.delete(incidentType.id);
        vm.proceedAfterAction(response);
    },

    hasPlaces(): boolean {
        return vm.places && vm.places.length !== 0;
    },

    async get(): Promise<void> {
        vm.places = await placeService.get(window.model.vieScolaire.structure.id);
        vm.safeApply();
    },

    async toggleVisibility(places: Place): Promise<void> {
        places.hidden = !places.hidden;
        let form = {} as PlaceRequest;
        form.id = places.id;
        form.hidden = places.hidden;
        form.label = places.label;
        await placeService.update(form);
    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.get();
        }
    },

    openIncidentsManageLightbox(place: Place): void {
        incidentsPlaceManage.that.$emit(INCIDENTS_PLACE_EVENT.SEND, place);
    },
};

export const incidentsPlaceManage = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            incidentsPlaceManage.that = this;
            vm.safeApply = safeApply;
        },
        setHandler: function () {
            // using vieScolaire.structure to update current structure from viescolaire
            this.$watch(() => window.model.vieScolaire.structure, async () => vm.get());
            this.$on(INCIDENTS_PLACE_EVENT.RESPOND, () => vm.get());
            this.$on('reload', vm.get);
        }
    }
};