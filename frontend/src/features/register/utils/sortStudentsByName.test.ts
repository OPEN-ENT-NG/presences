import { RegisterStudent } from '~/models/register';
import { sortStudentsByName } from './sortStudentsByName';

const student = (name: string) => ({ id: name, name }) as RegisterStudent;

describe('sortStudentsByName', () => {
  it('sorts the students by name, ignoring accents and case', () => {
    const students = ['MOREAU Jade', 'ÉVRARD Lou', 'andré Lucas'].map(student);

    expect(sortStudentsByName(students).map(({ name }) => name)).toStrictEqual([
      'andré Lucas',
      'ÉVRARD Lou',
      'MOREAU Jade',
    ]);
  });

  it('leaves the given list untouched', () => {
    const students = ['MOREAU Jade', 'ANDRÉ Lucas'].map(student);

    sortStudentsByName(students);

    expect(students.map(({ name }) => name)).toStrictEqual([
      'MOREAU Jade',
      'ANDRÉ Lucas',
    ]);
  });
});
