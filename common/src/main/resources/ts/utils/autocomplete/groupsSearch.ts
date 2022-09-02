import {SearchService} from "@common/services/SearchService";
import {AutoCompleteUtils} from "./auto-complete";
import {Group, GroupService, GroupingService} from "@common/services";
import {Grouping, GroupingResponse, instanceOfGrouping, StudentDivision} from "@common/model/Grouping";

/**
 * ⚠ This class is used for the directive async-autocomplete
 * use it for group only (groups/classes)
 */

export class GroupsSearch extends AutoCompleteUtils {

    private groups: Array<Group | Grouping>;
    private selectedGroups: Array<{}>;

    private groupService: GroupService;
    private groupingService: GroupingService;

    public group: string;

    constructor(structureId: string, searchService: SearchService, groupService: GroupService, groupingService?: GroupingService) {
        super(structureId, searchService);
        this.groupService = groupService;
        this.groupingService = groupingService;
    }

    public getGroups() {
        return this.groups;
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

    public selectGroups(valueInput, groupItem: Group | Grouping) {
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

    public selectGroup(valueInput, groupItem: Group | Grouping) {
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
            if (this.groupingService) {
                let listGroupings: Array<GroupingResponse> = await this.groupingService.search(this.structureId, valueInput)
                listGroupings.map((groupingResponse: GroupingResponse) => {
                    let listGroup: Array<Group> = groupingResponse.student_divisions.map((student_divisions: StudentDivision) => {
                        let group: Group = {id: student_divisions.student_divison_id, name: student_divisions.student_divison_name} as Group
                        group.toString = () => group.name;
                        return group;
                    });
                    return {groupList: listGroup, name: groupingResponse.name} as Grouping
                }).forEach((grouping: Grouping) => {
                    grouping.toString = () => grouping.name;
                    this.groups.push(grouping)
                })
            }
        } catch (err) {
            this.groups = [];
            throw err;
        }
    };
}