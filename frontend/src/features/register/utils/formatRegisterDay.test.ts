import { formatRegisterDay } from './formatRegisterDay';

describe('formatRegisterDay', () => {
  it('gives the weekday, the day, the month and the year in French', () => {
    expect(formatRegisterDay('2026-07-21T08:30:00.000', 'fr')).toBe(
      'mardi 21 juillet 2026',
    );
  });

  it('keeps the day of the register late in the evening (no time zone shift)', () => {
    expect(formatRegisterDay('2026-10-13T23:30:00.000', 'fr')).toBe(
      'mardi 13 octobre 2026',
    );
  });

  it('follows the language of the user', () => {
    expect(formatRegisterDay('2026-07-21T08:30:00.000', 'en')).toBe(
      'Tuesday, July 21, 2026',
    );
  });
});
