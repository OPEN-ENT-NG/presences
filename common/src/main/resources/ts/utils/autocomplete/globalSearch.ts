import {SearchItem, SearchService} from "@common/services/SearchService";
import {GroupService} from "@common/services";
import {AutoCompleteUtils} from "./auto-complete";

/**
 * ⚠ This class is used for the directive async-autocomplete
 * use it only for every item
 */

export class GlobalSearch extends AutoCompleteUtils {

    public static readonly TYPE = {
        user: 'USER',
        group: 'GROUP'
    };

    private searchItems: Array<SearchItem>;
    private students: Array<SearchItem>;

    private selectedItems: Array<{}>;

    public search: string;

    private groupService: GroupService;

    constructor(structureId: string, searchService: SearchService, groupService: GroupService) {
        super(structureId, searchService);
        this.groupService = groupService;
        this.searchItems = [];
        this.selectedItems = [];
    }

    public getItems() {
        return this.searchItems;
    }

    public getStudents() {
        return this.students;
    }

    public getSelectedItems() {
        return this.selectedItems;
    }

    public removeSelectedItems(item) {
        this.selectedItems.splice(this.selectedItems.indexOf(item), 1);
    }

    public resetItems() {
        this.searchItems = [];
    }


    public resetStudents() {
        this.students = [];
    }

    public resetSelectedStudents() {
        this.selectedItems = [];
    }

    public selectItems(valueInput, item) {
        if (this.selectedItems.find(i => i["id"] === item.id) === undefined) {
            this.selectedItems.push(item);
        }
    };

    public async searchStudentsOrGroups(valueInput: string) {
        try {
            this.searchItems = await this.searchService.search(this.structureId, valueInput);
        } catch (err) {
            this.searchItems = [];
            throw err;
        }
    };

    public async getStudentsFromGroup(id: string, type: string) {
        try {
            this.students = await this.groupService.getStudentsFromGroupId(id, type);
        } catch (err) {
            this.students = [];
            throw err;
        }
    }
}