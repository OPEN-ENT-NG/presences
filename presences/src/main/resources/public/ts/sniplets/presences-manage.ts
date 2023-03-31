import {
    INCIDENTS_PARTNER_EVENT,
    INCIDENTS_PLACE_EVENT,
    INCIDENTS_PROTAGONIST_TYPE_EVENT,
    INCIDENTS_PUNISHMENT_TYPE_EVENT,
    INCIDENTS_SERIOUSNESS_EVENT,
    INCIDENTS_TYPE_EVENT
} from "@common/core/enum/incidents-event";
import {PRESENCES_ACTION, PRESENCES_DISCIPLINE} from "@common/core/enum/presences-event";
import {IAngularEvent} from "angular";
import {model, toasts} from "entcore";
import {INIT_TYPE} from "../core/enum/init-type";
import rights from "../rights";
import {IInitStatusResponse, initService} from "../services";

declare let window: any;

console.log("presenceManage sniplet");

interface ViewModel {
    initialized: boolean;

    safeApply(fn?: () => void): void;

    scrollToElement($element, option?: string): void;

    sendEvent(event, data): void;

    respondEvent(event, data): void;

    init(initType: INIT_TYPE): Promise<void>;

    hasRight(right: string): boolean;
}

function safeApply() {
    let that = presencesManage.that;
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

function fetchInitializationStatus(): void {
    initService.getPresencesInitStatus(window.model.vieScolaire.structure.id).then((status: IInitStatusResponse) => {
        if (status.initialized !== undefined) vm.initialized = status.initialized;
        else vm.initialized = false;
        vm.safeApply();
    })
    .catch(err => console.error('Failed to retrieve structure initialization status', err));
}

const vm: ViewModel = {
    initialized: true,
    safeApply: null,
    async init(initType: INIT_TYPE): Promise<void> {
        try {
            initService.initPresences(window.model.vieScolaire.structure.id, initType).then(() => {
                toasts.confirm(initType == INIT_TYPE.ONE_D ? "presences.init.1d.success" : "presences.init.2d.success");
                vm.initialized = true;
                presencesManage.that.$broadcast('reload');
                vm.safeApply();
            });
        } catch (err) {
            toasts.warning('presences.init.error');
            throw err;
        }
    },

    /**
     * Scroll To Element
     *
     * @param target    $element html
     * @param option    string optional parameter that will change block position
     */
    scrollToElement(target, option?: ScrollLogicalPosition): void {
        let element = document.getElementById(target);
        element.scrollIntoView({behavior: "smooth", block: option ? option : "center", inline: "nearest"});
        vm.safeApply();
    },

    sendEvent(event, data): void {
        switch (event.name) {
            case PRESENCES_DISCIPLINE.SEND:
                presencesManage.that.$broadcast(PRESENCES_DISCIPLINE.TRANSMIT, data);
                break;
            case PRESENCES_ACTION.SEND:
                presencesManage.that.$broadcast(PRESENCES_ACTION.TRANSMIT, data);
                break;
            case INCIDENTS_TYPE_EVENT.SEND:
                presencesManage.that.$broadcast(INCIDENTS_TYPE_EVENT.TRANSMIT, data);
                break;
            case INCIDENTS_PARTNER_EVENT.SEND:
                presencesManage.that.$broadcast(INCIDENTS_PARTNER_EVENT.TRANSMIT, data);
                break;
            case INCIDENTS_PLACE_EVENT.SEND:
                presencesManage.that.$broadcast(INCIDENTS_PLACE_EVENT.TRANSMIT, data);
                break;
            case INCIDENTS_PROTAGONIST_TYPE_EVENT.SEND:
                presencesManage.that.$broadcast(INCIDENTS_PROTAGONIST_TYPE_EVENT.TRANSMIT, data);
                break;
            case INCIDENTS_SERIOUSNESS_EVENT.SEND:
                presencesManage.that.$broadcast(INCIDENTS_SERIOUSNESS_EVENT.TRANSMIT, data);
                break;
            case INCIDENTS_PUNISHMENT_TYPE_EVENT.SEND:
                presencesManage.that.$broadcast(INCIDENTS_PUNISHMENT_TYPE_EVENT.TRANSMIT, data);
                break;
        }
    },

    hasRight(right: string): boolean {
        return model.me.hasWorkflow(rights.workflow[right]);
    },

    respondEvent(event, args): void {
        switch (event.name) {
            case PRESENCES_DISCIPLINE.SEND_BACK:
                presencesManage.that.$broadcast(PRESENCES_DISCIPLINE.RESPOND);
                break;
            case PRESENCES_ACTION.SEND_BACK:
                presencesManage.that.$broadcast(PRESENCES_ACTION.RESPOND);
                break;
            case INCIDENTS_TYPE_EVENT.SEND_BACK:
                presencesManage.that.$broadcast(INCIDENTS_TYPE_EVENT.RESPOND);
                break;
            case INCIDENTS_PARTNER_EVENT.SEND_BACK:
                presencesManage.that.$broadcast(INCIDENTS_PARTNER_EVENT.RESPOND);
                break;
            case INCIDENTS_PLACE_EVENT.SEND_BACK:
                presencesManage.that.$broadcast(INCIDENTS_PLACE_EVENT.RESPOND);
                break;
            case INCIDENTS_PROTAGONIST_TYPE_EVENT.SEND_BACK:
                presencesManage.that.$broadcast(INCIDENTS_PROTAGONIST_TYPE_EVENT.RESPOND);
                break;
            case INCIDENTS_SERIOUSNESS_EVENT.SEND_BACK:
                presencesManage.that.$broadcast(INCIDENTS_SERIOUSNESS_EVENT.RESPOND);
                break;
            case INCIDENTS_PUNISHMENT_TYPE_EVENT.SEND_BACK:
                presencesManage.that.$broadcast(INCIDENTS_PUNISHMENT_TYPE_EVENT.RESPOND);
                break;
        }

    }
};

export const presencesManage = {
    title: 'presences.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            presencesManage.that = this;
            vm.safeApply = safeApply;
            fetchInitializationStatus();
        },
        setHandler: function () {
            this.$watch(() => window.model.vieScolaire.structure, fetchInitializationStatus);

            /* presence discipline event */
            this.$on(PRESENCES_DISCIPLINE.SEND, (event: IAngularEvent, args) => vm.sendEvent(event, args));
            this.$on(PRESENCES_DISCIPLINE.SEND_BACK, (event: IAngularEvent, args) => vm.respondEvent(event, args));

            /* presence action event */
            this.$on(PRESENCES_ACTION.SEND, (event: IAngularEvent, args) => vm.sendEvent(event, args));
            this.$on(PRESENCES_ACTION.SEND_BACK, (event: IAngularEvent, args) => vm.respondEvent(event, args));

            /* incident type event */
            this.$on(INCIDENTS_TYPE_EVENT.SEND, (event: IAngularEvent, args) => vm.sendEvent(event, args));
            this.$on(INCIDENTS_TYPE_EVENT.SEND_BACK, (event: IAngularEvent, args) => vm.respondEvent(event, args));

            /* partner event */
            this.$on(INCIDENTS_PARTNER_EVENT.SEND, (event: IAngularEvent, args) => vm.sendEvent(event, args));
            this.$on(INCIDENTS_PARTNER_EVENT.SEND_BACK, (event: IAngularEvent, args) => vm.respondEvent(event, args));

            /* place event */
            this.$on(INCIDENTS_PLACE_EVENT.SEND, (event: IAngularEvent, args) => vm.sendEvent(event, args));
            this.$on(INCIDENTS_PLACE_EVENT.SEND_BACK, (event: IAngularEvent, args) => vm.respondEvent(event, args));

            /* protagonist type event */
            this.$on(INCIDENTS_PROTAGONIST_TYPE_EVENT.SEND, (event: IAngularEvent, args) => vm.sendEvent(event, args));
            this.$on(INCIDENTS_PROTAGONIST_TYPE_EVENT.SEND_BACK, (event: IAngularEvent, args) => vm.respondEvent(event, args));

            /* seriousness type event */
            this.$on(INCIDENTS_SERIOUSNESS_EVENT.SEND, (event: IAngularEvent, args) => vm.sendEvent(event, args));
            this.$on(INCIDENTS_SERIOUSNESS_EVENT.SEND_BACK, (event: IAngularEvent, args) => vm.respondEvent(event, args));

            /* punishment type event */
            this.$on(INCIDENTS_PUNISHMENT_TYPE_EVENT.SEND, (event: IAngularEvent, args) => vm.sendEvent(event, args));
            this.$on(INCIDENTS_PUNISHMENT_TYPE_EVENT.SEND_BACK, (event: IAngularEvent, args) => vm.respondEvent(event, args));

        }
    }
};