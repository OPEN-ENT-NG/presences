import {GroupingResponse} from "@common/model/Grouping";
import {ng} from "entcore";

export interface GroupingService {
    search(structureId: string, value: string): Promise<GroupingResponse[]>;
}

export const GroupingService: GroupingService = {
    search: async (structureId: string, value: string) => {
        return [
            {
                id: "id1",
                name: "name1",
                student_divisions: [
                    {
                        student_divison_id: "student_divison_id1",
                        student_divison_name: "student_divison_name1"
                    },
                    {
                        student_divison_id: "student_divison_id2",
                        student_divison_name: "student_divison_name2"
                    }
                ]
            },
            {
                id: "id2",
                name: "name2",
                student_divisions: [
                    {
                        student_divison_id: "student_divison_id3",
                        student_divison_name: "student_divison_name3"
                    },
                    {
                        student_divison_id: "student_divison_id4",
                        student_divison_name: "student_divison_name4"
                    }
                ]
            }
        ];
        // try {
        //     value = value.replace("\\s", "").toLowerCase();
        //     const {data} = await http.get(`/presences/search/groups?structureId=${structureId}&q=${value}&field=name`);
        //     return data;
        // } catch (err) {
        //     throw err;
        // }
    }
};

export const groupingService = ng.service('GroupingService', (): GroupingService => GroupingService);