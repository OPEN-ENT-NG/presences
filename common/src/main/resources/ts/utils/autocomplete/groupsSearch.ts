import {SearchService} from "@common/services/SearchService";
import {AutoCompleteUtils} from "./auto-complete";
import {Group, GroupService} from "@common/services";

/**
 * ⚠ This class is used for the directive async-autocomplete
 * use it for group only (groups/classes)
 */

export class GroupsSearch extends AutoCompleteUtils {

    private groups: Array<Group>;
    private selectedGroups: Array<{}>;

    private groupService: GroupService;

    public group: string;

    constructor(structureId: string, searchService: SearchService, groupService: GroupService) {
        super(structureId, searchService);
        this.groupService = groupService;
        this.groups = [];
        this.selectedGroups = [];
    }

    public getGroups() {
        return this.groups;
    }

    public getSelectedGroups() {
        return this.selectedGroups;
    }

    public setSelectedGroups(selectedGroups: Array<{}>) {
        this.selectedGroups = selectedGroups;
    }

    public removeSelectedGroups(groupItem) {
        this.selectedGroups.splice(this.selectedGroups.indexOf(groupItem), 1);
    }

    public resetGroups() {
        this.groups = [];
    }

    public resetSelectedGroups() {
        this.selectedGroups = [];
    }

    public selectGroups(valueInput, groupItem) {
        if (this.selectedGroups.find(group => group["id"] === groupItem.id) === undefined) {
            this.selectedGroups.push(groupItem);
        }
    };

    public selectGroup(valueInput, groupItem) {
        this.selectedGroups = [];
        this.selectedGroups.push(groupItem);
    }

    public async searchGroups(valueInput: string) {
        try {
            this.groups = await this.groupService.search(this.structureId, valueInput);
            this.groups.map((group: Group) => group.toString = () => group.name);
        } catch (err) {
            this.groups = [];
            throw err;
        }
    };
}