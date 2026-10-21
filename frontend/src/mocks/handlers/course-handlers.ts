import { HttpResponse, http } from 'msw';
import { baseUrl } from '~/services/api';
import { mockCourses } from '../datas/courses';

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

    const courses = mockCourses.filter((course) => {
      const day = course.startDate.slice(0, 10);
      return course.structureId === structure && day >= start && day <= end;
    });
    return HttpResponse.json(courses, { status: 200 });
  }),
];
