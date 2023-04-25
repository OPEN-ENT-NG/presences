import {ReasonService, reasonService} from "../../../services/ReasonService";
import {AxiosError, AxiosResponse} from "axios";
import {Reason, ReasonRequest} from "../../../models/Reason";
import {idiom, toasts} from "entcore";
import {ReasonSnipletModel} from "./presences-reason-manager";
import {safeApply} from "@common/utils";
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";
import {ALERT_RULE} from "@common/core/enum/alert-rule";

declare let window: any;

export class AbsenceReasonSniplet implements ReasonSnipletModel {
    lang: typeof idiom;

    reasonTypeId: number;
    reasons: Reason[];
    editReasonLightbox: boolean;
    form: ReasonRequest;
    formEdit: ReasonRequest;


    constructor(private $scope: any, private reasonService: ReasonService) {
        this.lang = idiom;
        this.init();
    }

    init() {
        this.reasons = [];
        this.editReasonLightbox = false;
        this.form = {} as ReasonRequest;
        this.formEdit = {} as ReasonRequest;
        this.reasonTypeId = REASON_TYPE_ID.ABSENCE;

        this.form = {
            label: "",
            absenceCompliance: true,
            proving: false,
            excludeAlertRegularised: false,
            excludeAlertNoRegularised: false,
            excludeAlertLateness: false,
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
        this.form.proving = false;
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
        this.formEdit.excludeAlertRegularised = (<any>reasonCopy.reason_alert_rules).includes(ALERT_RULE.REGULARIZED);
        this.formEdit.excludeAlertNoRegularised = (<any>reasonCopy.reason_alert_rules).includes(ALERT_RULE.UNREGULARIZED);
        this.formEdit.excludeAlertLateness = false;
        this.formEdit.structureId = reasonCopy.structure_id;
    }

    async updateReason(): Promise<void> {
        try {
            let response: AxiosResponse = await this.reasonService.update(this.formEdit);
            this.proceedAfterAction(response);
            this.closeReasonLightbox();
        } catch (err) {
            console.error(err)
        }
    }

    async toggleVisibility(reason: Reason): Promise<void> {
        let form: ReasonRequest = {} as ReasonRequest;
        reason.hidden = !reason.hidden;
        form.id = reason.id;
        form.absenceCompliance = reason.absence_compliance;
        form.hidden = reason.hidden;
        form.proving = reason.proving;
        form.label = reason.label;
        form.excludeAlertLateness = (<any>reason.reason_alert_rules).includes(ALERT_RULE.LATENESS);
        form.excludeAlertRegularised = (<any>reason.reason_alert_rules).includes(ALERT_RULE.REGULARIZED);
        form.excludeAlertNoRegularised = (<any>reason.reason_alert_rules).includes(ALERT_RULE.UNREGULARIZED);
        form.structureId = reason.structure_id;
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

    getReasonAlertColor(reason: Reason): string {
        if (!(<any>reason.reason_alert_rules).includes(ALERT_RULE.REGULARIZED) && !(<any>reason.reason_alert_rules).includes(ALERT_RULE.UNREGULARIZED)) {
            return "#ffb600";
        } else if (!(<any>reason.reason_alert_rules).includes(ALERT_RULE.REGULARIZED)) {
            return "#72bb53";
        } else if (!(<any>reason.reason_alert_rules).includes(ALERT_RULE.UNREGULARIZED)) {
            return "#ff8a84";
        }
        return null;
    }

    getReasonAlertTitle(reason: Reason): string {
        if (!(<any>reason.reason_alert_rules).includes(ALERT_RULE.REGULARIZED) && !(<any>reason.reason_alert_rules).includes(ALERT_RULE.UNREGULARIZED)) {
            return this.lang.translate('presences.absence.reason.regularized.and.unregularized');
        } else if (!(<any>reason.reason_alert_rules).includes(ALERT_RULE.REGULARIZED)) {
            return this.lang.translate('presences.absence.reason.regularized');
        } else if (!(<any>reason.reason_alert_rules).includes(ALERT_RULE.UNREGULARIZED)) {
            return this.lang.translate('presences.absence.reason.unregularized');
        }
        return "";
    }
}

export const presencesReasonManageAbsence = {
    title: 'presences.absence.reason.title',
    public: false,
    controller: {
        init: function (): void {
            this.vm = new AbsenceReasonSniplet(this, reasonService);
        }
    }
};