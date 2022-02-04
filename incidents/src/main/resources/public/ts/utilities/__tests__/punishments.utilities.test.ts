import {PunishmentsUtils} from "../punishments";
import {IPDetentionField, IPDutyField, IPExcludeField, IPunishment, IPunishmentBody} from "@incidents/models";
import {IPunishmentType} from "@incidents/models/PunishmentType";

describe('PunishmentsUtilities', () => {
    it('test the correct functioning of the initPunishmentRules method', done => {
        expect(PunishmentsUtils.initPunishmentRules()).toEqual([
            {isSelected: true, label: "incidents.punishments", type: "PUNISHMENT", value: "PUNISHMENT"},
            {isSelected: true, label: "incidents.sanctions", type: "SANCTION", value: "SANCTION"}
        ]);
        done();
    });

    it('test the correct functioning of the initPunishmentStates method', done => {
        const data: Array<{ label: string, value: string, isSelected: boolean }> = [
            {isSelected: true, label: "incidents.state.processed", value: "PROCESSED"},
            {isSelected: true, label: "incidents.state.not.processed", value: "NOT_PROCESSED"}
        ];
        expect(PunishmentsUtils.initPunishmentStates()).toEqual(data)
        done();
    });

    it('test the correct functioning of the initMassmailingsPunishments method', done => {
        const data: Array<{ label: string, isSelected: boolean }> = [
            {isSelected: false, label: "massmailing.massmailed.mailed"},
            {isSelected: true, label: "massmailing.massmailed.waiting"}
        ];
        expect(PunishmentsUtils.initMassmailingsPunishments()).toEqual(data)
        done();
    });

    it('test the correct functioning of the canCreatePunishmentOnly method', done => {
        expect(PunishmentsUtils.canCreatePunishmentOnly()).toEqual(false)
        done();
    });

    it('test the correct functioning of the isValidPunishmentBody method', done => {
        const punishment: IPunishmentBody = {student_ids: []}
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(undefined);
        const punishmentType : IPunishmentType = {
            hidden: false,
            id: 0,
            label: "",
            punishment_category_id: 1,
            structure_id: "",
            type: ""
        }
        //Test dutyField
        let duty : IPDutyField = undefined
        punishment.fields = duty;
        punishment.type = punishmentType;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(undefined);
        duty = {};
        duty.delay_at = undefined;
        punishment.fields = duty;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(undefined);
        duty.delay_at = "2000/01/01 00:00:00";
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(false);
        duty.delay_at = "2000-01-01 00:00:00";
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(true);

        //Test detentionField
        let detention : Array<IPDetentionField> = undefined;
        punishment.fields = detention;
        punishment.type.punishment_category_id = 2;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(false);
        detention = [];
        punishment.fields = detention;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(true);
        let slot1: IPDetentionField = {start_at: "2000-01-01 00:00:00", end_at: "2000-01-02 00:00:00"};
        let slot2: IPDetentionField = {start_at: "2000gteers", end_at: "2zsvbrsbe"};
        detention = [slot1, slot2];
        punishment.fields = detention;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(false);
        slot1 = {start_at: "2000-01-01 00:00:00", end_at: "2000-01-02 00:00:00"};
        slot2 = {start_at: "2000-01-01 00:00:00", end_at: "2000-01-02 00:00:00"};
        detention = [slot1, slot2];
        punishment.fields = detention;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(true);

        //Test blameField
        punishment.type.punishment_category_id = 3;
        punishment.fields = undefined;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(true);

        //Test excludeField
        punishment.type.punishment_category_id = 4;
        let exclude : IPExcludeField = undefined
        punishment.fields = exclude;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(undefined);
        exclude = {start_at: undefined, end_at: undefined};
        punishment.fields = exclude;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(undefined);
        exclude = {start_at: "2000-01-01 00:00:00", end_at: "2000-01-02 00:0000"};
        punishment.fields = exclude;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(false);
        exclude = {start_at: "2000-01-01 00:00:00", end_at: "2000-01-02 00:00:00"};
        punishment.fields = exclude;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(true);

        punishment.type.punishment_category_id = 5;
        expect(PunishmentsUtils.isValidPunishmentBody(punishment)).toEqual(false);
        done();
    });

    it('test the correct functioning of the getPunishmentDate method', done => {
        const punishment : IPunishment = {
            id: "",
            owner: undefined,
            structure_id: "",
            student: undefined,
            type: undefined,
            created_at: "2000-02-01 00:00:00",
            grouped_punishment_id: ""
        }
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("01/02/2000")).toEqual(true);
        const punishmentType : IPunishmentType = {
            hidden: false,
            id: 0,
            label: "",
            punishment_category_id: 1,
            structure_id: "",
            type: ""
        }
        //Test dutyField
        let duty : IPDutyField = {delay_at: undefined}
        punishment.fields = duty;
        punishment.type = punishmentType;
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("01/02/2000")).toEqual(true);
        duty.delay_at = "2000-02-08 00:00:00";
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("08/02/2000")).toEqual(true);

        //Test detentionField
        let detention : IPDetentionField = {};
        punishment.fields = detention;
        punishment.type.punishment_category_id = 2;
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("01/02/2000")).toEqual(true);
        detention.end_at = "2000-03-09 00:00:00";
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("01/02/2000")).toEqual(true);
        detention.end_at = undefined;
        detention.start_at = "2000-03-09 00:00:00";
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("09/03/2000")).toEqual(true);

        //Test blameField
        punishment.type.punishment_category_id = 3;
        punishment.fields = undefined;
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("01/02/2000")).toEqual(true);

        //Test excludeField
        punishment.type.punishment_category_id = 4;
        let exclude : IPExcludeField = {};
        punishment.fields = exclude;
        punishment.type.punishment_category_id = 2;
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("01/02/2000")).toEqual(true);
        exclude.end_at = "2000-03-09 00:00:00";
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("01/02/2000")).toEqual(true);
        exclude.end_at = undefined;
        exclude.start_at = "2000-03-09 00:00:00";
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("09/03/2000")).toEqual(true);
        exclude.end_at = "2000-04-09 00:00:00";
        expect(PunishmentsUtils.getPunishmentDate(punishment).includes("09/03/2000")).toEqual(true);

        done();
    });

    it('test the correct functioning of the isValidDetention method', done => {
        let slot: IPDetentionField = {start_at: undefined, end_at: undefined}
        expect(PunishmentsUtils.isValidDetention(slot)).toEqual(false);
        slot = {start_at: "2000-01-01 00:00:00", end_at: undefined}
        expect(PunishmentsUtils.isValidDetention(slot)).toEqual(false);
        slot = {start_at: undefined, end_at: "2000-01-01 00:00:00"}
        expect(PunishmentsUtils.isValidDetention(slot)).toEqual(false);
        slot = {start_at: "2000-01-01 00:0000", end_at: "2000-01-02 00:00:00"}
        expect(PunishmentsUtils.isValidDetention(slot)).toEqual(false);
        slot = {start_at: "2000-01-01 00:00:00", end_at: "2000-01-02 00:0000"}
        expect(PunishmentsUtils.isValidDetention(slot)).toEqual(false);
        slot = {start_at: "2000-01-01 00:00:00", end_at: "2000-01-02 00:00:00"}
        expect(PunishmentsUtils.isValidDetention(slot)).toEqual(true);
        done();
    });
});