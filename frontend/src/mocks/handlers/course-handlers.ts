import { HttpResponse, http } from 'msw';
import { baseUrl } from '~/services/api';
import { mockCourses } from '../datas/courses';
import {
  MOCK_STRUCTURE_FORBIDDEN,
  MOCK_STRUCTURE_OFFLINE,
  MOCK_STRUCTURE_SERVER_ERROR,
  MOCK_STRUCTURE_SESSION_EXPIRED,
} from '../datas/entry-points';

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/**
 * MSW Handlers
 * Mock HTTP methods for course service
 */
export const courseHandlers = [
  // Get the courses of a structure (one day for the register page)
  http.get(`${baseUrl}/courses`, ({ request }) => {
    const params = new URL(request.url).searchParams;
    const structure = params.get('structure');
    const start = params.get('start');
    const end = params.get('end');

    // Same parameter check as the back
    if (
      !structure ||
      !start ||
      !end ||
      !DATE_PATTERN.test(start) ||
      !DATE_PATTERN.test(end)
    ) {
      return new HttpResponse(null, { status: 400 });
    }

    // Structures made to fail, to see the error screens
    if (structure === MOCK_STRUCTURE_FORBIDDEN) {
      // How the ENT refuses a missing right: a 401 with an HTML error page
      return HttpResponse.html('<html>Erreur</html>', { status: 401 });
    }
    if (structure === MOCK_STRUCTURE_SESSION_EXPIRED) {
      // What the ENT answers once the session expired: the login page
      return HttpResponse.html('<html>login</html>');
    }
    if (structure === MOCK_STRUCTURE_SERVER_ERROR) {
      return new HttpResponse(null, { status: 500 });
    }
    if (structure === MOCK_STRUCTURE_OFFLINE) {
      return HttpResponse.error();
    }

    const courses = mockCourses.filter((course) => {
      const day = course.startDate.slice(0, 10);
      return course.structureId === structure && day >= start && day <= end;
    });
    return HttpResponse.json(courses, { status: 200 });
  }),
];
