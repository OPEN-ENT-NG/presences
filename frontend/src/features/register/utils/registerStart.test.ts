import { readRegisterStart } from './registerStart';

describe('readRegisterStart', () => {
  it('reads the day and the hour of the register start', () => {
    expect(readRegisterStart('2026-10-13T08:30:00.000')).toStrictEqual({
      day: '2026-10-13',
      hour: 8,
    });
  });

  it('keeps the local day and hour, with no time zone shift', () => {
    expect(readRegisterStart('2026-10-13T23:45:00.000')).toStrictEqual({
      day: '2026-10-13',
      hour: 23,
    });
  });
});
