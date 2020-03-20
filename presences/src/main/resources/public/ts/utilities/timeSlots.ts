import {ITimeSlot} from "@common/model";

export class TimeSlotsUtils {

    /**
     * get slots index as a number array
     *
     * (e.g) :
     * [{M1 08:00 - 09:00}
     * {M2 08:00 - 09:00}
     * {M3 08:00 - 09:00}] will get be [0, 1, 2]
     *
     * @param {Array<ITimeSlot>} slots your timeSlots from the current structure
     */
    private static getSlotsIndexAsArray(slots: Array<ITimeSlot>): number[] {
        let slotsIndexArray = [];
        for (let i = 0; i < slots.length; i++) {
            slotsIndexArray.push(i);
        }
        return slotsIndexArray;
    }

    /**
     * check if number is odd
     * isOdd(1) = 1
     * isOdd(2) = 0
     * isOdd(3) = 1
     * isOdd(4) = 0
     *
     * @param {number} num the number
     */
    private static isOdd(num: number): number {
        return num % 2;
    }

    /**
     * check if your current slot belongs to the afternoon slot (second part of TimeSlots)
     *
     * e.g [0, 1, (2)] "2" could possibly belong to the afternoon slot
     *
     * @param {number} slotIndex
     * @param {Array<ITimeSlot>} slots
     */
    static isSlotSecondPart(slotIndex: number, slots: Array<ITimeSlot>): boolean {
        if (this.isOdd(slots.length) === 1) {
            let endSlotsOfDay = this.getEndSlotsOfDay(slots);
            return endSlotsOfDay.some(item => item === slotIndex);
        }
        return false;
    }

    /**
     * get the "morning" slots first part
     *
     * e.g [0, 1, 2, 3, 4, 5, 6, 7, 8] will return [0, 1, 2, 3]
     * note: 4 is not taken in account since it is the "middle"
     * splitting morning and afternoon then it is supposed to be the "break/lunch" time
     *
     * @param {Array<ITimeSlot>} slots
     */
    static getStartSlotsOfDay(slots: Array<ITimeSlot>): number[] {
        let slotsIndexArray = this.getSlotsIndexAsArray(slots);
        return slotsIndexArray.slice().splice(0, slotsIndexArray.length / 2);
    }

    /**
     * get the "afternoon" slots second part
     *
     * e.g [0, 1, 2, 3, 4, 5, 6, 7, 8] will return [5, 6, 7, 8]
     * note: 4 is not taken in account since it is the "middle"
     * splitting morning and afternoon then it is supposed to be the "break/lunch" time
     *
     * @param {Array<ITimeSlot>} slots
     */
    static getEndSlotsOfDay(slots: Array<ITimeSlot>): number[] {
        let slotsIndexArray = this.getSlotsIndexAsArray(slots);
        return slotsIndexArray.slice().splice((slotsIndexArray.length / 2) + 1, slotsIndexArray.length);
    }
}