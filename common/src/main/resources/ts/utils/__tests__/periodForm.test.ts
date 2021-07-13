import {PeriodFormUtils} from "../periodForm";
import {ITimeSlot, TimeSlotHourPeriod} from "../../model/Viescolaire";

const date = '2021-07-12 10:00:00';
let form: { startDate: string, endDate: string, startTime?: string, endTime?: string, startSlot?: string, endSlot?: string};
let timeSlotTimePeriod: { start: ITimeSlot, end: ITimeSlot };
const time09: ITimeSlot = {
    name: '',
    startHour: '09:00',
    endHour: '10:00',
    id: "09"
}
const time11: ITimeSlot = {
    name: '',
    startHour: '11:00',
    endHour: '12:00',
    id: "11"
}

describe('setHourSelectorsFromTimeSlots', () => {
    test(`When end date got his timeslot before of start date one, correct it with the start date timePeriod end 
    (after start date entry)`, () => {
        form = {startDate: '2021-07-12 11:00:00', endDate: '2021-07-12 10:00:00'}
        timeSlotTimePeriod = {start: time11, end: time09}

        PeriodFormUtils.setHourSelectorsFromTimeSlots(
            date, TimeSlotHourPeriod.START_HOUR, timeSlotTimePeriod, form, "startDate", "endDate"
        );
        expect(form.startDate).toEqual('2021-07-12 11:00:00');
        expect(form.endDate).toEqual('2021-07-12 12:00:00');
        expect(timeSlotTimePeriod.start).toEqual(time11);
        expect(timeSlotTimePeriod.end).toEqual(time11);
    });

    test(`When start date got his timeslot after of end date one, correct it with the end date timePeriod start 
    (after end date entry)`, () => {
        form = {startDate: '2021-07-12 11:00:00', endDate: '2021-07-12 10:00:00'}
        timeSlotTimePeriod = {start: time11, end: time09}

        PeriodFormUtils.setHourSelectorsFromTimeSlots(
            date, TimeSlotHourPeriod.END_HOUR, timeSlotTimePeriod, form, "startDate", "endDate"
        );
        expect(form.startDate).toEqual('2021-07-12 09:00:00');
        expect(form.endDate).toEqual('2021-07-12 10:00:00');
        expect(timeSlotTimePeriod.start).toEqual(time09);
        expect(timeSlotTimePeriod.end).toEqual(time09);
    });
});


describe('setHourSelectorsFromTimeSlotsOrFree', () => {
    test(`When end date got his timeslot before of start date one, correct it with the start date timePeriod end 
    (after start date entry)`, () => {
        form = {startDate: '2021-07-12', endDate: '2021-07-12',
            startSlot: '2021-07-12 11:00:00', endSlot: '2021-07-12 10:00:00'}
        timeSlotTimePeriod = {start: time11, end: time09}

        PeriodFormUtils.setHourSelectorsFromTimeSlotsOrFree(
            TimeSlotHourPeriod.START_HOUR, false, form, "startTime", "endTime",
            "startDate", "endDate", timeSlotTimePeriod, form, "startSlot", "endSlot"
        );
        expect(form.startDate).toEqual('2021-07-12');
        expect(form.endDate).toEqual(new Date('2021-07-12T00:00:00.000Z'));
        expect(form.startSlot).toEqual('2021-07-12 11:00:00');
        expect(form.endSlot).toEqual('2021-07-12 12:00:00');
        expect(timeSlotTimePeriod.start).toEqual(time11);
        expect(timeSlotTimePeriod.end).toEqual(time11);
    });
});
