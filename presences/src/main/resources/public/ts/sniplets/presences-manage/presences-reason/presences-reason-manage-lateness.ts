import {idiom, toasts} from "entcore";
import {Reason, ReasonRequest} from "../../../models/Reason";
import {ReasonService, reasonService} from "../../../services/ReasonService";
import {AxiosError, AxiosResponse} from "axios";
import {ReasonSnipletModel} from "./presences-reason-manager";
import {safeApply} from "@common/utils";
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";

declare let window: any;

export class LatenessReasonSniplet implements ReasonSnipletModel {
    lang: typeof idiom;

    reasonTypeId: number;
    reasons: Reason[];
    editReasonLightbox: boolean;
    form: ReasonRequest;
    formEdit: ReasonRequest;


    constructor(private readonly $scope: any, private reasonService: ReasonService) {
        this.lang = idiom;
        this.init();
    }

    init() {
        this.reasons = [];
        this.editReasonLightbox = false;
        this.form = {} as ReasonRequest;
        this.formEdit = {} as ReasonRequest;
        this.reasonTypeId = REASON_TYPE_ID.LATENESS;

        this.form = {
            label: "",
            absenceCompliance: true,
            proving: true
        }

        this.$scope.$watch(() => window.model.vieScolaire.structure, async () => this.getReasons());
        this.$scope.$on('reload', () => this.getReasons());
    }

    openReasonLightbox(reason: Reason): void {
        this.editReasonLightbox = true;
        this.setFormEdit(reason);
    }

    closeReasonLightbox(): void {
        this.editReasonLightbox = false;
    }

    isFormValid(form: ReasonRequest): boolean {
        return !!(form.label);
    }

    hasReasons(): boolean {
        return this.reasons && this.reasons.length !== 0;
    }

    async getReasons(): Promise<void> {
        try {
            let reasons: Reason[] = await this.reasonService.getReasons(window.model.vieScolaire.structure.id, this.reasonTypeId);
            this.reasons = reasons.filter((reason: Reason) => reason.id != -1);
            safeApply(this.$scope);
        } catch (err) {
            console.error(err);
        }
    }

    async createReason(): Promise<void> {
        if (!this.form.hasOwnProperty("absenceCompliance")) {
            this.form.absenceCompliance = true;
        }
        if (!this.form.hasOwnProperty("proving")) {
            this.form.proving = true;
        }
        this.form.structureId = window.model.vieScolaire.structure.id;
        try {
            let response: AxiosResponse = await this.reasonService.create(this.form, this.reasonTypeId);
            this.proceedAfterAction(response);
        } catch (err) {
            console.error(err);
        }

        /* Reset form*/
        this.form.label = '';
        this.form.proving = true;
        this.form.absenceCompliance = true;
    }

    async deleteReason(reason: Reason): Promise<void> {
        try {
            let response: AxiosResponse = await this.reasonService.delete(reason.id);
            this.proceedAfterAction(response);
        } catch (err) {
            console.error(err)
        }
    }

    setFormEdit(reason: Reason): void {
        let reasonCopy: Reason = JSON.parse(JSON.stringify(reason));
        this.formEdit.id = reasonCopy.id;
        this.formEdit.absenceCompliance = reasonCopy.absence_compliance;
        this.formEdit.hidden = reasonCopy.hidden;
        this.formEdit.proving = reasonCopy.proving;
        this.formEdit.label = reasonCopy.label;
    }

    async updateReason(): Promise<void> {
        try {
            let response: AxiosResponse = await this.reasonService.update(this.formEdit);
            this.proceedAfterAction(response);
            this.closeReasonLightbox();
        } catch (err) {
            console.error(err);
        }
    }

    async toggleVisibility(reason: Reason): Promise<void> {
        reason.hidden = !reason.hidden;
        let form: ReasonRequest = {} as ReasonRequest;
        form.id = reason.id;
        form.absenceCompliance = reason.absence_compliance;
        form.hidden = reason.hidden;
        form.proving = reason.proving;
        form.label = reason.label;
        await this.reasonService.update(form).catch((err: AxiosError) => {
            toasts.warning('presences.reason.error');
            console.error(err)
        });

    }

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            this.getReasons().catch((e: AxiosError) => console.error(e));
        }
    }
}

export const presencesReasonManageLateness = {
    title: 'presences.lateness.reason.title',
    public: false,
    controller: {
        init: function (): void {
            this.vm = new LatenessReasonSniplet(this, reasonService);
        },
    }
};