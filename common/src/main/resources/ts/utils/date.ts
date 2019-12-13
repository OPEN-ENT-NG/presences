import {moment} from 'entcore';
import {DurationInputArg1, DurationInputArg2} from 'moment';

export class DateUtils {
    static FORMAT = {
        'YEAR-MONTH-DAY-HOUR-MIN-SEC': 'YYYY-MM-DD HH:mm:ss',
        'YEAR-MONTH-DAY': 'YYYY-MM-DD',
        'YEAR-MONTH': 'YYYY-MM',
        'DAY-MONTH-YEAR': 'DD/MM/YYYY',
        'DAY-MONTH': 'DD/MM', // e.g "04/11"
        'HOUR-MINUTES': 'kk:mm', // e.g "09:00"
        'BIRTHDATE': 'L',
        'DAY-MONTH-YEAR-LETTER': 'LL',  // e.g "9 juin 2019"
        'DAY-DATE': 'dddd L',
        'DATE-FULL-LETTER': 'dddd LL'
    };

    /**
     * Format date based on given format using moment
     * @param date date to format
     * @param format format
     */
    static format(date: any, format: string) {
        return moment(date).format(format);
    }

    /**
     * Get formatted date day based on given format
     * @param date date
     * @param day day number. 0 = Sunday, 6 = Saturday
     * @param format format
     */
    static getDayNumberDate(date: any, day: number, format: string) {
        return moment(date).day(day).format(format);
    }

    /**
     * Add step to given date.
     * @param date Date to update
     * @param step Step size
     * @param stepType Optional. Step type.
     */
    static add(date: any, step: DurationInputArg1, stepType: DurationInputArg2 = 'd'): Date {
        return moment(date).add(step, stepType).toDate();
    }

    /**
     * Set Date to first Time of day (fr format)
     * @param date Date to set
     */
    static setFirstTime(date: any): Date {
        return moment(date).set({hour: 0, minute: 0, second: 0}).toDate();
    }

    /**
     * Set Date to end Time of day (fr format)
     * @param date Date to set
     */
    static setLastTime(date: any): Date {
        return moment(date).set({hour: 23, minute: 59, second: 59}).toDate();
    }

    static isValid(date: any, format: string): Boolean {
        return (moment(date, format, true).isValid());
    }

    /**
     * Check if your start and end date is matching with constant date
     *
     * (e.g in register view when we compare our current date with the slots)
     *
     * @param startDateValue        StartDate value
     * @param endDateValue          EndDate value
     * @param startDateToCompare    StartDate to compare
     * @param endDateToCompare      EndDate to compare
     * @param dateValueFormat       date value format (optional)
     * @param dateToCompareFormat   date to compare format (optional)
     */
    static isBetween(startDateValue: string, endDateValue: string,
                     startDateToCompare: string, endDateToCompare: string,
                     dateValueFormat?: string, dateToCompareFormat?: string): boolean {

        let startDate = DateUtils
            .format(startDateValue, dateValueFormat ? dateValueFormat : DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
        let endDate = DateUtils
            .format(endDateValue, dateValueFormat ? dateValueFormat : DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
        let comparedStartDate = DateUtils
            .format(startDateToCompare, dateToCompareFormat ? dateToCompareFormat : DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
        let comparedEndDate = DateUtils
            .format(endDateToCompare, dateToCompareFormat ? dateToCompareFormat : DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);

        return ((comparedStartDate >= startDate) || (startDate < comparedEndDate))
            &&
            ((comparedEndDate <= endDate) || (endDate > comparedStartDate));
    }

    /**
     * ⚠ This method format your TIME but your DATE will have your date.now() ⚠
     * @param time  time value as a string (e.g) "09:00"
     */
    static getTimeFormat(time: string): string {
        return moment().set('HOUR', time.split(":")[0]).set('MINUTE', time.split(":")[1]);
    }
}