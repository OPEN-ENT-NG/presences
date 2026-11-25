import { readRegisterStart } from './registerStart';

/**
 * Day of a register, e.g. `mardi 21 juillet 2026`.
 * The date is read as a calendar day, so no time zone can shift it.
 * @param startDate - Start of the register, see `readRegisterStart`.
 * @param locale - Language of the user.
 */
export const formatRegisterDay = (
  startDate: string,
  locale: string,
): string => {
  const [year, month, day] = readRegisterStart(startDate)
    .day.split('-')
    .map(Number);

  return new Intl.DateTimeFormat(locale, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(year, month - 1, day)));
};
