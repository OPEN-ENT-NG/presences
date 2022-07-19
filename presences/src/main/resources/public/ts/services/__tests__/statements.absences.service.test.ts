import {statementsAbsencesService} from "@presences/services";

describe('StatementsAbsenceService', () => {
    it('returns data when export request is correctly called', done => {
        const statementsAbsencesRequest = {
            structure_id: "structureId",
            start_at: "2000-01-01 00:00:00",
            end_at: "2020-01-01 23:59:59",
            page: null,
            isTreated: true
        };

        const res: string = statementsAbsencesService.export(statementsAbsencesRequest);
        expect(res).toEqual(`/presences/statements/absences/export?structure_id=${
            statementsAbsencesRequest.structure_id}&start_at=${statementsAbsencesRequest.start_at}&end_at=${
            statementsAbsencesRequest.end_at}`);
        done();
    });
});