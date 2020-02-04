import {
    INCIDENTS_PARTNER_EVENT,
    INCIDENTS_PLACE_EVENT,
    INCIDENTS_PROTAGONIST_TYPE_EVENT,
    INCIDENTS_SERIOUSNESS_EVENT,
    INCIDENTS_TYPE_EVENT
} from "@common/enum/incidents-event";
import {PRESENCES_ACTION, PRESENCES_DISCIPLINE} from "@common/enum/presences-event";
import {IAngularEvent} from "angular";

declare let window: any;

console.log("presenceManage sniplet");

interface ViewModel {
    safeApply(fn?: () => void): void;

    scrollToElement($element): void;

    sendEvent(event, data): void;

    respondEvent(event, data): void;
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

const vm: ViewModel = {
    safeApply: null,

    scrollToElement(target): void {
        let element = document.getElementById(target);
        element.scrollIntoView({behavior: "smooth", block: "start", inline: "nearest"});
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
        }
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
        },
        setHandler: function () {
            this.$watch(() => window.model.vieScolaire.structure, async () => this.$apply());

            /* presence discipline event */
            this.$on(PRESENCES_DISCIPLINE.SEND, (event: IAngularEvent, args) => vm.sendEvent(event, args));
            this.$on(PRESENCES_DISCIPLINE.SEND_BACK, (event: IAngularEvent, args) => vm.respondEvent(event, args));

            /* presence discipline event */
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

        }
    }
};