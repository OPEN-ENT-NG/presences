import { RegisterStudent } from '~/models/register';

/**
 * Students in alphabetical order of their name (`LASTNAME Firstname`): the API
 * gives them in no particular order. Accents and case do not count.
 */
export const sortStudentsByName = (
  students: RegisterStudent[],
): RegisterStudent[] =>
  [...students].sort((a, b) =>
    a.name.localeCompare(b.name, 'fr', { sensitivity: 'base' }),
  );
