import {USER_TYPES} from "@common/core/constants/UserType";

export class UserUtils {


    /**
     * Check if our model.me.type is the one that match with type (Case is Relative)
     * @param {String} type
     * @return boolean
     */
    static isRelative(type: string) {
        return type === USER_TYPES.relative;
    }

    /**
     * Check if our model.me.type is the one that match with type (Case is Child)
     * @param {String} type
     * @return boolean
     */
    static isChild(type: string): boolean {
        return type === USER_TYPES.student;
    }

    /**
     * Check if our model.me.type is the one that match with type (Case is Teacher)
     * @param {String} type
     * @return boolean
     */
    static isTeacher(type: string): boolean {
        return type === USER_TYPES.teacher;
    }
}