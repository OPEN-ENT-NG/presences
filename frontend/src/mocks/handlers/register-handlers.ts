import { HttpResponse, http } from 'msw';
import { CreateRegisterPayload } from '~/models/register';
import { baseUrl } from '~/services/api';
import { mockRegisters, toCreatedRegister } from '../datas/registers';

/**
 * MSW Handlers
 * Mock HTTP methods for register service
 */
export const registerHandlers = [
  // Create a register: idempotent, returns the existing register of the course
  http.post(`${baseUrl}/registers`, async ({ request }) => {
    const body = (await request.json()) as CreateRegisterPayload;
    const entry = Object.entries(mockRegisters).find(
      ([, register]) => register.course_id === body.course_id,
    );
    if (!entry) {
      return new HttpResponse(null, { status: 500 });
    }
    const [id, register] = entry;
    return HttpResponse.json(toCreatedRegister(Number(id), register), {
      status: 201,
    });
  }),

  // Get a register with its students
  http.get(`${baseUrl}/registers/:id`, ({ params }) => {
    const id = Number(params.id);
    if (!Number.isInteger(id)) {
      return new HttpResponse(null, { status: 400 });
    }
    const register = mockRegisters[id];
    if (!register) {
      return new HttpResponse(null, { status: 404 });
    }
    return HttpResponse.json(register, { status: 200 });
  }),
];
