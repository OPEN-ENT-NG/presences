import { describe, expect, it } from 'vitest';
import { parseCourseContext } from './useCourseContext';

const VALID = {
  courseId: 'course-1',
  structureId: 'structure-1',
  date: '2026-10-13',
  startTime: '08:30',
  endTime: '11:45',
};

const params = (values: Record<string, string>) => new URLSearchParams(values);

describe('parseCourseContext', () => {
  it('returns the context when every parameter is valid', () => {
    expect(parseCourseContext(params(VALID))).toEqual(VALID);
  });

  it.each(Object.keys(VALID))('returns null when %s is missing', (key) => {
    const values: Record<string, string> = { ...VALID };
    delete values[key];
    expect(parseCourseContext(params(values))).toBeNull();
  });

  it.each([
    ['date', '13/10/2026'],
    ['date', '2026-13-01'],
    ['date', '2026-02-31'],
    ['date', '2026-04-31'],
    ['startTime', '8:30'],
    ['endTime', '24:00'],
  ])('returns null when %s is malformed (%s)', (key, value) => {
    expect(parseCourseContext(params({ ...VALID, [key]: value }))).toBeNull();
  });

  it('returns null when startTime is not before endTime', () => {
    expect(
      parseCourseContext(
        params({ ...VALID, startTime: '14:00', endTime: '13:00' }),
      ),
    ).toBeNull();
  });
});
