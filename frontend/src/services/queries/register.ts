import { queryOptions } from '@tanstack/react-query';
import { registerService } from '~/services/api';

/**
 * registerQueryOptions: a register with its students and events.
 * The cache is the single source of the register data.
 */
export const registerQueryOptions = (registerId: number) =>
  queryOptions({
    queryKey: ['register', registerId],
    queryFn: () => registerService.getRegister(registerId),
  });
