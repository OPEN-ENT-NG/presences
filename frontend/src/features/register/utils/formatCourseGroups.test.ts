import { formatCourseGroups } from './formatCourseGroups';

describe('formatCourseGroups', () => {
  it('gives the class of the course', () => {
    expect(formatCourseGroups({ classes: ['CP–CE1'], groups: [] })).toBe(
      'CP–CE1',
    );
  });

  it('lists the classes then the groups, like the AngularJS page', () => {
    expect(
      formatCourseGroups({ classes: ['CP', 'CE1'], groups: ['Chorale'] }),
    ).toBe('CP, CE1, Chorale');
  });
});
