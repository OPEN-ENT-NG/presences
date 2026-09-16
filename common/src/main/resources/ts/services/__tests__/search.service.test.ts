jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';
import {SearchService, searchService} from "@common/services";

describe('TimeslotClasseService', () => {
    it('returns data when API request is correctly called for search method', done => {
        const data = [{type: "USER", displayName: "displayName1", groupName: "groupName1"},
            {type: "GROUP", displayName: "displayName2", groupName: "groupName2"}];
        const structureId = "sturctureId";
        const value = "value";
        const url = `/presences/search?structureId=${structureId}&q=${value}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        SearchService.search(structureId, value).then(res => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(res[0].toString()).toEqual("displayName1 - groupName1");
            expect(res[1].toString()).toEqual("displayName2");
            done();
        });
    });

    it('returns data when API request is correctly called for searchUser method', done => {
        const data = [{idClasse: ["3184$3EME1", "3487$3EME2"], classesNames: ["3EME2", "3EME1"], displayName: "displayName1"},
            {idClasse: ["AMIENS-2588141165", "AMIENS-1689416"], classesNames: ["CP", "CM1"], displayName: "displayName2"},
            {displayName: "displayName3"}];
        const structureId = "sturctureId";
        const value = "value";
        const profile = "profile";
        const url = `/presences/search/users?structureId=${structureId}&profile=${profile}&q=${value}&field=firstName&field=lastName`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        SearchService.searchUser(structureId, value, profile).then((res: Array<any>) => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(res[0].toString()).toEqual("displayName1 - 3EME1,3EME2");
            expect(res[1].toString()).toEqual("displayName2 - CM1,CP");
            expect(res[2].toString()).toEqual("displayName3");

            expect(res[0].idClasse).toEqual(["3EME1", "3EME2"]);
            expect(res[1].idClasse).toEqual(["AMIENS-2588141165", "AMIENS-1689416"]);
            expect(res[0].classesNames).toEqual(["3EME1", "3EME2"]);
            expect(res[1].classesNames).toEqual(["CM1", "CP"]);
            done();
        });
    });

    it('returns data when API request is correctly called for searchStudents method', done => {
        const data = [{idClasse: ["3184$3EME1", "3487$3EME2"], classesNames: ["3EME2", "3EME1"], displayName: "displayName1"},
            {idClasse: ["AMIENS-2588141165", "AMIENS-1689416"], classesNames: ["CP", "CM1"], displayName: "displayName2"},
            {displayName: "displayName3"}];
        const structureId = "sturctureId";
        const value = "value";
        const url = `/presences/search/students?structureId=${structureId}&q=${value}&field=firstName&field=lastName`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        SearchService.searchStudents(structureId, value).then((res: Array<any>) => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(res[0].toString()).toEqual("displayName1 - 3EME1,3EME2");
            expect(res[1].toString()).toEqual("displayName2 - CM1,CP");
            expect(res[2].toString()).toEqual("displayName3");

            expect(res[0].idClasse).toEqual(["3EME1", "3EME2"]);
            expect(res[1].idClasse).toEqual(["AMIENS-2588141165", "AMIENS-1689416"]);
            expect(res[0].classesNames).toEqual(["3EME1", "3EME2"]);
            expect(res[1].classesNames).toEqual(["CM1", "CP"]);
            done();
        });
    });
});
