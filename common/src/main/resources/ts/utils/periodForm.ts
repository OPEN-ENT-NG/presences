import {moment} from 'entcore';
import {ITimeSlot, TimeSlotHourPeriod} from "../model/Viescolaire";
import {DateUtils} from "./date";

export class PeriodFormUtils {

    // form: object => concern the form that contains the two attributes "formStartAtt" and "formEndAtt" that will
    // have to be changed to keep period (and the form) valid.
    static setHourSelectorsFromTimeSlots(date: string,
                                         hourPeriod: TimeSlotHourPeriod,
                                         timeSlotTimePeriod: { start: ITimeSlot, end: ITimeSlot },
                                         form: Object, formStartAtt: string, formEndAtt: string): void {
        switch (hourPeriod) {
            case TimeSlotHourPeriod.START_HOUR:
                if (timeSlotTimePeriod.start != null) {
                    form[formStartAtt] = DateUtils.getDateFormat(new Date(date),
                        DateUtils.getTimeFormatDate(timeSlotTimePeriod.start.startHour))

                    if (!form[formEndAtt] || !timeSlotTimePeriod.end
                        || !DateUtils.isPeriodValid(form[formStartAtt], form[formEndAtt])) {
                        timeSlotTimePeriod.end = {...timeSlotTimePeriod.start};
                        form[formEndAtt] = DateUtils.getDateFormat(new Date(date),
                            DateUtils.getTimeFormatDate(timeSlotTimePeriod.start.endHour))
                    }
                } else form[formStartAtt] = null;
                break;
            case TimeSlotHourPeriod.END_HOUR:
                if (timeSlotTimePeriod.end != null) {
                    form[formEndAtt] = DateUtils.getDateFormat(new Date(date),
                        DateUtils.getTimeFormatDate(timeSlotTimePeriod.end.endHour))

                    if (!form[formStartAtt] || !timeSlotTimePeriod.start ||
                        !DateUtils.isPeriodValid(form[formStartAtt], form[formEndAtt])) {
                        timeSlotTimePeriod.start = {...timeSlotTimePeriod.end};
                        form[formStartAtt] = DateUtils.getDateFormat(new Date(date),
                            DateUtils.getTimeFormatDate(timeSlotTimePeriod.end.startHour));
                    }
                } else form[formEndAtt] = null;
                break;
            default:
                return;
        }
    }

    // formFreeDates: object => concern the form that contains the attributes
    // "freeStartTimeAtt" and "freeEndTimeAtt", times attributes that are linked to "startDateAttr" and "endDateAttr"
    // dates attributes that will have to be changed to keep period (and the form)
    // valid when the form mod is "isFreeSchedule".
    static setHourSelectorsFromTimeSlotsOrFree(
        hourPeriod: TimeSlotHourPeriod, isFreeSchedule: boolean, formFreeDates: Object,
        freeStartTimeAtt: string, freeEndTimeAtt: string, startDateAttr: string, endDateAttr: string,
        timeSlotTimePeriod: { start: ITimeSlot; end: ITimeSlot; },
        form: Object, formStartDateAttr: string, formEndDateAttr: string,
    ): void {
        let startHour: Date = null;
        let endHour: Date = null;

        if (isFreeSchedule && formFreeDates) {
            startHour = formFreeDates[freeStartTimeAtt];
            endHour = formFreeDates[freeEndTimeAtt];
        } else if (!isFreeSchedule && timeSlotTimePeriod) {
            if (timeSlotTimePeriod.start && timeSlotTimePeriod.start.startHour)
                startHour = DateUtils.getTimeFormatDate(timeSlotTimePeriod.start.startHour);
            if (timeSlotTimePeriod.end && timeSlotTimePeriod.end.endHour)
                endHour = DateUtils.getTimeFormatDate(timeSlotTimePeriod.end.endHour);
        }

        let start: string = formFreeDates[startDateAttr] && startHour ? DateUtils.getDateFormat(formFreeDates[startDateAttr], startHour) : null;
        let end: string = formFreeDates[endDateAttr] && endHour ? DateUtils.getDateFormat(formFreeDates[endDateAttr], endHour) : null;

        switch (hourPeriod) {
            case TimeSlotHourPeriod.START_HOUR:
                if ((formFreeDates[startDateAttr] && !formFreeDates[endDateAttr]) ||
                    (start && end && !DateUtils.isPeriodValid(start, end)) ||
                    (!(start && end) && formFreeDates[startDateAttr] && formFreeDates[endDateAttr]
                        && !DateUtils.isPeriodValid(formFreeDates[startDateAttr], formFreeDates[endDateAttr]))) {
                    if(startHour) {
                        if (isFreeSchedule) formFreeDates[freeEndTimeAtt] = moment(formFreeDates[freeStartTimeAtt]).add(1, 'hours').toDate()
                        else {
                            timeSlotTimePeriod.end = {...timeSlotTimePeriod.start};
                            endHour = DateUtils.getTimeFormatDate(timeSlotTimePeriod.end.endHour);
                        }
                    }
                    if(formFreeDates[startDateAttr]) {
                        formFreeDates[endDateAttr] = new Date(formFreeDates[startDateAttr]);
                        if(endHour) form[formEndDateAttr] = DateUtils.getDateFormat(formFreeDates[endDateAttr], endHour);
                    }
                }
                break;
            case TimeSlotHourPeriod.END_HOUR:
                if ((formFreeDates[endDateAttr] && !formFreeDates[startDateAttr]) ||
                    (start && end && !DateUtils.isPeriodValid(start, end)) ||
                    (!(start && end) && formFreeDates[startDateAttr] && formFreeDates[endDateAttr]
                        && !DateUtils.isPeriodValid(formFreeDates[startDateAttr], formFreeDates[endDateAttr]))) {
                    if (endHour != null) {
                        if (isFreeSchedule) formFreeDates[freeStartTimeAtt] = moment(formFreeDates[freeEndTimeAtt]).add(-1, 'hours').toDate()
                        else {
                            timeSlotTimePeriod.start = {...timeSlotTimePeriod.end};
                            endHour = DateUtils.getTimeFormatDate(timeSlotTimePeriod.end.endHour);
                        }
                    }
                    if(formFreeDates[endDateAttr]) {
                        formFreeDates[startDateAttr] = new Date(formFreeDates[endDateAttr]);
                        if(startHour) form[formStartDateAttr] = DateUtils.getDateFormat(formFreeDates[startDateAttr], startHour)
                    }
                }
                break;
            default:
                return;
        }
    }
}