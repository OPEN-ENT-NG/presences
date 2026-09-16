// tricks to fake "mock" entcore ng class in order to use service
import {model, ng} from "@presences/models/__mocks__/entcore";

jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';
import {ViescolaireService} from "../ViescolaireService";
import {IStructure} from "@common/model";

describe('ViescolaireService', () => {
    it('returns data when API request is correctly called for getSchoolYearDates method', done => {
        const structureId = "structureId";
        const data = {response: true};
        const url = 'viescolaire/settings/periode/schoolyear?structureId=' + structureId;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        ViescolaireService.getSchoolYearDates("structureId").then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for getSlotProfile method', done => {
        const data = {response: true};
        const url = '/viescolaire/structures/structureId/time-slot';
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        ViescolaireService.getSlotProfile("structureId").then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for getStudent method', done => {
        const structureId = "structureId";
        const studentId = "studentId";
        const data = {response: true};
        const url = `viescolaire/eleves?idUser=${studentId}&idStructure=${structureId}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        ViescolaireService.getStudent("structureId", "studentId").then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for getBuildOwnStructure method', done => {
        const structure1: IStructure = {id: "id1", name: "name1"}
        const structure2: IStructure = {id: "id2", name: "name2"}
        const expected: Array<IStructure> = [structure1, structure2];

        model.me.structures.push("id1");
        model.me.structures.push("id2");

        model.me.structureNames.push("name1");
        model.me.structureNames.push("name2");

        expect(ViescolaireService.getBuildOwnStructure()).toEqual(expected);
        done();
    });
});
