import {SearchService} from "@common/services/SearchService";
import {AutoCompleteUtils} from "./auto-complete";
import {Group, GroupingService, GroupService} from "@common/services";
import {Grouping, instanceOfGrouping} from "@common/model/grouping";

/**
 * ⚠ This class is used for the directive async-autocomplete
 * use it for group only (groups/classes)
 */

export class GroupsSearch extends AutoCompleteUtils {

    private groups: Array<Group | Grouping>;
    private selectedGroups: Array<{}>;

    private groupService: GroupService;
    private _groupingService: GroupingService;

    public group: string;

    constructor(structureId: string, searchService: SearchService, groupService: GroupService, groupingService?: GroupingService) {
        super(structureId, searchService);
        this.groupService = groupService;
        this._groupingService = groupingService;
        this.groups = [];
    }

    //Only returns group from grouping, but not the grouping itself
    public getGroups(): Array<Group> {
        let groupsResult: Array<Group> = []
        this.groups.forEach((groupItem: Group | Grouping) => {
            if (instanceOfGrouping(groupItem)) {
                groupsResult = (<Grouping>groupItem).groupList.concat(groupsResult);
            } else {
                groupsResult.push(groupItem);
            }
        })
        return groupsResult;
    }

    public getSelectedGroups() {
        return this.selectedGroups ? this.selectedGroups : [];
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

    public selectGroups(valueInput: string, groupItem: Group | Grouping) {
        if (!this.selectedGroups) this.selectedGroups = [];
        if (instanceOfGrouping(groupItem)) {
            let that: GroupsSearch = this;
            (<Grouping>groupItem).groupList.forEach((group: Group) => {
                that.selectGroups(valueInput, group);
            });
        } else {
            if (this.selectedGroups.find(group => group["id"] === groupItem.id) === undefined) {
                this.selectedGroups.push(groupItem);
            }
        }
    };

    public selectGroup(valueInput: string, groupItem: Group | Grouping) {
        this.selectedGroups = [];
        if (instanceOfGrouping(groupItem)) {
            let that: GroupsSearch = this;
            (<Grouping>groupItem).groupList.forEach((group: Group) => {
                that.selectGroups(valueInput, group);
            });
        } else {
            this.selectedGroups.push(groupItem);
        }
    }

    public async searchGroups(valueInput: string) {
        try {
            this.groups = await this.groupService.search(this.structureId, valueInput);
            this.groups.forEach((group: Group) => group.toString = () => group.name);
            if (this._groupingService) {
                let groupingList: Array<Grouping> = await this._groupingService.search(this.structureId, valueInput)
                groupingList.forEach((grouping: Grouping) => {
                    grouping.toString = () => grouping.name;
                    this.groups.push(grouping)
                });
            }
        } catch (err) {
            this.groups = [];
            throw err;
        }
    };

    set groupingService(value: GroupingService) {
        this._groupingService = value;
    }

    clone(): GroupsSearch {
        let groupsSearch: GroupsSearch = new GroupsSearch(this.structureId, this.searchService, this.groupService, this._groupingService);
        groupsSearch.group = this.group;
        groupsSearch.groups = this.groups;
        groupsSearch.selectedGroups = this.selectedGroups;
        return groupsSearch;
    }
}