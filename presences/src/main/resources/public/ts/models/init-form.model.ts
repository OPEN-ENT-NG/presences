import {DateUtils} from "@common/utils";
import {DAY_OF_WEEK} from "../core/enum/day-of-week.enum";
import {moment} from "entcore";
import {INIT_TYPE} from "@presences/core/enum/init-type";

export enum HOLIDAY_SYSTEM {
    FRENCH = "FRENCH",
    OTHER = "OTHER"
}

// Zone values that will be passed to the https://data.education.gouv.fr/ API.
// Values are validated by the backend in viescolaire module in file:
// "vie-scolaire/src/main/resources/jsonschema/init_structure.json" 
// So values in this enum and values in backend validator must match! :)
export enum HOLIDAYS_ZONE {
    ZONE_A = "Zone A",
    ZONE_B = "Zone B",
    ZONE_C = "Zone C",
    CORSE = "Corse",
    GUADELOUPE = "Guadeloupe",
    GUYANE = "Guyane",
    MARTINIQUE = "Martinique",
    MAYOTTE = "Mayotte",
    NOUVELLE_CALEDONIE = "Nouvelle Calédonie",
    POLYNESIE = "Polynésie",
    REUNION = "Réunion",
    SAINT_PIERRE_ET_MIQUELON = "Saint Pierre et Miquelon",
    WALLIS_ET_FUTUNA = "Wallis et Futuna",
}

export interface IInitFormDay {
    label: string;
    value: string;
    isChecked: boolean;
}


export interface IInitForm {
    schoolYear: IInitFormYear;
    timetable: IInitFormTimetable;
    holidays: IInitFormHolidays;

    initType: INIT_TYPE;
}

export interface IInitFormYear {
    startDate: string;
    endDate: string;
}

export interface IInitFormTimetable {
    morning: IInitFormDayHours;
    afternoon: IInitFormDayHours;
    fullDays: Array<string>;
    halfDays: Array<string>;
}

export interface IInitFormDayHours {
    startHour: string;
    endHour: string;
}

export interface IInitFormHolidays {
    system: string;
    zone: string;
}

export interface IInitFormZone {
    value: string;
    label: string;
}

export class InitFormYear {

    private _startDate: string;
    private _endDate: string;

    constructor() {
        this._startDate = "";
        this._endDate = "";
    }

    get startDate(): string {
        return this._startDate;
    }

    set startDate(value: string) {
        this._startDate = value;
    }

    get endDate(): string {
        return this._endDate;
    }

    set endDate(value: string) {
        this._endDate = value;
    }

    isValid(): boolean {
        return DateUtils.isPeriodValid(this.startDate, this.endDate);
    }

    toJSON(): IInitFormYear {
        return {
            startDate: DateUtils.format(this.startDate,DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
            endDate: DateUtils.format(this.endDate,DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])
        };
    }
}

export class InitFormTimetable {

    private _morning: IInitFormDayHours;
    private _afternoon: IInitFormDayHours;
    private _fullDays: Array<IInitFormDay>;
    private _halfDays: Array<IInitFormDay>;

    constructor() {
        this._morning = {
            startHour: moment().set({hour: 8, minute: 0, second: 0, millisecond: 0}).toDate(),
            endHour: moment().set({hour: 12, minute: 0, second: 0, millisecond: 0}).toDate(),
        };
        this._afternoon = {
            startHour: moment().set({hour: 14, minute: 0, second: 0, millisecond: 0}).toDate(),
            endHour: moment().set({hour: 17, minute: 0, second: 0, millisecond: 0}).toDate(),
        };
        this.fullDays = [
            {label: 'presences.monday', value: DAY_OF_WEEK.MONDAY, isChecked: true},
            {label: 'presences.tuesday', value: DAY_OF_WEEK.TUESDAY, isChecked: true},
            {label: 'presences.wednesday', value: DAY_OF_WEEK.WEDNESDAY, isChecked: false},
            {label: 'presences.thursday', value: DAY_OF_WEEK.THURSDAY, isChecked: true},
            {label: 'presences.friday', value: DAY_OF_WEEK.FRIDAY, isChecked: true},
            {label: 'presences.saturday', value: DAY_OF_WEEK.SATURDAY, isChecked: false},
        ];

        this.halfDays = [
            {label: 'presences.monday', value: DAY_OF_WEEK.MONDAY, isChecked: false},
            {label: 'presences.tuesday', value: DAY_OF_WEEK.TUESDAY, isChecked: false},
            {label: 'presences.wednesday', value: DAY_OF_WEEK.WEDNESDAY, isChecked: true},
            {label: 'presences.thursday', value: DAY_OF_WEEK.THURSDAY, isChecked: false},
            {label: 'presences.friday', value: DAY_OF_WEEK.FRIDAY, isChecked: false},
            {label: 'presences.saturday', value: DAY_OF_WEEK.SATURDAY, isChecked: false},
        ];
    }

    get morning(): IInitFormDayHours {
        return this._morning;
    }

    set morning(value: IInitFormDayHours) {
        this._morning = value;
    }

    set morningStartHour(value: string) {
        this._morning.startHour = value;
    }

    set morningEndHour(value: string) {
        this._morning.endHour = value;
    }

    set afternoonStartHour(value: string) {
        this._afternoon.startHour = value;
    }

    set afternoonEndHour(value: string) {
        this._afternoon.endHour = value;
    }

    get afternoon(): IInitFormDayHours {
        return this._afternoon;
    }

    set afternoon(value: IInitFormDayHours) {
        this._afternoon = value;
    }

    get fullDays(): Array<IInitFormDay> {
        return this._fullDays;
    }

    set fullDays(value: Array<IInitFormDay>) {
        this._fullDays = value;
    }

    setFullDay(value: IInitFormDay) {
        const dayIndex: number = Object.keys(DAY_OF_WEEK).indexOf(value.value);

        if (dayIndex !== -1) {
            if (this._fullDays[dayIndex].isChecked) {
                this._fullDays[dayIndex].isChecked = false;
                return;
            }

            this._fullDays[dayIndex].isChecked = true;
            this._halfDays[dayIndex].isChecked = false;
        }
    }

    setHalfDay(value: IInitFormDay) {
        const dayIndex: number = Object.keys(DAY_OF_WEEK).indexOf(value.value);

        if (dayIndex !== -1) {
            if (this._halfDays[dayIndex].isChecked) {
                this._halfDays[dayIndex].isChecked = false;
                return;
            }

            this._fullDays[dayIndex].isChecked = false;
            this._halfDays[dayIndex].isChecked = true;
        }
    }

    get halfDays(): Array<IInitFormDay> {
        return this._halfDays;
    }

    set halfDays(value: Array<IInitFormDay>) {
        this._halfDays = value;
    }

    isValid(): boolean {
        return this.morning.startHour != null
            && this.morning.endHour != null
            && this.afternoon.startHour != null
            && this.afternoon.endHour != null
            && DateUtils.isPeriodValid(this.morning.startHour, this.morning.endHour)
            && DateUtils.isPeriodValid(this.afternoon.startHour, this.afternoon.endHour)
            && DateUtils.isPeriodValid(this.morning.endHour, this.afternoon.startHour);
    }

    toJSON(): IInitFormTimetable {
        return {
            morning: {
                startHour: DateUtils.format(this.morning.startHour, DateUtils.FORMAT['HOUR-MIN']),
                endHour: DateUtils.format(this.morning.endHour, DateUtils.FORMAT['HOUR-MIN'])
            },
            afternoon: {
                startHour: DateUtils.format(this.afternoon.startHour, DateUtils.FORMAT['HOUR-MIN']),
                endHour: DateUtils.format(this.afternoon.endHour, DateUtils.FORMAT['HOUR-MIN'])
            },
            fullDays: this.fullDays
                .filter((day: IInitFormDay) => day.isChecked)
                .map((day: IInitFormDay) => day.value),
            halfDays: this.halfDays
                .filter((day: IInitFormDay) => day.isChecked)
                .map((day: IInitFormDay) => day.value)
        };
    }
}

export class InitFormHolidays {
    private _system: string;
    private _zone: string;

    constructor() {
        this._system = HOLIDAY_SYSTEM.FRENCH;
        this._zone = null;
    }

    get system(): string {
        return this._system;
    }

    set system(value: string) {
        switch (value) {
            case HOLIDAY_SYSTEM.FRENCH:
                this._system = HOLIDAY_SYSTEM.FRENCH;
                break;
            default:
                this._system = HOLIDAY_SYSTEM.OTHER;
                break;
        }
    }

    get zone(): string {
        return this._zone;
    }

    set zone(value: string) {
        this._zone = value;
    }

    isValid(): boolean {
        return (this.system === HOLIDAY_SYSTEM.FRENCH && this.zone != null) || this.system === HOLIDAY_SYSTEM.OTHER;
    }

    toJSON(): IInitFormHolidays {
        return {
            system: this.system,
            zone: this.zone
        }
    }
}

export class InitForm {

    private _schoolYear: InitFormYear;
    private _timetable: InitFormTimetable;
    private _holidays: InitFormHolidays;

    private _initType: INIT_TYPE;

    constructor() {
        this._schoolYear = new InitFormYear();
        this._timetable = new InitFormTimetable();
        this._holidays = new InitFormHolidays();
        this._initType = null;
    }

    get schoolYear(): InitFormYear {
        return this._schoolYear;
    }

    set schoolYear(value: InitFormYear) {
        this._schoolYear = value;
    }

    get timetable(): InitFormTimetable {
        return this._timetable;
    }

    set timetable(value: InitFormTimetable) {
        this._timetable = value;
    }

    get holidays(): InitFormHolidays {
        return this._holidays;
    }

    set holidays(value: InitFormHolidays) {
        this._holidays = value;
    }

    get initType(): INIT_TYPE {
        return this._initType;
    }

    set initType(value: INIT_TYPE) {
        this._initType = value;
    }


    isValid = (): boolean => {
        return this.schoolYear.isValid() && this.timetable.isValid() && this.holidays.isValid();
    }

    toJSON(): IInitForm {
        return {
            schoolYear: this.schoolYear.toJSON(),
            timetable: this.timetable.toJSON(),
            holidays: this.holidays.toJSON(),
            initType: this.initType
        }
    }


}

