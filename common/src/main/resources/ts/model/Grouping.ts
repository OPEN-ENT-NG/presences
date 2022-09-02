import {Group} from "@common/services";

export interface GroupingResponse {
    id: string,
    name: string,
    student_divisions: Array<StudentDivision>
}

export interface StudentDivision {
    student_divison_id: string,
    student_divison_name : string
}

export interface Grouping {
    name: string
    groupList: Array<Group>
}

export function instanceOfGrouping(object: any): object is Grouping {
    return 'name' in object && 'groupList' in object;
}
