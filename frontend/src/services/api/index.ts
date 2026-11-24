import { createCourseService } from './courseService';
import { createRegisterService } from './registerService';

export const baseUrl = '/presences';

export const courseService = createCourseService();
export const registerService = createRegisterService();

export * from './http';
