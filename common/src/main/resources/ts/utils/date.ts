import {moment} from 'entcore';
import {DurationInputArg1, DurationInputArg2} from 'moment';
import {ITimeSlot, TimeSlotHourPeriod} from "@common/model";

export class DateUtils {
    static FORMAT = {
        'YEAR-MONTH-DAY-HOUR-MIN-SEC': 'YYYY-MM-DD HH:mm:ss',
        'YEAR/MONTH/DAY-HOUR-MIN-SEC': 'YYYY/MM/DD HH:mm:ss',
        'YEAR/MONTH/DAY-HOUR-MIN': 'YYYY/MM/DD HH:mm',
        'YEAR-MONTH-DAY-T-HOUR-MIN-SEC': 'YYYY-MM-DDTHH:mm:ss',
        'YEAR-MONTH-DAY': 'YYYY-MM-DD',
        'YEARMONTHDAY': 'YYYYMMDD',
        'YEAR-MONTH': 'YYYY-MM',
        'DAY-MONTH-YEAR': 'DD/MM/YYYY',
        'DAY-MONTH': 'DD/MM', // e.g "04/11"
        'HOUR-MINUTES': 'kk:mm', // e.g "09:00"
        'BIRTHDATE': 'L',
        'SHORT-MONTH': 'MMM', // e.g "Jan"
        'MONTH': 'MMMM', // e.g "January"
        'DAY-MONTH-YEAR-LETTER': 'LL',  // e.g "9 juin 2019"
        'DAY-DATE': 'dddd L',
        'DATE-FULL-LETTER': 'dddd LL',
        'DAY-MONTH-HALFYEAR': 'DD/MM/YY',
        'DAY-MONTH-HALFYEAR-HOUR-MIN': 'DD/MM/YY HH:mm',
        'HOUR-MIN-SEC': 'HH:mm:ss',
        'HOUR-MIN': 'HH:mm',
        'MINUTES': 'mm'
    };

    static START_DAY_TIME = "00:00:00";
    static END_DAY_TIME = "23:59:59";

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
     * @param date      Date format
     * @param dateTime  Time format
     */
    static getDateFormat(date: Date, dateTime: Date): string {
        return moment(moment(date)
            .format('YYYY-MM-DD') + ' ' + moment(dateTime)
            .format('HH:mm'), 'YYYY-MM-DD HH:mm')
            .format('YYYY-MM-DD HH:mm:ss');
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

    static isValid(date: any, format: string): boolean {
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
    static isMatchDate(startDateValue: string, endDateValue: string,
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

        return startDate === comparedStartDate && endDate === comparedEndDate;
    }

    /**
     * Check if your start and end date is between constant date
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

    /**
     * ⚠ This method format your TIME but your DATE will have your date.now() ⚠
     * @param time  time value as a string (e.g) "09:00"
     */
    static getTimeFormatDate(time: string): Date {
        return moment().set('HOUR', time.split(":")[0]).set('MINUTE', time.split(":")[1]).toDate();
    }


    /**
     * ⚠ MUST use "Sort" method. Array based of startTime & endTime ⚠
     * example : Array.sort(compareTime)
     *
     * E.G timeArray: [
     *      {"id": 1, "start": "18:00","end": "20:00"},
     *      {"id": 2, "start": "19:00","end": "21:00"},
     *      {"id": 3, "start": "17:00","end": "19:00"}
     *   ]
     *
     * timeArray.sort(compareTime('start', 'end'));
     *
     * Will sort in order [3..., 1..., 2...] then :
     *   timeArray: [
     *      {"id": 3, "start": "17:00","end": "19:00"},
     *      {"id": 1, "start": "18:00","end": "20:00"},
     *      {"id": 2, "start": "19:00","end": "21:00"}
     *   ]
     *
     * @param startTimeKey  key startDate (could be startDate or start_date)
     * @param endTimeKey    key endDate (could be endDate or end_date)
     * param 'a' and 'b' are two elements to compare on function sort
     */
    static compareTime(startTimeKey: string, endTimeKey: string) {
        let startTime = startTimeKey;
        let endTime = endTimeKey;
        return function (a, b) {
            if (a[endTime] < b[endTime] || (a[endTime] == b[endTime] && a[startTime] > b[startTime]))
                return -1;
            if (a[endTime] > b[endTime] || (a[endTime] == b[endTime] && a[startTime] < b[startTime]))
                return 1;
            return 0;
        }
    }

    static getDayNumberDifference(dateA: String | Date, dateB: String | Date): number {
        return moment(dateA).diff(moment(dateB), 'days');
    }

    static getDateFromMoment(date) {
        return new Date(date.year(), date.month(), date.date(), date.hour(), date.minutes(), 0)
    }

    /**
     * Get current date in specified format
     */
    static getCurrentDate(format: string): string {
        return moment(new Date()).format(format);
    }

    static isBetweenTimeStamp(startDateValue: any, endDateValue: any, startDateToCompare: any, endDateToCompare: any) {
        let aStartTimestamp = this.getDateFromMoment(startDateValue).getTime();
        let aEndTimestamp = this.getDateFromMoment(endDateValue).getTime();

        let bStartTimestamp = this.getDateFromMoment(startDateToCompare).getTime();
        let bEndTimestamp = this.getDateFromMoment(endDateToCompare).getTime();

        return (aStartTimestamp < bEndTimestamp && aEndTimestamp > bStartTimestamp)
            || (bStartTimestamp < aEndTimestamp && bEndTimestamp > aStartTimestamp);
    }

    static isPeriodValid(startAt: String, endAt: String): boolean {
        return this.isValid(startAt, this.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])
            && this.isValid(endAt, this.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])
            && moment(startAt).isBefore(moment(endAt))
    }
}