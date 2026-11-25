import { getDayPeriod } from './dayPeriod';

describe('getDayPeriod', () => {
  it.each([
    ['2026-10-13T08:30:00.000', 'morning'],
    ['2026-10-13T11:59:00.000', 'morning'],
    ['2026-10-13T12:00:00.000', 'afternoon'],
    ['2026-10-13T13:30:00.000', 'afternoon'],
  ])('a course starting at %s is in the %s', (startDate, period) => {
    expect(getDayPeriod(startDate)).toBe(period);
  });
});
