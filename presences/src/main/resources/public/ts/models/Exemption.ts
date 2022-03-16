import http from 'axios';
import {Mix} from 'entcore-toolkit';
import {DateUtils} from "@common/utils";
import {ITimeSlot, LoadingCollection} from "@common/model";
import {_, moment} from "entcore";

export interface ExemptionRegister {
    attendance?: boolean;
    subject_id?: string;
    recursive_id?: number;
}

export class ExemptionView {
    exemption_id?: number;
    exemption_recursive_id?: number;
    start_date?: string;
    end_date?: string;
    isEveryTwoWeeks?: boolean;
    is_every_two_weeks?: boolean;
    structure_id?: string;
    student?: any;
    student_id?: string;
    subject_id?: string;
    recursive_id?: number;
    timeSlotTimePeriod?: {
        start: ITimeSlot;
        end: ITimeSlot;
    };
    day_of_week?: Array<string>;
    dayOfWeek?: Array<string>;
    startDateRecursive?: string;
    endDateRecursive?: string;
    isRecursiveMode?: boolean;
}

export interface IExemption extends ExemptionView {
    id: string;
    studentId: string;
    structureId: string;
    subjectId: string;
    startDate: any;
    endDate: any;
    comment: string;
    attendance: boolean;
    students?: any;
    subject: any;
}

export class Exemption extends ExemptionView {
    id: string;
    studentId: string;
    structureId: string;
    subjectId: string;
    startDate: any;
    endDate: any;
    comment: string;
    attendance: boolean;
    students: any;
    subject: any;

    constructor(structureId, form?) {
        super();
        this.structureId = structureId;
        this.startDate = new Date();
        this.endDate = DateUtils.add(new Date(), 7, "d");
        this.comment = "";
        this.attendance = false;
        this.students = null;
        if (form == true) {
            this.isRecursiveMode = false;
            this.students = [];
        }
    }

    static loadData(exemptions: Array<Exemption>): Array<IExemption> {
        let dataModel: Array<IExemption> = [];
        exemptions.map(exemption => exemption.student.displayName = exemption.student.lastName + " " + exemption.student.firstName);
        exemptions.forEach((exemption: IExemption) => {

            let exemptionElement: IExemption = {
                id: exemption.id ? exemption.id : null,
                exemption_id: exemption.exemption_id ? exemption.exemption_id : null,
                exemption_recursive_id: exemption.exemption_recursive_id ? exemption.exemption_recursive_id : null,
                studentId: exemption.student_id,
                structureId: exemption.structure_id,
                subjectId: exemption.subject_id,
                startDate: exemption.start_date,
                endDate: exemption.end_date,
                comment: exemption.comment,
                attendance: exemption.attendance,
                recursive_id: exemption.recursive_id,
                student: exemption.student,
                subject: exemption.subject,
                isEveryTwoWeeks: exemption.is_every_two_weeks,
                day_of_week: exemption.day_of_week
            } as IExemption;

            dataModel.push(exemptionElement);
        });
        return dataModel;
    };

    toJson() {
        let exemp = {
            "structure_id": this.structureId,
            "subject_id": this.subject.id,
            "start_date": moment(this.startDate)
                .set({hour:0,minute:0,second:0})
                .format(DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']),
            "end_date": moment(this.endDate)
                .set({hour:23,minute:59,second:59})
                .format(DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']),
            "attendance": this.attendance,
            "comment": this.comment
        };

        if (!this.id) {
            exemp["student_id"] = _.pluck(this.students, 'id');
        } else {
            exemp["student_id"] = this.studentId;
        }
        if (this.timeSlotTimePeriod || this.isRecursiveMode || this.exemption_recursive_id) {
            exemp["is_recursive"] = this.isRecursiveMode = true;
            exemp["is_every_two_weeks"] = this.isEveryTwoWeeks;
            exemp["startDateRecursive"] = this.startDateRecursive;
            exemp["endDateRecursive"] = this.endDateRecursive;
            exemp["day_of_week"] = this.day_of_week;
        }
        return exemp;
    };

    isValidOnForm() {
        let startDate = moment(this.startDate).format('YYYY-MM-DD');
        let endDate = moment(this.endDate).format('YYYY-MM-DD');
        let isPunctualFormValid = startDate
            && endDate
            && this.subject
            && this.structureId
            && this.students
            && this.students.length
            && startDate <= endDate;
        let isRecursiveFormValid = startDate
            && endDate
            && this.structureId
            && this.students
            && this.students.length
            && (this.day_of_week && this.day_of_week.length > 0)
            && ((this.startDateRecursive && this.endDateRecursive) && (this.startDateRecursive <= this.endDateRecursive))
            && startDate <= endDate;
        return this.isRecursiveMode ? isRecursiveFormValid : isPunctualFormValid;
    };

    async save(structure?: string, start_date?: string, end_date?: string, students?: string[], audiences?: string[]) {
        if (this.exemption_id || this.exemption_recursive_id) {
            let id: number = this.exemption_id || this.exemption_recursive_id;
            let url = `/presences/exemption/${id}`;
            return http.put(url, this.toJson());
        } else {
            let url = `/presences/exemptions`;
            return http.post(url, this.toJson());
        }
    }

    async delete(structure?: string, start_date?: string, end_date?: string, students?: string[], audiences?: string[]) {
        let url: string;
        if (this.exemption_id) {
            url = `/presences/exemption?id=${this.exemption_id}`;
        } else {
            url = `/presences/exemption/recursive?id=${this.exemption_recursive_id}`;
        }
        return http.delete(url);
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
    order: string;
    reverse: boolean;

    constructor() {
        super();
        this.all = [];
    }

    prepareUrl = () => {
        let dateFormat = DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC'];
        let url = `?structure_id=${this.structureId}` +
            `&start_date=${DateUtils.format(DateUtils.setFirstTime(this.start_date), dateFormat)}` +
            `&end_date=${DateUtils.format(DateUtils.setLastTime(this.end_date), dateFormat)}`;
        if (this.students && this.students.length > 0) {
            url += `&student_id=${this.students.join(',')}`;
        }
        if (this.audiences && this.audiences.length > 0) {
            url += `&audience_id=${this.audiences.join(',')}`;
        }
        return url;
    };

    prepareSync = (structureId: string, start_date: string, end_date: string, studentsFiltered?: any[], audiencesFiltered?: any[]) => {
        this.structureId = structureId;
        this.start_date = start_date;
        this.end_date = end_date;
        this.students = studentsFiltered ? _.pluck(studentsFiltered, 'id') : null;
        this.audiences = audiencesFiltered ? _.pluck(audiencesFiltered, 'id') : null;
    };

    async prepareSyncPaginate(structureId: string, start_date: string, end_date: string, studentsFiltered?: any[], audiencesFiltered?: any[]) {
        this.prepareSync(structureId, start_date, end_date, studentsFiltered, audiencesFiltered);
        this.page = 0; //auto sync
    }

    async syncPagination() {
        this.loading = true;
        try {
            let url = `/presences/exemptions` + this.prepareUrl();
            url += `&page=${this.page ? this.page : 0}`;
            if (this.order) {
                url += `&order=${this.order}`;
            }
            if (this.reverse) {
                url += `&reverse=${this.reverse}`;
            }
            const {data} = await http.get(url);
            this.all = Mix.castArrayAs(Exemption, Exemption.loadData(data.values));
            this.pageCount = data.page_count;
        } catch (err) {
            throw err;
        }
        this.loading = false;
    }

    export(structureId: string, start_date: string, end_date: string, studentsFiltered?: any[], audiencesFiltered?: any[]) {
        this.prepareSync(structureId, start_date, end_date, studentsFiltered, audiencesFiltered);
        let url = `/presences/exemptions/export` + this.prepareUrl();
        window.open(url);
    }
}
