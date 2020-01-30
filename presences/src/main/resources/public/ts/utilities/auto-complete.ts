import {SearchService} from "@common/services/SearchService";
import {User} from "@common/model/User";

export class AutoCompleteUtils {

    private readonly structureId: string;
    private students: User[];
    private selectedStudents: Array<{}>;
    private searchService: SearchService;

    public static student: string;

    constructor(structureId: string, searchService: SearchService) {
        this.structureId = structureId;
        this.searchService = searchService;
        this.selectedStudents = [];
        this.resetSearchFields();
    }

    public resetSearchFields() {
        AutoCompleteUtils.student = null;
        this.students = [];
    }

    public getStudents() {
        return this.students;
    }

    public getSelectedStudents() {
        return this.selectedStudents;
    }

    public setSelectedStudents(value) {
        this.selectedStudents = [...value];
    }

    public removeSelectedStudent(value) {
        this.selectedStudents.splice(this.selectedStudents.indexOf(value), 1);
    }

    public async searchStudents(value) {
        try {
            this.students = await this.searchService.searchUser(this.structureId, value, 'Student');
        } catch (err) {
            this.students = [];
            throw err;
        }
    };

    public selectStudent(model, item) {
        this.selectedStudents.push(item);
    };

}
