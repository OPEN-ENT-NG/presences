import {Grouping, GroupingResponse} from "@common/model/grouping";
import {ng} from "entcore";
import http from "axios";

export interface GroupingService {
    search(structureId: string, value: string): Promise<Grouping[]>;
}

export const GroupingService: GroupingService = {
    search: async (structureId: string, value: string) => {
        try {
            value = value.replace("\\s", "").toLowerCase();
            const {data} = await http.get(`/presences/grouping/structure/${structureId}/list?searchValue=${value}`);
            return data.map((groupingResponse: GroupingResponse) => new Grouping().buildFromResponse(groupingResponse));
        } catch (err) {
            throw err;
        }
    }
};

export const groupingService = ng.service('GroupingService', (): GroupingService => GroupingService);