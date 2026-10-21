import { Flex, Heading } from '@edifice.io/react';
import { QueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { LoaderFunctionArgs, useParams } from 'react-router-dom';
import { BackButton } from '~/components/BackButton/BackButton';
import { HttpError } from '~/services/api';
import { registerQueryOptions } from '~/services/queries/register';
import { sortStudentsByName } from './sortStudents';

export const loader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const registerId = Number(params.registerId);
    if (!Number.isInteger(registerId)) {
      throw new HttpError(404, 'Invalid register identifier');
    }

    // Static: reuse the register already in the cache, fetch it otherwise
    await queryClient.query({
      ...registerQueryOptions(registerId),
      staleTime: 'static',
    });
    return null;
  };

export const Register = () => {
  const { t } = useTranslation('presences');
  const { registerId } = useParams();
  // Already in the cache: the loader waited for it
  const { data: register } = useSuspenseQuery(
    registerQueryOptions(Number(registerId)),
  );

  // `YYYY-MM-DDTHH:mm:ss.SSS`
  const date = register.start_date.slice(0, 10);
  const startTime = register.start_date.slice(11, 16);
  const endTime = register.end_date.slice(11, 16);

  return (
    <Flex direction="column" align="start" gap="8">
      <Heading level="h2" headingStyle="h1">
        {t('presences.register')}
      </Heading>
      <p>{t('presences.register.context', { date, startTime, endTime })}</p>
      <Flex as="ul" direction="column" gap="4">
        {sortStudentsByName(register.students).map(({ id, name }) => (
          <li key={id} data-testid={`presences-label-student-${id}`}>
            {name}
          </li>
        ))}
      </Flex>
      <BackButton />
    </Flex>
  );
};

export default Register;
