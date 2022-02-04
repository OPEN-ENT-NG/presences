import {
    IPDetentionField,
    IPDutyField,
    IPExcludeField,
    IPunishment,
    IPunishmentBody,
    MassmailingsPunishments,
    PunishmentsProcessStates,
    PunishmentsRules
} from "@incidents/models";
import {idiom as lang, model} from "entcore";
import incidentsRights from "@incidents/rights";
import {DateUtils} from "@common/utils";

export class PunishmentsUtils {

    public static readonly RULES = {
        punishment: 'PUNISHMENT',
        sanction: 'SANCTION'
    };

    /* init punishments rules to interact */
    static initPunishmentRules = (): Array<{ label: string, value: string, isSelected: boolean, type: string }> => {
        let punishmentsRules = [];
        Object.keys(PunishmentsRules)
            .forEach(punishmentRule => {
                switch (punishmentRule) {
                    case PunishmentsRules[PunishmentsRules.PUNISHMENT]: {
                        let i18n = 'incidents.punishments';
                        punishmentsRules.push({
                            label: i18n,
                            value: punishmentRule,
                            isSelected: true,
                            type: PunishmentsUtils.RULES.punishment
                        });
                        break;
                    }
                    case PunishmentsRules[PunishmentsRules.SANCTION]: {
                        if (PunishmentsUtils.canCreatePunishmentOnly()) break;
                        let i18n = 'incidents.sanctions';
                        punishmentsRules.push({
                            label: i18n,
                            value: punishmentRule,
                            isSelected: true,
                            type: PunishmentsUtils.RULES.sanction
                        });
                        break;
                    }
                }
            });
        return punishmentsRules;
    };

    /**
     * Init punishments rules to interact
     */
    static initPunishmentStates = (): Array<{ label: string, value: string, isSelected: boolean }> => {
        let punishmentsStates: Array<{ label: string, value: string, isSelected: boolean }> = [];
        Object.keys(PunishmentsProcessStates)
            .forEach((punishmentState: string) => {
                switch (punishmentState) {
                    case PunishmentsProcessStates[PunishmentsProcessStates.PROCESSED]: {
                        let i18n: string = 'incidents.state.processed';
                        punishmentsStates.push({label: i18n, value: punishmentState, isSelected: true});
                        break;
                    }
                    case PunishmentsProcessStates[PunishmentsProcessStates.NOT_PROCESSED]: {
                        let i18n: string = 'incidents.state.not.processed';
                        punishmentsStates.push({label: i18n, value: punishmentState, isSelected: true});
                        break;
                    }
                }
            });
        return punishmentsStates;
    }

    /* init massmailing punishments menu to interact */
    static initMassmailingsPunishments = (): Array<{ label: string, isSelected: boolean }> => {
        let massmailingsPunishments = [];
        Object.keys(MassmailingsPunishments)
            .forEach(massmailingPunishment => {
                switch (massmailingPunishment) {
                    case MassmailingsPunishments[MassmailingsPunishments.PUBLISHED]: {
                        let i18n = 'massmailing.massmailed.mailed';
                        massmailingsPunishments.push({label: i18n, isSelected: false});
                        break;
                    }
                    case MassmailingsPunishments[MassmailingsPunishments.NOT_PUBLISHED]: {
                        let i18n = 'massmailing.massmailed.waiting';
                        massmailingsPunishments.push({label: i18n, isSelected: true});
                        break;
                    }
                }
            });
        return massmailingsPunishments;
    };

    /* Method that check if can create ONLY punishment */
    static canCreatePunishmentOnly = (): boolean => {
        return model.me.hasWorkflow(incidentsRights.workflow.createPunishment) &&
            !model.me.hasWorkflow(incidentsRights.workflow.createSanction);
    };

    static isValidPunishmentBody = (punishment: IPunishmentBody): boolean => {
        if (punishment.type) {
            switch (punishment.type.punishment_category_id) {
                case 1: // DUTY
                    let fieldDuty: IPDutyField = (<IPDutyField>punishment.fields)
                    return fieldDuty && fieldDuty.delay_at && DateUtils.isValid(fieldDuty.delay_at,
                        DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])
                case 2: // DETENTION
                    let slots: Array<IPDetentionField> = (<Array<IPDetentionField>>punishment.fields)
                    return !!slots
                        && slots.filter((slot: IPDetentionField) => !PunishmentsUtils.isValidDetention(slot))
                            .length == 0;
                case 3: // BLAME
                    return true;
                case 4: // EXCLUSION
                    let fieldExclusion: IPExcludeField = (<IPExcludeField>punishment.fields)
                    return fieldExclusion && fieldExclusion.start_at && fieldExclusion.end_at &&
                        DateUtils.isPeriodValid(fieldExclusion.start_at, fieldExclusion.end_at);
            }
            return false;
        }
    }

    static getPunishmentDate = (punishment: IPunishment): string => {
        let createdDate: string = DateUtils.format(punishment.created_at, DateUtils.FORMAT['DAY-MONTH-YEAR']);

        if (punishment.type) {
            switch (punishment.type.punishment_category_id) {
                case 1: // DUTY
                    let dutyDate: string = createdDate;
                    if ((<IPDutyField>punishment.fields).delay_at) {
                        dutyDate = DateUtils.format((<IPDutyField>punishment.fields).delay_at, DateUtils.FORMAT['DAY-MONTH-YEAR']);
                    }
                    return lang.translate('incidents.punishments.date.for.the') + dutyDate;
                case 2: // DETENTION
                    let startDetentionDate: string = createdDate;
                    if ((<IPDetentionField>punishment.fields).start_at) {
                        startDetentionDate = DateUtils.format((<IPDetentionField>punishment.fields).start_at, DateUtils.FORMAT['DAY-MONTH-YEAR']);
                    }
                    return lang.translate('incidents.punishments.date.for.the') + startDetentionDate;
                case 3: // BLAME
                    return lang.translate('incidents.punishments.date.created.on') + createdDate;
                case 4: // EXCLUSION
                    if ((<IPExcludeField>punishment.fields).start_at && (<IPExcludeField>punishment.fields).end_at) {
                        let startExcludeDate: string = DateUtils.format((<IPExcludeField>punishment.fields).start_at, DateUtils.FORMAT['DAY-MONTH-YEAR']);
                        let endExcludeDate: string = DateUtils.format((<IPExcludeField>punishment.fields).end_at, DateUtils.FORMAT['DAY-MONTH-YEAR']);
                        if (startExcludeDate && endExcludeDate) {
                            return lang.translate('incidents.punishments.date.from') +
                                startExcludeDate + lang.translate('incidents.punishments.date.to') +
                                endExcludeDate;
                        }
                    }
            }
        }
        return lang.translate('incidents.punishments.date.created.on') + createdDate;
    };

    static isValidDetention = (slot: IPDetentionField): boolean => {
        return !!slot.start_at && !!slot.end_at
            && DateUtils.isPeriodValid(slot.start_at, slot.end_at);
    }
}