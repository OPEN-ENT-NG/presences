import {IPunishment, IPunishmentResponse, Punishments} from "../Punishments";

describe('PunishmentsModel', () => {
    it('test the correct functioning of constructor and the build method', done => {
        const punishment1: IPunishment = {
            id: "",
            owner: undefined,
            structure_id: "",
            student: undefined,
            type: undefined,
            grouped_punishment_id: ""
        };

        const punishment2: IPunishment = {
            id: "",
            owner: undefined,
            structure_id: "",
            student: undefined,
            type: undefined,
            grouped_punishment_id: ""
        };

        const data: IPunishmentResponse = {
            all: [punishment1, punishment2],
            page: 10,
            page_count: 20
        };

        const punishment: Punishments = new Punishments("structure_id");
        expect(punishment.structure_id).toEqual("structure_id");
        expect(punishment.punishmentResponse).toEqual({});

        punishment.build(data).then(() => {
            expect(punishment.punishmentResponse).toEqual(data);
            done();
        });
    });
});
