import {Group} from "@common/services";

export interface GroupingResponse {
    id: string,
    name: string,
    student_divisions: Array<StudentDivisionResponse>
}

export interface StudentDivisionResponse {
    id: string,
    name : string
}

export class Grouping {
    id: string;
    name: string;
    groupList: Array<Group>;

    buildFromResponse(groupingResponse: GroupingResponse): Grouping {
        this.name = groupingResponse.name;
        this.groupList = groupingResponse.student_divisions
            .map((student_divisions: StudentDivisionResponse) => new Group().buildFromStudentDivision(student_divisions));
        this.id = groupingResponse.id;
        return this;
    }
}

export function instanceOfGrouping(object: any): object is Grouping {
    return 'name' in object && 'groupList' in object;
}
