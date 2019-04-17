import {DateUtils} from '../date'

const date = '2019-04-16 18:15:00';

describe('format', () => {
    test(`Using "${DateUtils.FORMAT["YEAR-MONTH-DAY"]}" it should return "2019-04-16"`, () => {
        expect(DateUtils.format(date, DateUtils.FORMAT["YEAR-MONTH-DAY"])).toEqual("2019-04-16");
    });

    test(`Using "${DateUtils.FORMAT["HOUR-MINUTES"]}" it should return "18:15"`, () => {
        expect(DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"])).toEqual("18:15");
    });

    test(`Using "${DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]}" it should return "${date}"`, () => {
        expect(DateUtils.format(date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])).toEqual(date);
    });

    test(`Using "${DateUtils.FORMAT.BIRTHDATE}" it should return "16/04/2019`, () => {
        expect(DateUtils.format(date, DateUtils.FORMAT.BIRTHDATE)).toEqual("16/04/2019");
    })
});

describe('getDayNumberDate', () => {
    test('It should return "2019-04-14"', () => {
        expect(DateUtils.getDayNumberDate(date, 0, DateUtils.FORMAT["YEAR-MONTH-DAY"])).toEqual("2019-04-14");
    })
});

describe('add', () => {
    test('Adding one (without step type) should add a day and return "2019-04-17"', () => {
        const newDate: Date = DateUtils.add(date, 1);
        expect(newDate.getDate()).toEqual(17);
        expect(newDate.getMonth()).toEqual(3);
        expect(newDate.getFullYear()).toEqual(2019);
    });

    test('Adding one month should return "2019-05-16"', () => {
        const newDate: Date = DateUtils.add(date, 1, 'M');
        expect(newDate.getDate()).toEqual(16);
        expect(newDate.getMonth()).toEqual(4);
        expect(newDate.getFullYear()).toEqual(2019);
    })
});