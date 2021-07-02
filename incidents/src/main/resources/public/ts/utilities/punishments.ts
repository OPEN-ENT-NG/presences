import {
    IPBlameField, IPDetentionField, IPDutyField, IPExcludeField,
    IPunishmentBody,
    MassmailingsPunishments,
    PunishmentsProcessStates,
    PunishmentsRules
} from "@incidents/models";
import {model} from "entcore";
import incidentsRights from "@incidents/rights";
import {DateUtils} from "@common/utils";
import {idiom as lang, moment} from 'entcore';

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
                    return DateUtils.isValid((<IPDutyField>punishment.fields).delay_at,
                        DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])
                case 2: // DETENTION
                    let fieldDetention: IPDetentionField = (<IPDetentionField>punishment.fields)
                    return DateUtils.isPeriodValid(fieldDetention.start_at, fieldDetention.end_at);
                case 3: // BLAME
                    return true;
                case 4: // EXCLUSION
                    let fieldExclusion: IPExcludeField = (<IPExcludeField>punishment.fields)
                    return DateUtils.isPeriodValid(fieldExclusion.start_at, fieldExclusion.end_at);
            }
            return false;
        }
    }
}