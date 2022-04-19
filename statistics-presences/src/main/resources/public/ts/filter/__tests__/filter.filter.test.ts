import {FilterTypeFactory} from "@statistics/filter";
import { Reason } from "@presences/models";
import { IPunishmentType } from "@incidents/models/PunishmentType";
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";
import { PunishmentsUtils } from "@incidents/utilities/punishments";

describe('FilterTypeFactory', () => {
    let filter: FilterTypeFactory;

    beforeEach(() => {
        const reason1: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 0,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        };
        const reason2: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 1,
            isSelected: false,
            label: "",
            proving: true,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        };
        const reason3: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 2,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.LATENESS
        };
        const reason4: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 3,
            isSelected: false,
            label: "",
            proving: true,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.LATENESS
        };
        const punishment1: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "",
            punishment_category_id: 0,
            structure_id: "",
            type: PunishmentsUtils.RULES.punishment
        };
        const punishment2: IPunishmentType = {
            hidden: false,
            id: 1,
            label: "",
            punishment_category_id: 0,
            structure_id: "",
            type: PunishmentsUtils.RULES.sanction
        };
        const reason: Reason[] = [reason1, reason2, reason3, reason4];
        const punishmentTypes: IPunishmentType[] = [punishment1, punishment2];
        filter = new FilterTypeFactory(reason, punishmentTypes);
    });

    it('changePunishmentFilter methods', done => {
        filter.punishmentTypes.filter((el:IPunishmentType) => {
            expect(filter.punishmentTypesMap[el.id]).toBeTruthy();
        });
        filter.changePunishmentFilter(false);
        expect(filter.punishmentTypesMap[filter.punishmentTypes[0].id]).toBeFalsy();
        expect(filter.punishmentTypesMap[filter.punishmentTypes[1].id]).toBeTruthy();

        filter.punishmentTypes.filter((el:IPunishmentType) => {
            filter.punishmentTypesMap[el.id] = false;
        });
        filter.changePunishmentFilter(true);
        expect(filter.punishmentTypesMap[filter.punishmentTypes[0].id]).toBeTruthy();
        expect(filter.punishmentTypesMap[filter.punishmentTypes[1].id]).toBeFalsy();
        done();
    });

    it('changeSanctionFilter methods', done => {
        filter.punishmentTypes.filter((el:IPunishmentType) => {
            expect(filter.punishmentTypesMap[el.id]).toBeTruthy();
        });
        filter.changeSanctionFilter(false);
        expect(filter.punishmentTypesMap[filter.punishmentTypes[0].id]).toBeTruthy();
        expect(filter.punishmentTypesMap[filter.punishmentTypes[1].id]).toBeFalsy();

        filter.punishmentTypes.filter((el:IPunishmentType) => {
            filter.punishmentTypesMap[el.id] = false;
        });
        filter.changeSanctionFilter(true);
        expect(filter.punishmentTypesMap[filter.punishmentTypes[0].id]).toBeFalsy();
        expect(filter.punishmentTypesMap[filter.punishmentTypes[1].id]).toBeTruthy();
        done();
    });

    it('changeUnProvingAbsences methods', done => {
        filter.reasons.filter((el:Reason) => {
            expect(filter.reasonsMap[el.id]).toBeTruthy();
        });
        filter.changeUnProvingAbsences(false);
        expect(filter.reasonsMap[filter.reasons[0].id]).toBeFalsy();
        expect(filter.reasonsMap[filter.reasons[1].id]).toBeTruthy();
        expect(filter.reasonsMap[filter.reasons[2].id]).toBeTruthy();
        expect(filter.reasonsMap[filter.reasons[3].id]).toBeTruthy();

        filter.reasons.filter((el:Reason) => {
            filter.reasonsMap[el.id] = false;
        });
        filter.changeUnProvingAbsences(true);
        expect(filter.reasonsMap[filter.reasons[0].id]).toBeTruthy();
        expect(filter.reasonsMap[filter.reasons[1].id]).toBeFalsy();
        expect(filter.reasonsMap[filter.reasons[2].id]).toBeFalsy();
        expect(filter.reasonsMap[filter.reasons[3].id]).toBeFalsy();
        done();
    });

    it('changeProvingAbsences methods', done => {
        filter.reasons.filter((el:Reason) => {
            expect(filter.reasonsMap[el.id]).toBeTruthy();
        });
        filter.changeProvingAbsences(false);
        expect(filter.reasonsMap[filter.reasons[0].id]).toBeFalsy();
        expect(filter.reasonsMap[filter.reasons[1].id]).toBeFalsy();
        expect(filter.reasonsMap[filter.reasons[2].id]).toBeTruthy();
        expect(filter.reasonsMap[filter.reasons[3].id]).toBeTruthy();

        filter.reasons.filter((el:Reason) => {
            filter.reasonsMap[el.id] = false;
        });
        filter.changeProvingAbsences(true);
        expect(filter.reasonsMap[filter.reasons[0].id]).toBeTruthy();
        expect(filter.reasonsMap[filter.reasons[1].id]).toBeTruthy();
        expect(filter.reasonsMap[filter.reasons[2].id]).toBeFalsy();
        expect(filter.reasonsMap[filter.reasons[3].id]).toBeFalsy();
        done();
    });

    it('changeLateness methods', done => {
        filter.reasons.filter((el:Reason) => {
            expect(filter.reasonsMap[el.id]).toBeTruthy();
        });
        filter.changeLateness(false);
        expect(filter.reasonsMap[filter.reasons[0].id]).toBeFalsy();
        expect(filter.reasonsMap[filter.reasons[1].id]).toBeTruthy();
        expect(filter.reasonsMap[filter.reasons[2].id]).toBeFalsy();
        expect(filter.reasonsMap[filter.reasons[3].id]).toBeFalsy();

        filter.reasons.filter((el:Reason) => {
            filter.reasonsMap[el.id] = false;
        });
        filter.changeLateness(true);
        expect(filter.reasonsMap[filter.reasons[0].id]).toBeTruthy();
        expect(filter.reasonsMap[filter.reasons[1].id]).toBeFalsy();
        expect(filter.reasonsMap[filter.reasons[2].id]).toBeTruthy();
        expect(filter.reasonsMap[filter.reasons[3].id]).toBeTruthy();
        done();
    });

    it('unselectAllReasons methods', done => {
        filter.reasons.filter((el:Reason) => {
            expect(filter.reasonsMap[el.id]).toBeTruthy();
        });
        filter.unselectAllReasons();
        filter.reasons.filter((el:Reason) => {
            expect(filter.reasonsMap[el.id]).toBeFalsy();
        });
        filter.unselectAllReasons();
        filter.reasons.filter((el:Reason) => {
            expect(filter.reasonsMap[el.id]).toBeFalsy();
        });
        done();
    });
})