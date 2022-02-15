import {Indicator} from './index';
import {INDICATOR_TYPE} from "../core/constants/IndicatorType";
import {FILTER_TYPE} from '../filter';
import {IPunishmentType} from '@incidents/models/PunishmentType';
import {IStructureSlot, ITimeSlot, Reason, Student} from '@presences/models';
import {IWeekly, IWeeklyResponse, WeeklyStatistics, WeeklyStatisticsResponse} from "@statistics/model/Weekly";
import {DateUtils} from "@common/utils";
import {DISPLAY_TYPE} from "../core/constants/DisplayMode";
import {IndicatorFactory} from '../indicator';
import {AxiosError} from "axios";
import {idiom as lang, model, moment, toasts} from "entcore";
import {ViescolaireService} from "@common/services";
import {timeslotClasseService} from "@common/services/TimeslotClasseService";

declare let window: any;

export class Weekly extends Indicator {
    audienceFilter: string;
    userFilter: string;
    disabled: boolean;
    timeslot: ITimeSlot[];

    constructor(reasons: Reason[], punishmentTypes: IPunishmentType[]) {
        super(INDICATOR_TYPE.weekly, reasons, punishmentTypes);
        this.resetValues();
        this.setFilterTypes([
            this._factoryFilter.getFilter(FILTER_TYPE.NO_REASON, null),
            this._factoryFilter.getFilter(FILTER_TYPE.UNREGULARIZED, (value: boolean) => this._factoryFilter.changeUnProvingReasons(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.REGULARIZED, (value: boolean) => this._factoryFilter.changeProvingReasons(value)),
            this._factoryFilter.getFilter(FILTER_TYPE.LATENESS, null),
            this._factoryFilter.getFilter(FILTER_TYPE.DEPARTURE, null)
        ]);
        this.values = {slots: []};
        this.timeslot = [];
        model.calendar.setDate(moment());
    }

    isEmpty(): boolean {
        return (this.values as IWeekly).slots === null || (this.values as IWeekly).slots.length == 0 ||
            (this.audienceFilter == null && this.userFilter == null);
    }

    resetDates(): void {
        this._from = DateUtils.setFirstTime(new Date());
        this._to = DateUtils.setLastTime(new Date());
    }

    resetDisplayMode(): void {
        this._display = DISPLAY_TYPE.TABLE;
    }

    resetValues(): void {
    }

    async search(start: Date, end: Date, users: string[], audiences: string[]): Promise<void> {
        await new Promise((resolve, reject) => {
            if (audiences.length == 0 && users.length == 0) {
                this.values = {slots: []};
                resolve();
                return;
            }
            super.fetchIndicator(start, end, users, audiences)
                .then((res: IWeeklyResponse) => {
                    this._mapResults(res)
                    this.values = {slots: this._mapResults(res)}
                    resolve();
                })
                .catch((error: AxiosError) => {
                    reject(error);
                });
        });
    }

    private _mapResults(res: IWeeklyResponse): Array<WeeklyStatistics> {
           return res.data.map((week: WeeklyStatisticsResponse) => {
                const slot = this.timeslot.find((slot: ITimeSlot) => slot._id == week.slot_id);
                if (!!slot && !!slot.startHour && !!slot.endHour) {
                    return {
                        dayOfWeek: week.dayOfWeek,
                        slot_id: slot._id,
                        endMoment: moment()
                            .isoWeekday(week.dayOfWeek)
                            .set('hour', Number(slot.endHour.split(":")[0]))
                            .set('minute', Number(slot.endHour.split(":")[1]))
                            .set('second', 0)
                            .set('millisecond', 0),
                        startMoment: moment()
                            .isoWeekday(week.dayOfWeek)
                            .set('hour', Number(slot.startHour.split(":")[0]))
                            .set('minute', Number(slot.startHour.split(":")[1]))
                            .set('second', 0)
                            .set('millisecond', 0),
                        is_periodic: false,
                        locked: true,
                        rate: week.rate,
                        max: week.max
                    } as WeeklyStatistics;
                }
            });
    }

    public async initTimeslot(users: string[], audiences: string[]): Promise<void> {
        try {
            this.userFilter = (!!users.length && !audiences.length) ? users[0] : null;
            this.audienceFilter = (!!audiences.length) ? audiences[0] : null;

            let structure_slots: IStructureSlot;
            if (!this.audienceFilter && !this.userFilter) {
                structure_slots = await ViescolaireService.getSlotProfile(window.structure.id);
            } else {
                let audience: string = this.audienceFilter ? this.audienceFilter : await this.getAudienceFromUserFilter();
                structure_slots = await timeslotClasseService.getAudienceTimeslot(audience);
            }
            this.timeslot = structure_slots.slots;
            model.calendar.setTimeslots(this.timeslot);
        } catch (err) {
            this.disabled = true;
            toasts.warning("statistics-presences.slots.error");
            console.error(err, err.stack);
        }
    }


    private async getAudienceFromUserFilter(): Promise<any> {
        return new Promise(async (resolve, reject) => {
            let students: Array<Student> = await ViescolaireService.getStudent(window.structure.id, this.userFilter);
            if (students.length == 0) {
                reject(lang.translate("statistics-presences.slots.error"));
            } else {
                resolve(students[0].idClasse);
            }
        });
    }

}

IndicatorFactory.register(Weekly);