import {idiom} from "entcore";
import {Reason, ReasonRequest} from "@presences/models";
import {AxiosResponse} from "axios";

export interface ReasonSnipletModel {
    lang: typeof idiom;

    reasonTypeId: number;
    reasons: Reason[];
    editReasonLightbox: boolean;
    form: ReasonRequest;
    formEdit: ReasonRequest;

    init(): void;

    openReasonLightbox(reason: Reason): void;

    closeReasonLightbox(): void;

    isFormValid(form: ReasonRequest): boolean;

    hasReasons(): boolean;

    // interaction
    getReasons(): void;

    createReason(): void;

    setFormEdit(reason: Reason): void;

    updateReason(): void;

    toggleVisibility(reason: Reason): Promise<void>;

    deleteReason(reason: Reason): void;

    proceedAfterAction(response: AxiosResponse): void;

}