import {DateUtils} from '../date'
import {moment} from "entcore";

const date = '2019-04-16 18:15:00';
const falsyDateString = 'falsy date string';

describe('format', () => {

    beforeAll(() => {
        moment.locale('fr');
    });

    test(`Using "${DateUtils.FORMAT["YEAR-MONTH-DAY"]}" it should returns "2019-04-16"`, () => {
        expect(DateUtils.format(date, DateUtils.FORMAT["YEAR-MONTH-DAY"])).toEqual("2019-04-16");
    });

    test(`Using "${DateUtils.FORMAT["HOUR-MINUTES"]}" it should returns "18:15"`, () => {
        expect(DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"])).toEqual("18:15");
    });

    test(`Using "${DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]}" it should returns "${date}"`, () => {
        expect(DateUtils.format(date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])).toEqual(date);
    });

    test(`Using "${DateUtils.FORMAT.BIRTHDATE}" it should returns "16/04/2019`, () => {
        expect(DateUtils.format(date, DateUtils.FORMAT.BIRTHDATE)).toEqual("16/04/2019");
    });

    test(`Using "${DateUtils.FORMAT["DAY-DATE"]} it should returns "mardi 16/04/2019"`, () => {
        expect(DateUtils.format(date, DateUtils.FORMAT["DAY-DATE"])).toEqual("mardi 16/04/2019");
    });
});

describe('getDayNumberDate', () => {
    test('It should return "2019-04-14"', () => {
        expect(DateUtils.getDayNumberDate(date, 0, DateUtils.FORMAT["YEAR-MONTH-DAY"])).toEqual("2019-04-14");
    });
});

describe('add', () => {
    test('Adding one (without step type) should add a day and return "2019-04-17"', () => {
        const newDate: Date = DateUtils.add(date, 1);
        expect(newDate.getDate()).toEqual(17);
        expect(newDate.getMonth()).toEqual(3);
        expect(newDate.getFullYear()).toEqual(2019);
    });

    test('Adding one month should returns "2019-05-16"', () => {
        const newDate: Date = DateUtils.add(date, 1, 'M');
        expect(newDate.getDate()).toEqual(16);
        expect(newDate.getMonth()).toEqual(4);
        expect(newDate.getFullYear()).toEqual(2019);
    })
});


describe('setFirstTime', () => {
    test('setFirstTime should set date to midnight', () => {
        const firstTimeDate: Date = DateUtils.setFirstTime(date);
        expect(DateUtils.format(firstTimeDate, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])).toEqual('2019-04-16 00:00:00');
    });
});

describe('setLastTime', () => {
    test(`setLastTime should set date to '2019-04-16 23:59:59'`, () => {
        const lastTimeDate: Date = DateUtils.setLastTime(date);
        expect(DateUtils.format(lastTimeDate, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])).toEqual('2019-04-16 23:59:59');
    });
});

describe('isValid', () => {
    test('isValid should returns false', () => {
        expect(DateUtils.isValid(falsyDateString, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])).toBeFalsy();
    });

    test('isValid should returns tru', () => {
        expect(DateUtils.isValid(date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])).toBeTruthy();
    });
});