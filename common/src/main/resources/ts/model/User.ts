export interface User {
    displayName?: string;
    firstName?: string;
    lastName?: string;
    id?: string;
    idEleve?: string;
    groupId?: string;
    groupName?: string;
    info?: string;
}

// id, child.displayName as displayName, c.id as classId, c.name as className