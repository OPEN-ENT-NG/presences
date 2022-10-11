// tricks to fake "mock" entcore ng class in order to use service
import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {SearchService, searchService} from "@common/services";

describe('TimeslotClasseService', () => {
    it('returns data when API request is correctly called for search method', done => {
        var mock = new MockAdapter(axios);
        const data = [{type: "USER", displayName: "displayName1", groupName: "groupName1"},
            {type: "GROUP", displayName: "displayName2", groupName: "groupName2"}];
        const structureId = "sturctureId";
        const value = "value";

        mock.onGet(`/presences/search?structureId=${structureId}&q=${value}`).reply(200, data);

        SearchService.search(structureId, value).then(res => {
            expect(res[0].toString()).toEqual("displayName1 - groupName1");
            expect(res[1].toString()).toEqual("displayName2");
            done();
        });
    });

    it('returns data when API request is correctly called for searchUser method', done => {
        var mock = new MockAdapter(axios);
        const data = [{idClasse: ["3184$3EME1", "3487$3EME2"], classesNames: ["3EME2", "3EME1"], displayName: "displayName1"},
            {idClasse: ["AMIENS-2588141165", "AMIENS-1689416"], classesNames: ["CP", "CM1"], displayName: "displayName2"},
            {displayName: "displayName3"}];
        const structureId = "sturctureId";
        const value = "value";
        const profile = "profile";

        mock.onGet(`/presences/search/users?structureId=${structureId}&profile=${profile}&q=${value}&field=firstName&field=lastName`)
            .reply(200, data);

        SearchService.searchUser(structureId, value, profile).then((res: Array<any>) => {
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
        var mock = new MockAdapter(axios);
        const data = [{idClasse: ["3184$3EME1", "3487$3EME2"], classesNames: ["3EME2", "3EME1"], displayName: "displayName1"},
            {idClasse: ["AMIENS-2588141165", "AMIENS-1689416"], classesNames: ["CP", "CM1"], displayName: "displayName2"},
            {displayName: "displayName3"}];
        const structureId = "sturctureId";
        const value = "value";

        mock.onGet(`/presences/search/students?structureId=${structureId}&q=${value}&field=firstName&field=lastName`)
            .reply(200, data);

        SearchService.searchStudents(structureId, value).then((res: Array<any>) => {
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