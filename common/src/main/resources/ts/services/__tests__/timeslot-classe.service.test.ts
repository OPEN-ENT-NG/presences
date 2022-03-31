// tricks to fake "mock" entcore ng class in order to use service
import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {timeslotClasseService, TimeslotClasseService} from "@common/services/TimeslotClasseService";
import {IStructureSlot} from "@common/model";

describe('TimeslotClasseService', () => {
    it('returns data when API request is correctly called for getAudienceTimeslot method', done => {
        const audienceId = "audienceId";
        var mock = new MockAdapter(axios);
        const data: IStructureSlot = {_id: "", name: "", slots: []};

        mock.onGet(`/viescolaire/timeslot/audience/${audienceId}`).reply(200, data);

        timeslotClasseService.getAudienceTimeslot("audienceId").then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for getAllClassFromTimeslot method', done => {
        const timeslotId = "timeslotId";
        var mock = new MockAdapter(axios);
        const data = ["ok"];

        mock.onGet(`/viescolaire/timeslot/${timeslotId}`).reply(200, data);

        timeslotClasseService.getAllClassFromTimeslot("timeslotId").then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for createOrUpdateClassTimeslot method', done => {
        const timeslotId = "timeslotId";
        const classId = "classId";
        var mock = new MockAdapter(axios);
        const data = {response: true};

        mock.onPost(`/viescolaire/timeslot/audience`, {timeslot_id: timeslotId, class_id: classId}).reply(200, data);

        timeslotClasseService.createOrUpdateClassTimeslot("timeslotId", "classId").then(response => {
            expect(response.data).toEqual(data);
            expect(response.config.url).toEqual(`/viescolaire/timeslot/audience`);
            expect(response.config.data).toEqual(JSON.stringify({timeslot_id: timeslotId, class_id: classId}));
            done();
        });
    });

    it('returns data when API request is correctly called for deleteAllAudienceFromTimeslot method', done => {
        const timeslotId = "timeslotId";
        var mock = new MockAdapter(axios);
        const data = {response: true};

        mock.onDelete(`/viescolaire/timeslot/${timeslotId}`).reply(200, data);

        timeslotClasseService.deleteAllAudienceFromTimeslot("timeslotId").then(response => {
            expect(response.data).toEqual(data);
            expect(response.config.url).toEqual(`/viescolaire/timeslot/${timeslotId}`);
            done();
        });
    });

    it('returns data when API request is correctly called for getAudienceTimeslot method', done => {
        const classId = "classId";
        var mock = new MockAdapter(axios);
        const data = {response: true};

        mock.onDelete(`/viescolaire/timeslot/audience/${classId}`).reply(200, data);

        timeslotClasseService.deleteClassTimeslot("classId").then(response => {
            expect(response.data).toEqual(data);
            expect(response.config.url).toEqual(`/viescolaire/timeslot/audience/${classId}`);
            done();
        });
    });
});