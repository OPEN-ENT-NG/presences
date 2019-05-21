import http from 'axios';
import {Mix} from 'entcore-toolkit';
import {DateUtils} from "@common/utils";
import {LoadingCollection} from "@common/model";
import {_, moment} from "entcore";

export interface Exemption {
    id: string;
    studentId: string;
    structureId: string;
    subjectId: string;
    startDate: Date;
    endDate: Date;
    comment: string;
    attendance: boolean;
    students: any;
    subject: any;

}

export class Exemption {
    constructor(structureId, form?) {
        this.structureId = structureId;
        this.startDate = new Date();
        this.endDate = DateUtils.add(new Date(), 7, "d");
        this.comment = "";
        this.attendance = false;
        this.attendance = false;
        this.students = null;
        if (form == true) {
            this.students = [];
        }
    }


    static loadData(data: any[]) {
        let dataModel = [];
        data.forEach(i => {
            dataModel.push({
                id: i.id ? i.id : null,
                studentId: i.student_id,
                structureId: i.structure_id,
                subjectId: i.subject_id,
                startDate: i.start_date,
                endDate: i.end_date,
                comment: i.comment,
                attendance: i.attendance,
                student: i.student,
                subject: i.subject
            });
        });
        return dataModel;
    };

    toJson() {
        let exemp = {
            "structure_id": this.structureId,
            "subject_id": this.subject.id,
            "start_date": moment(this.startDate).format('YYYY-MM-DD'),
            "end_date": moment(this.endDate).format('YYYY-MM-DD'),
            "attendance": this.attendance,
            "comment": this.comment
        };

        if (!this.id) {
            exemp["student_id"] = _.pluck(this.students, 'id');
        } else {
            exemp["student_id"] = this.studentId;
        }
        return exemp;
    };

    isValidOnForm() {
        return this.startDate
            && this.endDate
            && this.subject
            && this.structureId
            && this.students
            && this.students.length;
    };

    async save(structure: string, start_date: string, end_date: string, students?: string[], audiences?: string[]) {
        if (this.id) {
            let url = `/presences/exemption/${this.id}`;
            return await http.put(url, this.toJson());
        } else {
            let url = `/presences/exemptions`;
            return await http.post(url, this.toJson());
        }
    }

    async delete(structure: string, start_date: string, end_date: string, students?: string[], audiences?: string[]) {
        let url = `/presences/exemption?id=${this.id}`;
        return await http.delete(url,);
    }

}

export class Exemptions extends LoadingCollection {
    all: Exemption[];
    pageCount: number;
    structureId: string;
    start_date: string;
    end_date: string;
    students?: string[];
    audiences?: string[];

    constructor() {
        super();
        this.all = [];
    }

    async prepareSync(structureId: string, start_date: string, end_date: string, studentsFiltered?: any[], audiencesFiltered?: any[]) {
        this.structureId = structureId;
        this.start_date = start_date;
        this.end_date = end_date;
        this.students = studentsFiltered ? _.pluck(studentsFiltered, 'id') : null;
        this.audiences = audiencesFiltered ? _.pluck(audiencesFiltered, 'id') : null;
        this.page = 0;
    }

    async syncPagination() {
        this.loading = true;
        let dateFormat = DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC'];
        try {
            let url = `/presences/exemptions?structure_id=${this.structureId}` +
                `&start_date=${DateUtils.format(DateUtils.setFirstTime(this.start_date), dateFormat)}` +
                `&end_date=${DateUtils.format(DateUtils.setLastTime(this.end_date), dateFormat)}`;
            if (this.students && this.students.length > 0) {
                url += `&student_id=${this.students.join(',')}`;
            }
            if (this.audiences && this.audiences.length > 0) {
                url += `&audience_id=${this.audiences.join(',')}`;
            }
            url += `&page=${this.page? this.page:0}`;
            const {data} = await http.get(url);
            this.all = Mix.castArrayAs(Exemption, Exemption.loadData(data.values));
            this.pageCount = data.page_count;

        } catch (err) {
            throw err;
        }
        this.loading = false;
    }
}
