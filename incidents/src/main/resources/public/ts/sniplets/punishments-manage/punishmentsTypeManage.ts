import {punishmentsTypeService} from "@incidents/services/PunishmentTypeService";
import {IPunishmentType, IPunishmentTypeBody} from "@incidents/models/PunishmentType";
import {IPunishmentCategory} from "@incidents/models/PunishmentCategory";
import {punishmentsCategoryService} from "@incidents/services/PunishmentCategoryService";
import {AxiosResponse} from "axios";
import {toasts} from "entcore";
import {INCIDENTS_PUNISHMENT_TYPE_EVENT} from "@common/core/enum/incidents-event";
import {PunishmentsUtils} from "@incidents/utilities/punishments";

declare let window: any;

console.log('Incident Punishment sniplet');

interface ViewModel {
    safeApply(fn?: () => void): void;

    punishmentType: IPunishmentType[];
    punishmentCategory: IPunishmentCategory[];
    punishments: IPunishmentType[];
    sanctions: IPunishmentType[];
    img: string;

    form: IPunishmentTypeBody;

    getPunishmentsCategory(): Promise<void>;

    getPunishmentsType(): Promise<void>;

    createPunishmentType(): Promise<void>;

    toggleVisibility(punishmentType: IPunishmentTypeBody, punishmentCategory: IPunishmentCategory): Promise<void>;

    deletePunishmentType(punishmentType: IPunishmentTypeBody): Promise<void>;

    proceedAfterAction(response: AxiosResponse): void;

    switchCategory(): Promise<void>;

    openIncidentsManageLightbox(punishmentType: IPunishmentTypeBody, punishmentCategory: IPunishmentCategory): void;
}

function safeApply() {
    let that = punishmentsTypeManage.that;
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
    punishmentType: [],
    punishmentCategory: [],
    punishments: [],
    sanctions: [],
    img: null,
    form: {} as IPunishmentTypeBody,

    async getPunishmentsCategory(): Promise<void> {
        vm.punishmentCategory = await punishmentsCategoryService.get();
        vm.safeApply();
    },

    async getPunishmentsType(): Promise<void> {
        vm.punishments = [];
        vm.sanctions = [];
        vm.punishmentCategory = await punishmentsCategoryService.get();
        vm.punishmentType = await punishmentsTypeService.get(window.model.vieScolaire.structure.id);
        vm.punishmentType.forEach((item: IPunishmentType) => {
            if (item.type == PunishmentsUtils.RULES.punishment) {
                vm.punishments.push(item);
            }
            if (item.type == PunishmentsUtils.RULES.sanction) {
                vm.sanctions.push(item);
            }
        });
        vm.safeApply();
    },

    async createPunishmentType(): Promise<void> {
        vm.form.structure_id = window.model.vieScolaire.structure.id;
        let response = await punishmentsTypeService.create(vm.form);
        vm.proceedAfterAction(response);
        vm.form.label = '';
        vm.form.type = '';
        if (response) {
            toasts.confirm('presences.punishments.type.setting.method.create.confirm');
        } else {
            toasts.warning('presences.punishments.type.setting.method.create.error');
        }
    },

    async toggleVisibility(punishmentType: IPunishmentTypeBody): Promise<void> {
        punishmentType.hidden = !punishmentType.hidden;
        let form = {} as IPunishmentTypeBody;
        form.id = punishmentType.id;
        form.label = punishmentType.label;
        form.type = punishmentType.type;
        form.punishment_category_id = punishmentType.punishment_category_id;
        form.hidden = punishmentType.hidden;
        await punishmentsTypeService.update(form);
    },

    async deletePunishmentType(punishmentType: IPunishmentType): Promise<void> {
        let response = await punishmentsTypeService.delete(punishmentType.id);
        vm.proceedAfterAction(response);
        if (response) {
            toasts.confirm('presences.punishments.type.setting.method.delete.confirm');
        } else {
            toasts.warning('presences.punishments.type.setting.method.delete.error');
        }
    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.getPunishmentsType();
        }
    },

    async switchCategory(): Promise<void> {
        vm.img = vm.punishmentCategory
            .find((item: IPunishmentCategory) => item.id === vm.form.punishment_category_id).url_img;
    },

    openIncidentsManageLightbox(punishmentType: IPunishmentTypeBody): void {
        let punishmentData: {
            punishmentType: IPunishmentTypeBody;
            punishmentCategory: IPunishmentCategory[]
        } = {
            punishmentType: punishmentType,
            punishmentCategory: vm.punishmentCategory
        };
        punishmentsTypeManage.that.$emit(INCIDENTS_PUNISHMENT_TYPE_EVENT.SEND, punishmentData);
    },
};

export const punishmentsTypeManage = {
    title: 'punishments.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            punishmentsTypeManage.that = this;
            vm.safeApply = safeApply;
        },
        setHandler: function () {
            this.$watch(() => window.model.vieScolaire.structure, async () => vm.getPunishmentsType());
            this.$on(INCIDENTS_PUNISHMENT_TYPE_EVENT.RESPOND, () => vm.getPunishmentsType());
            this.$on('reload', vm.getPunishmentsType);
        }
    }
};