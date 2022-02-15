// tricks to fake "mock" entcore ng class in order to use service
import {model, ng} from "@presences/models/__mocks__/entcore";

import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {ViescolaireService} from "../ViescolaireService";
import {IStructure} from "@common/model";

describe('ViescolaireService', () => {
    it('returns data when API request is correctly called for getSchoolYearDates method', done => {
        const structureId = "structureId";
        var mock = new MockAdapter(axios);
        const data = {response: true};
        mock.onGet('viescolaire/settings/periode/schoolyear?structureId='+structureId).reply(200, data);

        ViescolaireService.getSchoolYearDates("structureId").then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for getSlotProfile method', done => {
        var mock = new MockAdapter(axios);
        const data = {response: true};
        mock.onGet('/viescolaire/structures/structureId/time-slot').reply(200, data);

        ViescolaireService.getSlotProfile("structureId").then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for getStudent method', done => {
        const structureId = "structureId";
        const studentId = "studentId";
        var mock = new MockAdapter(axios);
        const data = {response: true};
        mock.onGet(`viescolaire/eleves?idUser=${studentId}&idStructure=${structureId}`).reply(200, data);

        ViescolaireService.getStudent("structureId", "studentId").then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for getBuildOwnStructure method', done => {
        var mock = new MockAdapter(axios);
        const data = {response: true};

        const structure1: IStructure = {id: "id1", name: "name1"}
        const structure2: IStructure = {id: "id2", name: "name2"}
        const expected: Array<IStructure> = [structure1, structure2];

        model.me.structures.push("id1");
        model.me.structures.push("id2");

        model.me.structureNames.push("name1");
        model.me.structureNames.push("name2");

        mock.onGet('/viescolaire/structures/structureId/time-slot').reply(200, data);
        expect(ViescolaireService.getBuildOwnStructure()).toEqual(expected);
        done();
    });
});