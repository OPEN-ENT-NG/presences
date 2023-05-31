import http from 'axios';
import {Mix} from 'entcore-toolkit';
import {DateUtils} from '@common/utils'
import {LoadingCollection} from '@common/model'
import {IncidentType, Partner, Place, ProtagonistType, Seriousness} from "../../services";
import {model, moment} from "entcore";
import {User} from "@common/model/User";

export interface Incident {
    id: number;
    owner: User;
    structureId: string;
    structure_id?: string;

    date: Date;
    dateTime: Date;
    created: Date;

    selectedHour: boolean;
    selected_hour?: boolean;
    description: string;
    processed: boolean;

    place: Place;
    place_id?: number;
    partner: Partner;
    partner_id?: number;
    type_id?: number;

    type?: IncidentType;
    incidentType: IncidentType;
    incident_type?: IncidentType;

    seriousness: Seriousness;
    seriousness_id?: number;

    protagonists: ProtagonistForm[];
    protagonist_type_id?: number;
    protagonist?: ProtagonistType;
}

export interface ProtagonistForm {
    userId: string;
    user_id?: string;
    student?: User;

    label: string;

    incident_id?: number;

    protagonistType: ProtagonistType;
    type?: ProtagonistType;
    type_id?: number;
}

export class Incident {

    constructor(structureId) {
        this.id = null;
        this.owner = {
            id: model.me.userId
        };
        this.structureId = structureId;

        this.date = moment(new Date()).set({second: 0, millisecond: 0}).toDate();
        this.dateTime = moment(this.date).set({second: 0, millisecond: 0}).toDate();
        this.created = moment(new Date()).set({second: 0, millisecond: 0}).toDate();

        this.selectedHour = false;
        this.description = "";
        this.processed = false;

        this.place = null as Place;
        this.partner = null as Partner;
        this.incidentType = null as IncidentType;
        this.seriousness = null as Seriousness;

        this.protagonists = new Array<ProtagonistForm>();
    }

    static loadData(data: any[]) {
        let dataModel = [];

        data.forEach(response => {
            let protagonists = new Array<ProtagonistForm>();
            response.protagonists.forEach(protagonist => {
                let protagonistForm = {} as ProtagonistForm;
                protagonistForm.userId = protagonist.user_id;
                protagonistForm.label = protagonist.student != null ? protagonist.student.displayName : "";
                protagonistForm.protagonistType = protagonist.type;
                protagonists.push(protagonistForm);
            });

            dataModel.push({
                id: response.id ? response.id : null,
                owner: response.owner,
                structureId: response.structure_id,
                date: response.date,
                created: response.created,
                selectedHour: response.selected_hour,
                description: response.description,
                processed: response.processed,
                place: response.place,
                partner: response.partner,
                incidentType: response.incident_type,
                seriousness: response.seriousness,
                protagonists: protagonists
            });
        });
        return dataModel;
    };

    isIncidentFormValid(): boolean {
        // set empty object on undefined while selecting default value
        this.place = this.place == undefined ? null as Place : this.place;
        this.partner = this.partner == undefined ? null as Partner : this.partner;
        this.incidentType = this.incidentType == undefined ? null as IncidentType : this.incidentType;
        this.seriousness = this.seriousness == undefined ? null as Seriousness : this.seriousness;

        return this.owner
            && this.description
            && this.date
            && Object.keys(this.place).length !== 0
            && Object.keys(this.incidentType).length !== 0
            && Object.keys(this.seriousness).length !== 0
            && this.protagonists.length > 0;
    };

    toJson(): Object {
        let students = [];
        this.protagonists.forEach(protagonist => {
            students.push({user_id: protagonist.userId, type_id: protagonist.protagonistType.id})
        });
        let date = moment(moment(this.date).format('YYYY-MM-DD') + ' ' + moment(this.dateTime)
            .format('HH:mm'), 'YYYY-MM-DD HH:mm')
            .format('YYYY-MM-DD HH:mm');

        return {
            owner: this.owner,
            structure_id: this.structureId,
            date: date,
            selected_hour: this.selectedHour,
            description: this.description,
            created: moment(this.created).format('YYYY-MM-DD HH:mm'),
            processed: this.processed,
            place_id: this.place.id,
            partner_id: this.partner.id,
            type_id: this.incidentType.id,
            seriousness_id: this.seriousness.id,
            students: students
        }
    }

    async create() {
        try {
            return await http.post(`/incidents/incidents`, this.toJson());
        } catch (err) {
            throw err;
        }
    }

    async update() {
        try {
            return await http.put(`/incidents/incidents/${this.id}`, this.toJson());
        } catch (err) {
            throw err;
        }
    }

    async delete() {
        try {
            return await http.delete(`/incidents/incidents/${this.id}`);
        } catch (err) {
            throw err;
        }
    }
}

export class Incidents extends LoadingCollection {
    all: Incident[];
    pageCount: number;

    structureId: string;
    startDate: string;
    endDate: string;
    userId: string;
    audienceIds: string;
    order: string;
    reverse: boolean;

    constructor() {
        super();
        this.all = [];
    }

    async syncPagination(): Promise<void> {
        this.loading = true;
        let dateFormat: string = DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC'];
        try {
            let url: string =
                `/incidents/incidents?structureId=${this.structureId}` +
                `&startDate=${DateUtils.format(DateUtils.setFirstTime(this.startDate), dateFormat)}` +
                `&endDate=${DateUtils.format(DateUtils.setLastTime(this.endDate), dateFormat)}`;

            if (this.userId) {
                url += `&userId=${this.userId}`;
            }

            if (this.audienceIds) {
                url += `&audienceId=${this.audienceIds}`;
            }

            if (this.order) {
                url += `&order=${this.order}`;
            }

            if (this.reverse) {
                url += `&reverse=${this.reverse}`;
            }

            url += `&page=${this.page}`;
            const {data} = await http.get(url);
            this.pageCount = data.page_count;
            this.all = Mix.castArrayAs(Incident, Incident.loadData(data.all));
        } catch (err) {
            throw err;
        } finally {
            this.loading = false;
        }
    }

    async export(): Promise<void> {
        let dateFormat: string = DateUtils.FORMAT['YEAR-MONTH-DAY'];

        let url: string = `/incidents/incidents/export?structureId=${this.structureId}` +
            `&startDate=${DateUtils.format(DateUtils.setFirstTime(this.startDate), dateFormat) + " 00:00:00"}` +
            `&endDate=${DateUtils.format(DateUtils.setLastTime(this.endDate), dateFormat) + " 23:59:59"}`;

        if (this.userId) {
            url += `&userId=${this.userId}`;
        }

        if (this.audienceIds) {
            url += `&audienceId=${this.audienceIds}`;
        }

        window.open(url);
    }

}
