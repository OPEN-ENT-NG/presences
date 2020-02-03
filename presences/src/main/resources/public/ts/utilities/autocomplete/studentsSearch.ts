import {User} from "@common/model/User";
import {SearchService} from "@common/services/SearchService";
import {AutoCompleteUtils} from "../autocomplete";

export class StudentsSearch extends AutoCompleteUtils {

    private students: Array<User>;
    private selectedStudents: Array<{}>;

    public student: string;

    constructor(structureId: string, searchService: SearchService) {
        super(structureId, searchService);
        this.students = [];
        this.selectedStudents = [];
    }

    public getStudents() {
        return this.students;
    }

    public getSelectedStudents() {
        return this.selectedStudents;
    }

    public removeSelectedStudents(value) {
        this.selectedStudents.splice(this.selectedStudents.indexOf(value), 1);
    }

    public resetStudents() {
        this.students = [];
    }

    public resetSelectedStudents() {
        this.selectedStudents = [];
    }

    public selectStudents(model, item) {
        this.selectedStudents.push(item);
    };

    public selectStudent(model, item) {
        this.selectedStudents = [];
        this.selectedStudents.push(item);
    }

    public async searchStudents(value) {
        try {
            this.students = await this.searchService.searchUser(this.structureId, value, 'Student');
        } catch (err) {
            this.students = [];
            throw err;
        }
    };

    public searchStudentsFromArray(value, studentsArray) {
        this.students = [];
        try {
            studentsArray.forEach(student => {
                let user: User = {} as User;
                user.id = student.id;
                user.displayName = student.name;
                user.toString = () => student.name;
                this.students.push(user);
            });
            this.students = this.students.filter(
                student =>
                    student.displayName.toUpperCase().indexOf(value) > -1 ||
                    student.displayName.toLowerCase().indexOf(value) > -1
            );
        } catch (err) {
            this.students = [];
            throw err;
        }
    }
}