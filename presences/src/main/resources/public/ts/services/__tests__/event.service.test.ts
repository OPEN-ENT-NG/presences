import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {EventRequest, eventService} from "@presences/services";
import {ActionBody, IEventBody, IStudentEventRequest} from "@presences/models";
import {ExportType} from "@common/core/enum/export-type.enum";

describe('EventService', () => {
    it('check get methode in EventService', done => {
        const mock = new MockAdapter(axios);
        const eventRequest : EventRequest = {
            classes: "classes1,classes2",
            classesIds: ["classesId1", "classesId2"],
            endDate: "2000-02-01",
            endTime: "endTime",
            eventType: "4,5",
            followed: true,
            listReasonIds: "1,2,3",
            noReason: true,
            noReasonLateness: true,
            notFollowed: true,
            page: 7,
            regularized: true,
            startDate: "2000-01-02",
            startTime: "startTime",
            structureId: "structureId",
            userId: "userId",
            userIds: ["userId1", "userId2"]
        }
        const data = {all: [], page_count: 3};
        const result = {all: [], events: [], pageCount: 3};
        mock.onGet(`/presences/events?structureId=${eventRequest.structureId}&startDate=${eventRequest.startDate}&endDate=${eventRequest.endDate}` +
            `&startTime=${eventRequest.startTime}&endTime=${eventRequest.endTime}&noReason=${eventRequest.noReason}&noReasonLateness=${eventRequest.noReasonLateness}` +
            `&eventType=${eventRequest.eventType}&reasonIds=${eventRequest.listReasonIds}&userId=${eventRequest.userId}&userId=${eventRequest.userIds[0]}` +
            `&userId=${eventRequest.userIds[1]}&classes=${eventRequest.classes}&classes=${eventRequest.classesIds[0]}&classes=${eventRequest.classesIds[1]}` +
            `&regularized=${eventRequest.regularized}&page=${eventRequest.page}`)
            .reply(200, data);

        eventService.get(eventRequest).then(response => {
            expect(response).toEqual(result);
            done();
        });
    });

    it('check get methode in EventService', done => {
        const mock = new MockAdapter(axios);
        const eventRequest : EventRequest = {
            classes: "classes1,classes2",
            classesIds: ["classesId1", "classesId2"],
            endDate: "2000-02-01",
            endTime: "endTime",
            eventType: "4,5",
            followed: true,
            listReasonIds: "",
            noReason: true,
            noReasonLateness: false,
            notFollowed: false,
            page: 7,
            regularized: false,
            startDate: "2000-01-02",
            startTime: "startTime",
            structureId: "structureId",
            userId: "userId",
            userIds: ["userId1", "userId2"]
        }
        const data = {all: [], page_count: 3};
        const result = {all: [], events: [], pageCount: 3};
        mock.onGet(`/presences/events?structureId=${eventRequest.structureId}&startDate=${eventRequest.startDate}&endDate=${eventRequest.endDate}` +
            `&startTime=${eventRequest.startTime}&endTime=${eventRequest.endTime}&noReason=${eventRequest.noReason}` +
            `&eventType=${eventRequest.eventType}&userId=${eventRequest.userId}&userId=${eventRequest.userIds[0]}` +
            `&userId=${eventRequest.userIds[1]}&classes=${eventRequest.classes}&classes=${eventRequest.classesIds[0]}&classes=${eventRequest.classesIds[1]}` +
            `&regularized=${eventRequest.regularized}&followed=${!eventRequest.notFollowed}&page=${eventRequest.page}`)
            .reply(200, data);

        eventService.get(eventRequest).then(response => {
            expect(response).toEqual(result);
            done();
        });
    });

    it('check getEventActions methode in EventService', done => {
        const mock = new MockAdapter(axios);

        const data = {response: true};
        const eventId = 3;
        mock.onGet(`/presences/events/${eventId}/actions`)
            .reply(200, data);

        eventService.getEventActions(eventId).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('check createAction methode in EventService', done => {
        const mock = new MockAdapter(axios);

        const data = {response: true};
        const actionBody : ActionBody = {actionId: 0, comment: "", eventId: [3], owner: ""};
        mock.onPost(`/presences/events/actions`, actionBody)
            .reply(200, data);

        eventService.createAction(actionBody).then(response => {
            expect(response.data).toEqual(data);
            done();
        });
    });

    it('check getStudentEvent methode in EventService', done => {
        const mock = new MockAdapter(axios);

        const data = {response: true};
        let studentEventRequest : IStudentEventRequest = {
            end_at: "end_at",
            limit: 4,
            offset: 5,
            start_at: "start_at",
            structure_id: "structure_id",
            student_id: "student_id",
            type: ["1","3"]
        };
        mock.onGet(`/presences/students/student_id/events?structure_id=${studentEventRequest.structure_id}` +
            `&start_at=${studentEventRequest.start_at}&end_at=${studentEventRequest.end_at}&type=${studentEventRequest.type[0]}` +
            `&type=${studentEventRequest.type[1]}&limit=${studentEventRequest.limit}&offset=${studentEventRequest.offset}`)
            .reply(200, data);

        eventService.getStudentEvent(studentEventRequest).then(response => {
            expect(response).toEqual(data);
            done();
        });

        studentEventRequest = {
            end_at: "end_at",
            limit: undefined,
            offset: undefined,
            start_at: "start_at",
            structure_id: "structure_id",
            student_id: "student_id",
            type: ["1","3"]
        };

        mock.onGet(`/presences/students/student_id/events?structure_id=${studentEventRequest.structure_id}` +
            `&start_at=${studentEventRequest.start_at}&end_at=${studentEventRequest.end_at}&type=${studentEventRequest.type[0]}` +
            `&type=${studentEventRequest.type[1]}`)
            .reply(200, data);

        eventService.getStudentEvent(studentEventRequest).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('check export methode in EventService', done => {
        const actionBody : ActionBody = {actionId: 0, comment: "", eventId: undefined, owner: ""};
        const eventRequest : EventRequest = {
            classes: "classes1,classes2",
            classesIds: ["classesId1", "classesId2"],
            endDate: "2000-02-01",
            endTime: "endTime",
            eventType: "4,5",
            followed: true,
            listReasonIds: "1,2,3",
            noReason: true,
            noReasonLateness: true,
            notFollowed: true,
            page: 7,
            regularized: true,
            startDate: "2000-01-02",
            startTime: "startTime",
            structureId: "structureId",
            userId: "userId",
            userIds: ["userId1", "userId2"]
        }
        const exportType: ExportType = 'CSV';
        const res: string = eventService.getExportRequest(eventRequest, exportType);
        expect(res).toEqual(`/presences/events/export?type=${exportType}&structureId=${eventRequest.structureId}&startDate=${eventRequest.startDate}&endDate=${eventRequest.endDate}&noReason=${eventRequest.noReason}&noReasonLateness=${eventRequest.noReasonLateness}&eventType=${eventRequest.eventType}&reasonIds=${eventRequest.listReasonIds}&userId=${eventRequest.userId}&classes=${eventRequest.classes}&regularized=${eventRequest.regularized}`)
        done();
    });

    it('check getAbsentsCounts methode in EventService', done => {
        const mock = new MockAdapter(axios);

        const data = {response: true};
        //Must find a way to mock currentdate
        const startDate: string = "startDate";
        const endDate: string = "endDate";

        mock.onGet(`/presences/events/absences/summary&startAt=${startDate}&endAt=${endDate}`)
            .reply(200, data);

        eventService.getAbsentsCounts(undefined, startDate, endDate).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('check createLatenessEvent methode in EventService', done => {
        const mock = new MockAdapter(axios);

        const data = {response: true};
        //Must find a way to mock currentdate
        const structureId: string = "structureId";
        const eventBody: IEventBody = {end_date: "", reason_id: 0, register_id: 0, start_date: "", student_id: ""};

        mock.onPost(`/presences/events/${structureId}/lateness`, eventBody)
            .reply(200, data);

        eventService.createLatenessEvent(eventBody, structureId).then(response => {
            expect(response.data).toEqual(data);
            done();
        });
    });

    it('check updateEvent methode in EventService', done => {
        const mock = new MockAdapter(axios);

        const data = {response: true};
        //Must find a way to mock currentdate
        const eventId = 3;
        const eventBody: IEventBody = {end_date: "", reason_id: 0, register_id: 0, start_date: "", student_id: ""};

        mock.onPut(`/presences/events/${eventId}`, eventBody)
            .reply(200, data);

        eventService.updateEvent(eventId, eventBody).then(response => {
            expect(response.data).toEqual(data);
            done();
        });
    });

    it('check deleteEvent methode in EventService', done => {
        const mock = new MockAdapter(axios);

        const data = {response: true};
        //Must find a way to mock currentdate
        const eventId = 3;

        mock.onDelete(`/presences/events/${eventId}`)
            .reply(200, data);

        eventService.deleteEvent(eventId).then(response => {
            expect(response.data).toEqual(data);
            done();
        });
    });
});
