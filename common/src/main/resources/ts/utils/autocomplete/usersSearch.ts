import {User} from "@common/model/User";
import {SearchService} from "@common/services/SearchService";
import {AutoCompleteUtils} from "./auto-complete";

/**
 * ⚠ This class is used for the directive async-autocomplete
 * use it only for "important" user such as teacher...personal...
 */

export class UsersSearch extends AutoCompleteUtils {

    public static readonly PROFILES = {
        personnel: 'Personnel',
        teacher: 'Teacher'
    };

    private users: Array<User>;
    private selectedUsers: Array<{}>;

    public user: string;

    constructor(structureId: string, searchService: SearchService) {
        super(structureId, searchService);
    }

    public getUsers() {
        return this.users;
    }

    public getSelectedUsers() {
        return this.selectedUsers ? this.selectedUsers : [];
    }

    public removeSelectedUsers(studentItem) {
        this.selectedUsers.splice(this.selectedUsers.indexOf(studentItem), 1);
    }

    public resetUsers() {
        this.users = [];
    }

    public resetSelectedUsers() {
        this.selectedUsers = [];
    }

    public selectUsers(valueInput, studentItem) {
        if (!this.selectedUsers) this.selectedUsers = [];
        if (this.selectedUsers.find(student => student["id"] === studentItem.id) === undefined) {
            this.selectedUsers.push(studentItem);
        }
    };

    public selectStudent(valueInput, studentItem) {
        this.selectedUsers = [];
        this.selectedUsers.push(studentItem);
    }

    public async searchUsers(valueInput: string, profiles?: Array<string>) {
        try {
            let promiseList: Array<Promise<User[]>> = []
            if (!profiles) {
                promiseList = [
                    this.searchService.searchUser(this.structureId, valueInput, UsersSearch.PROFILES.personnel),
                    this.searchService.searchUser(this.structureId, valueInput, UsersSearch.PROFILES.teacher)
                ]
            } else {
                profiles.forEach(profile => {
                    promiseList.push(this.searchService.searchUser(this.structureId, valueInput, profile))
                })
            }

            await Promise.all(promiseList).then(response => {
                this.users = [].concat(...response);
            })
        } catch (err) {
            this.users = [];
            throw err;
        }
    };
}