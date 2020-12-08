import {LoadingCollection} from '@common/model';
import {_, moment} from 'entcore';
import {Student} from '@common/model/Student';
import {User} from '@common/model/User';
import {IMetadata} from '@common/model/Metadata';

export enum MailingType {
    PDF,
    MAIL,
    SMS
}

export interface MailingEvent {
    id: number;
    mailing_id: number;
    event_id: string;
    event_type: string;
}

export interface Mailing {
    id: number;
    student: Student;
    mailing_event: MailingEvent;
    recipient: User;
    structure_id: string;
    type: string;
    content: string;
    created: string;
    timestamp?: number;
    isSelected?: boolean;
    file_id?: string;
    metadata?: IMetadata;
}

export interface MailingRequest {
    structure: string;
    start: string;
    end: string;
    mailTypes: Array<String>;
    event_types: Array<String>;
    students: Array<String>;
    groups: Array<String>;
    page: number;
}

export interface MailingResponse {
    page: number;
    page_count: number;
    all: Array<Mailing>;
}

export class Mailings extends LoadingCollection {
    structure_id: string;
    mailingResponse: MailingResponse;
    map: any;
    keysOrder: Array<String>;

    constructor(structure_id) {
        super();
        this.structure_id = structure_id;
        this.mailingResponse = {} as MailingResponse;
        this.map = {};
        this.keysOrder = [];
    }

    async build(data: MailingResponse): Promise<void> {
        this.mailingResponse.all = [];
        data.all.forEach((mailing: Mailing) => {
            this.mailingResponse.all.push(mailing);
        });
        this.mailingResponse.all.map((mailing: Mailing) => mailing.timestamp = moment(mailing.created).valueOf());
        this.mailingResponse.all = _.sortBy(this.mailingResponse.all, 'timestamp');
        this.mailingResponse.page = data.page;
        this.mailingResponse.page_count = data.page_count;
        this.map = this.groupByDate();
        this.keysOrder = Object.keys(this.map).reverse();
    }

    groupByDate(): {} {
        const map = {};
        this.mailingResponse.all.forEach(mailing => {
            const start = moment(mailing.created).startOf('day').valueOf();
            if (!(start in map)) {
                map[start] = [];
            }
            map[start].push(mailing);
        });
        return map;
    }
}