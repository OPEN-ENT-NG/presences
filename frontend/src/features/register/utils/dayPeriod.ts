import { readRegisterStart } from './registerStart';

export type DayPeriod = 'morning' | 'afternoon';

/**
 * Half day of a register (1D: two courses a day at most): a course starting
 * before noon is in the morning, otherwise in the afternoon.
 * @param startDate - Start of the register, see `readRegisterStart`.
 */
export const getDayPeriod = (startDate: string): DayPeriod =>
  readRegisterStart(startDate).hour < 12 ? 'morning' : 'afternoon';
