import {SearchService} from "@common/services/SearchService";
import {AutoCompleteUtils} from "./auto-complete";
import {Group, GroupingService, GroupService} from "@common/services";
import {Grouping, instanceOfGrouping, toModelList} from "@common/model/grouping";

/**
 * ⚠ This class is used for the directive async-autocomplete
 * use it for group only (groups/classes)
 */

export class GroupsSearch extends AutoCompleteUtils {

    private _groups: Array<Group | Grouping>;
    private _selectedGroups: Array<{}>;

    private readonly _groupService: GroupService;
    private _groupingService: GroupingService;

    private _group: string;

    constructor(structureId: string, searchService: SearchService, groupService: GroupService, groupingService?: GroupingService) {
        super(structureId, searchService);
        this._groupService = groupService;
        this._groupingService = groupingService;
        this._groups = [];
    }

    //Only returns group from grouping, but not the grouping itself
    public getGroups(): Array<Group> {
        let groupsResult: Array<Group> = []
        if (!!this._groups) {
            this._groups.forEach((groupItem: Group | Grouping) => {
                if (instanceOfGrouping(groupItem)) {
                    groupsResult = (<Grouping>groupItem).groupList.concat(groupsResult);
                } else {
                    groupsResult.push(groupItem);
                }
            })
        }
        return groupsResult;
    }

    public getSelectedGroups() {
        return this._selectedGroups ? this._selectedGroups : [];
    }

    public setSelectedGroups(selectedGroups: Array<{}>) {
        this._selectedGroups = selectedGroups;
    }

    public removeSelectedGroups(groupItem) {
        this._selectedGroups.splice(this._selectedGroups.indexOf(groupItem), 1);
    }

    public resetGroups() {
        this._groups = [];
    }

    public resetSelectedGroups() {
        this._selectedGroups = [];
    }

    public selectGroups(valueInput: string, groupItem: Group | Grouping) {
        if (!this._selectedGroups) this._selectedGroups = [];
        this._selectedGroups = !(groupItem instanceof Group) ? [...this._selectedGroups, ...groupItem.groupList] :
            [...this._selectedGroups, ...[groupItem]];
        this._selectedGroups = Array.from(new Set(this._selectedGroups.map((selectedGroups: Group) => selectedGroups.id)))
            .map((selectedGroupId: string) => this._selectedGroups.find((group: Group) => group.id === selectedGroupId))
            .filter((selectedGroup: string) => !!selectedGroup);
    };

    public async searchGroups(valueInput: string) {
        try {
            this._groups = toModelList(await this._groupService.search(this.structureId, valueInput))
                .filter((group: Group) =>
                    !this.selectedGroups ||
                    !this.selectedGroups.find((selectGroup: Group) => selectGroup.id === group.id)
                ).map((group: Group) => {
                    group.toString = () => group.name;
                    return group
                });
            if (this._groupingService) {
                let groupingList: Array<Grouping> = await this._groupingService.search(this.structureId, valueInput)
                groupingList.forEach((grouping: Grouping) => {
                    grouping.toString = () => grouping.name;
                    this._groups.push(grouping)
                });
            }
        } catch (err) {
            this._groups = [];
            throw err;
        }
    };

    get groups(): Array<Group | Grouping> {
        return this._groups;
    }

    set groups(value: Array<Group | Grouping>) {
        this._groups = value;
    }

    get selectedGroups(): Array<{}> {
        return this._selectedGroups;
    }

    set selectedGroups(value: Array<{}>) {
        this._selectedGroups = value;
    }

    get groupingService(): GroupingService {
        return this._groupingService;
    }

    set groupingService(value: GroupingService) {
        this._groupingService = value;
    }

    get group(): string {
        return this._group;
    }

    set group(value: string) {
        this._group = value;
    }

    get groupService(): GroupService {
        return this._groupService;
    }

    clone(): GroupsSearch {
        let groupsSearch: GroupsSearch = new GroupsSearch(this.structureId, this.searchService, this.groupService, this.groupingService);
        groupsSearch.group = this.group;
        groupsSearch.groups = this.groups;
        groupsSearch.selectedGroups = this.selectedGroups;
        return groupsSearch;
    }
}