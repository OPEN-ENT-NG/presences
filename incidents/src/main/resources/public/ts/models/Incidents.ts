import http from 'axios';
import {Mix} from 'entcore-toolkit';
import {DateUtils} from '@common/utils'
import {LoadingCollection} from '@common/model'
import {IncidentType, Partner, Place, ProtagonistType, SeriousnessLevel} from "../services";
import {model, moment} from "entcore";

export interface Incident {
    id: number;
    owner: string;
    structureId: string;

    date: Date;
    created: Date;

    selectedHour: boolean;
    description: string;
    processed: boolean;

    place: Place;
    partner: Partner;
    incidentType: IncidentType;
    seriousness: SeriousnessLevel;

    protagonists: ProtagonistForm[];
}

export interface ProtagonistForm {
    userId: string;
    label: string;
    protagonistType: ProtagonistType;
}

export class Incident {

    constructor(structureId) {
        this.id = null;
        this.owner = model.me.login;
        this.structureId = structureId;

        this.date = moment(new Date()).set({second: 0, millisecond: 0}).toDate();
        this.created = moment(new Date()).set({second: 0, millisecond: 0}).toDate();

        this.selectedHour = false;
        this.description = "";
        this.processed = false;

        this.place = {} as Place;
        this.partner = {} as Partner;
        this.incidentType = {} as IncidentType;
        this.seriousness = {} as SeriousnessLevel;

        this.protagonists = new Array<ProtagonistForm>();
    }

    static loadData(data: any[]) {
        let dataModel = [];

        data.forEach(response => {
            let protagonists = new Array<ProtagonistForm>();
            response.protagonists.forEach(protagonist => {
                let protagonistForm = {} as ProtagonistForm;
                protagonistForm.userId = protagonist.user_id;
                protagonistForm.label = protagonist.student.firstName + ' ' + protagonist.student.lastName;
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
        this.place = this.place == undefined ? {} as Place : this.place;
        this.partner = this.partner == undefined ? {} as Partner : this.partner;
        this.incidentType = this.incidentType == undefined ? {} as IncidentType : this.incidentType;
        this.seriousness = this.seriousness == undefined ? {} as SeriousnessLevel : this.seriousness;

        return this.owner
            && this.description
            && this.date
            && Object.keys(this.place).length !== 0
            && Object.keys(this.partner).length !== 0
            && Object.keys(this.incidentType).length !== 0
            && Object.keys(this.seriousness).length !== 0
            && this.protagonists.length > 0;
    };

    toJson(): Object {
        let students = [];
        this.protagonists.forEach(protagonist => {
            students.push({user_id: protagonist.userId, type_id: protagonist.protagonistType.id})
        });
        return {
            owner: this.owner,
            structure_id: this.structureId,
            date: moment(this.date).format('YYYY-MM-DD HH:mm'),
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

    constructor() {
        super();
        this.all = [];
    }

    async syncPagination() {
        this.loading = true;
        let dateFormat = DateUtils.FORMAT['YEAR-MONTH-DAY'];
        try {
            let url =
                `/incidents/incidents?structureId=${this.structureId}` +
                `&startDate=${DateUtils.format(DateUtils.setFirstTime(this.startDate), dateFormat)}` +
                `&endDate=${DateUtils.format(DateUtils.setLastTime(this.endDate), dateFormat)}`;

            if (this.userId) {
                url += `&userId=${this.userId}`;
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

    async export() {
        let dateFormat = DateUtils.FORMAT['YEAR-MONTH-DAY'];

        let url = `/incidents/incidents/export?structureId=${this.structureId}` +
            `&startDate=${DateUtils.format(DateUtils.setFirstTime(this.startDate), dateFormat)}` +
            `&endDate=${DateUtils.format(DateUtils.setLastTime(this.endDate), dateFormat)}`;

        if (this.userId) {
            url += `&userId=${this.userId}`;
        }
        window.open(url);
    }

}
