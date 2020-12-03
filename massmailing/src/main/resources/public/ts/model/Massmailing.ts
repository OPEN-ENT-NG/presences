import {Template} from '../services/SettingsService';
import http, {AxiosRequestConfig, AxiosResponse} from 'axios';
import {DateUtils} from '@common/utils';
import {MailingType} from '../model/Mailing';
import {BlobUtil} from '@common/utils/blob';
import {IPunishmentType} from "@incidents/models/PunishmentType";
import {EVENT_TYPES} from "@common/model";

export interface IMassmailingFilterPreferences {
    start_at: number;
    status: {
        REGULARIZED: boolean
        UNREGULARIZED: boolean
        LATENESS: boolean,
        NO_REASON: boolean
    };
    massmailing_status: {
        mailed: boolean,
        waiting: boolean
    };
    allReasons: boolean;
    noReasons: boolean;
    reasons: any;
    punishments: any;
    punishmentTypes: IPunishmentType[];
    anomalies: {
        MAIL: boolean,
        SMS: boolean
    };
}

export interface IRelative {
    id: string;
    displayName: string;
    contact: string;
    selected: boolean;
    address: string;
}

export interface IMassmailingBody {
    event_types: Array<string>,
    template: number,
    structure: string,
    no_reason: boolean,
    start_at: number,
    reasons: Array<string>,
    punishmentsTypes: Array<number>,
    sanctionsTypes: Array<number>,
    start: string,
    end: string,
    students: {},
    massmailed: boolean
}

interface MassmailingStudent {
    id: string;
    selected: boolean;
    opened: boolean;
    displayName: string;
    className: string;
    events: {
        REGULARIZED?: number,
        UNREGULARIZED?: number,
        LATENESS?: number,
        NO_REASON?: number,
        PUNISHMENT?: number,
        SANCTION?: number
    };
    relative: IRelative[];
}

declare const window: any;

export class Massmailing {
    type: string;
    counts: {
        massmailing: number,
        students: number,
        anomalies: number
    };
    students: MassmailingStudent[];
    filter: any;
    template: Template;

    constructor(type, counts, students) {
        this.type = type;
        this.counts = counts;
        this.students = students;
        this.students.forEach((student: MassmailingStudent) => {
            student.relative.forEach((relative: IRelative) => {
                if (student.relative.length > 1) {
                    if (relative.address && (student.relative[0].address == student.relative[1].address)) {
                        student.relative[0].selected = (student.relative[0].contact !== null && student.relative[0].contact.trim() !== '')
                            || this.type === MailingType[MailingType.PDF];
                    } else {
                        relative.selected = (relative.contact !== null && relative.contact.trim() !== '') || this.type === MailingType[MailingType.PDF];
                    }
                } else {
                    relative.selected = (relative.contact !== null && relative.contact.trim() !== '') || this.type === MailingType[MailingType.PDF];
                }
                if (!relative.selected) this.counts.massmailing--;
            });
            let shouldSelect = false;
            student.relative.map(relative => (shouldSelect = (shouldSelect || relative.selected)));
            if ((student.relative.length > 0 && shouldSelect) || this.type === MailingType[MailingType.PDF]) {
                student.selected = true;
            }
            student.opened = false;
        });
    }

    private getStudents() {
        const students = {};
        this.students.forEach(function ({id, relative}) {
            students[id] = [];
            relative.forEach(function (relative) {
                if (relative.selected) students[id].push(relative.id);
            });
        });

        Object.keys(students).forEach(id => {
            if (students[id].length === 0) delete students[id];
        });

        return students;
    }

    private massmailed() {
        const {waiting, mailed} = this.filter.massmailing_status;
        if (mailed && waiting) {
            return null;
        }

        return mailed;
    }

    toJson(): IMassmailingBody {
        const event_types = Object.keys(this.filter.status).filter(type => this.filter.status[type]);
        const reasons = [];
        Object.keys(this.filter.reasons).forEach((reason) => {
            if (this.filter.reasons[reason]) reasons.push(reason);
        });

        const punishmentsTypes: Array<number> = this.filter.punishments
            .filter((punishmentType: IPunishmentType) => punishmentType.type === EVENT_TYPES.PUNISHMENT)
            .map((punishmentType: IPunishmentType) => punishmentType.id);

        const sanctionsTypes: Array<number> = this.filter.punishments
            .filter((punishmentType: IPunishmentType) => punishmentType.type === EVENT_TYPES.SANCTION)
            .map((punishmentType: IPunishmentType) => punishmentType.id);

        return {
            event_types,
            template: this.template.id,
            structure: window.structure.id,
            no_reason: this.filter.noReasons,
            start_at: this.filter.start_at,
            reasons,
            punishmentsTypes,
            sanctionsTypes,
            start: DateUtils.format(this.filter.start_date, DateUtils.FORMAT['YEAR-MONTH-DAY']),
            end: DateUtils.format(this.filter.end_date, DateUtils.FORMAT['YEAR-MONTH-DAY']),
            students: this.getStudents(),
            massmailed: this.massmailed()
        };
    }
    
    async process(): Promise<void> {
        if (this.type === MailingType[MailingType.PDF]) {
            const config: AxiosRequestConfig = {
                responseType: 'arraybuffer',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/pdf'
                }
            };
            http.post(`/massmailing/massmailings/${this.type}`, this.toJson(), config)
                .then((resp: AxiosResponse) => new BlobUtil(
                    BlobUtil.getFileNameByContentDisposition(resp.headers['content-disposition']),
                    [resp.data],
                    {type: 'application/pdf'})
                    .downloadPdf('massmailing.pdf.error.download'));

        } else {
            await http.post(`/massmailing/massmailings/${this.type}`, this.toJson());
        }
    }

    getRelativeCheckedCount(student: MassmailingStudent): number {
        let count = 0;
        student.relative.forEach(relative => (count += relative.selected ? 1 : 0));
        return count;
    }
}