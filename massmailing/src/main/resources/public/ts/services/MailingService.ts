import {ng} from 'entcore'
import http from 'axios';
import {Mailing, MailingRequest, MailingResponse} from "../model";

export interface MailingService {
    /**
     * Retrieve mailings history
     * @param mailingRequest MailingRequest param data
     */
    get(mailingRequest: MailingRequest): Promise<MailingResponse>;
    
    downloadFile(mailing: Mailing): void;
}

export const mailingService: MailingService = {
    get: async (mailingRequest: MailingRequest): Promise<MailingResponse> => {
        try {

            /* Retrieve students fetched */
            let studentParams = '';
            if (mailingRequest.students) {
                mailingRequest.students.forEach(student => {
                    studentParams += `&student=${student}`;
                });
            }

            /* Retrieve groups fetched */
            let groupParams = '';
            if (mailingRequest.groups) {
                mailingRequest.groups.forEach(group => {
                    groupParams += `&group=${group}`;
                });
            }

            /* Retrieve mailTypes fetched */
            let mailTypeParams = '';
            if (mailingRequest.mailTypes) {
                mailingRequest.mailTypes.forEach(mailType => {
                    mailTypeParams += `&mailType=${mailType}`;
                });
            }

            /* Retrieve event_types fetched */
            let eventTypeParams = '';
            if (mailingRequest.event_types) {
                mailingRequest.event_types.forEach(event_type => {
                    eventTypeParams += `&type=${event_type}`;
                });
            }

            const structureUrl = `?structure=${mailingRequest.structure}`;
            const dateUrl = `&start=${mailingRequest.start}&end=${mailingRequest.end}`;
            const paramsUrl = `${studentParams}${groupParams}${mailTypeParams}${eventTypeParams}`;
            const pageUrl = `&page=${mailingRequest.page}`;
            const {data} = await http.get(`/massmailing/mailings${structureUrl}${dateUrl}${paramsUrl}${pageUrl}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    downloadFile(mailing: Mailing): void {
        const basicUrl: string = `/massmailing/mailings/`; //mailings/:idMailing/file/:id
        const urlFetchingData: string = `${mailing.id}/file/${mailing.file_id}`;
        const structure_id: string = `?structure=${mailing.structure_id}`;
        window.open(`${basicUrl}${urlFetchingData}${structure_id}`);
    }
};

export const MailingService = ng.service('MailingService', (): MailingService => mailingService);
