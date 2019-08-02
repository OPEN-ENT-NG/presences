import {moment} from 'entcore';
import {DurationInputArg1, DurationInputArg2} from 'moment';

export class DateUtils {
    static FORMAT = {
        'YEAR-MONTH-DAY-HOUR-MIN-SEC': 'YYYY-MM-DD HH:mm:ss',
        'YEAR-MONTH-DAY': 'YYYY-MM-DD',
        'DAY-MONTH-YEAR': 'DD/MM/YYYY',
        'HOUR-MINUTES': 'kk:mm',
        'BIRTHDATE': 'L',
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
}