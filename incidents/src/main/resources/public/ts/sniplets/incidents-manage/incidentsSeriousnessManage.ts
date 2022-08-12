import {INCIDENTS_SERIOUSNESS_EVENT} from "@common/core/enum/incidents-event";
import {Seriousness, SeriousnessRequest, seriousnessService} from "@incidents/services";
import {AxiosResponse} from "axios";
import {idiom as lang} from "entcore";
import {safeApply} from "@common/utils";

declare let window: any;

interface ViewModel {
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

export class IncidentsSeriousnessManage implements ViewModel {
    lang: typeof lang;

    seriousnesses: Array<any>;
    form: SeriousnessRequest;
    seriousnessLevel: number;

    constructor(private $scope: any) {
        this.lang = lang;
        this.init();
    }

    init() {
        this.seriousnesses = [];
        this.form = {excludeAlertSeriousness: false} as SeriousnessRequest;
        this.seriousnessLevel = 0;

        this.$scope.$watch(() => window.model.vieScolaire.structure, async () => this.get());
        this.$scope.$on(INCIDENTS_SERIOUSNESS_EVENT.RESPOND, () => this.get());
        this.$scope.$on('reload', this.get);
    }

    async create(): Promise<void> {
        this.form.structureId = window.model.vieScolaire.structure.id;
        this.form.level = this.seriousnessLevel;
        let response = await seriousnessService.create(this.form);
        this.proceedAfterAction(response);
        this.form.label = '';
        this.seriousnessLevel = 0;
    }

    async delete(incidentType: Seriousness): Promise<void> {
        let response = await seriousnessService.delete(incidentType.id);
        this.proceedAfterAction(response);
    }

    hasSeriousnesses(): boolean {
        return this.seriousnesses && this.seriousnesses.length !== 0;
    }

    async get(): Promise<void> {
        this.seriousnesses = await seriousnessService.get(window.model.vieScolaire.structure.id);
        safeApply(this.$scope);
    }

    async toggleVisibility(seriousness: Seriousness): Promise<void> {
        seriousness.hidden = !seriousness.hidden;
        let form = {} as SeriousnessRequest;
        form.id = seriousness.id;
        form.hidden = seriousness.hidden;
        form.label = seriousness.label;
        form.level = seriousness.level;
        form.excludeAlertSeriousness = seriousness.exclude_alert_seriousness;
        await seriousnessService.update(form);
    }

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            this.get();
        }
    }

    openIncidentsManageLightbox(seriousness: Seriousness): void {
        this.$scope.$emit(INCIDENTS_SERIOUSNESS_EVENT.SEND, seriousness);
    }

    chooseLevel(level: number): void {
        this.seriousnessLevel = level;
    }

    getSeriousnessAlertColor(seriousness: Seriousness): string {
        return seriousness.exclude_alert_seriousness ? null : "#ffb600";
    }
}

export const incidentsSeriousnessManage = {
    title: 'incidents.manage.title',
    public: false,
    controller: {
        init: async function () {
            this.vm = new IncidentsSeriousnessManage(this);
        }
    }
};