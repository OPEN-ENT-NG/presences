import {SearchService} from "@common/services/SearchService";

export class AutoCompleteUtils {

    protected readonly structureId: string;

    protected searchService: SearchService;

    constructor(structureId: string, searchService: SearchService) {
        this.structureId = structureId;
        this.searchService = searchService;
    }

}
