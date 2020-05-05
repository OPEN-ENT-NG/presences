import {MassmailingsPunishments, PunishmentsRules} from "@incidents/models";
import {model} from "entcore";
import incidentsRights from "@incidents/rights";

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
}