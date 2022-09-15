import {User} from "@common/model/User";
import {SearchService} from "@common/services/SearchService";
import {AutoCompleteUtils} from "./auto-complete";
import {Student} from "@common/model/Student";

/**
 * ⚠ This class is used for the directive async-autocomplete
 * use it only for students's info purpose
 */

export class StudentsSearch extends AutoCompleteUtils {

    private _students: Array<User>;
    private _selectedStudents: Array<{}>;

    private _student: string;

    constructor(structureId: string, searchService: SearchService) {
        super(structureId, searchService);
    }

    public getStudents() {
        return this._students;
    }

    public getSelectedStudents() {
        return this._selectedStudents ? this._selectedStudents : [];
    }

    public setSelectedStudents(selectedStudents: Array<{}>) {
        this._selectedStudents = selectedStudents;
    }

    public removeSelectedStudents(studentItem) {
        this._selectedStudents.splice(this._selectedStudents.indexOf(studentItem), 1);
    }

    public resetStudents() {
        this._students = [];
    }

    public resetSelectedStudents() {
        this._selectedStudents = [];
    }

    public selectStudents(valueInput: string, studentItem: Student): void {
        if (!this._selectedStudents) this._selectedStudents = [];
        if (this._selectedStudents.find(student => student["id"] === studentItem.id) === undefined) {
            this._selectedStudents.push(studentItem);
        }
    }

    public selectStudent(valueInput, studentItem) {
        this._selectedStudents = [];
        this._selectedStudents.push(studentItem);
    }

    public async searchStudents(valueInput: string) {
        try {
            this._students = await this.searchService.searchStudents(this.structureId, valueInput);
        } catch (err) {
            this._students = [];
            throw err;
        }
    };

    public searchStudentsFromArray(valueInput: string, studentsArray) {
        try {
            studentsArray.forEach(student => {
                let user: User = {} as User;
                user.id = student.id;
                user.displayName = student.name;
                user.toString = () => student.name;
                this._students.push(user);
            });
            this._students = this._students.filter(
                student =>
                    student.displayName.toUpperCase().indexOf(valueInput) > -1 ||
                    student.displayName.toLowerCase().indexOf(valueInput) > -1
            );
        } catch (err) {
            this._students = [];
            throw err;
        }
    }

    get students(): Array<User> {
        return this._students;
    }

    set students(value: Array<User>) {
        this._students = value;
    }

    get selectedStudents(): Array<{}> {
        return this._selectedStudents;
    }

    set selectedStudents(value: Array<{}>) {
        this._selectedStudents = value;
    }

    get student(): string {
        return this._student;
    }

    set student(value: string) {
        this._student = value;
    }

    clone(): StudentsSearch {
        let studentsSearch: StudentsSearch = new StudentsSearch(this.structureId, this.searchService);
        studentsSearch.students = this.students;
        studentsSearch.selectedStudents = this.selectedStudents;
        studentsSearch.student = this.student;
        return studentsSearch
    }
}