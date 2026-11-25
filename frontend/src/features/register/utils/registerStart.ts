/** Start of a register, as the parts the page reads. */
export interface RegisterStart {
  /** `YYYY-MM-DD` */
  day: string;
  /** 0 to 23 */
  hour: number;
}

/**
 * Reads the start of a register, kept in local time: no `Date` parsing, so no
 * time zone can shift the day or the hour.
 * @param startDate - `YYYY-MM-DDTHH:mm:ss.SSS` (local time).
 */
export const readRegisterStart = (startDate: string): RegisterStart => ({
  day: startDate.slice(0, 10),
  hour: Number(startDate.slice(11, 13)),
});
