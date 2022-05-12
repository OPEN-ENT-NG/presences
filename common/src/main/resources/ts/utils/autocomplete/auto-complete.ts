import {SearchService} from "@common/services/SearchService";

export class AutoCompleteUtils {

    public readonly structureId: string;

    protected searchService: SearchService;

    constructor(structureId: string, searchService: SearchService) {
        this.structureId = structureId;
        this.searchService = searchService;
    }

}
