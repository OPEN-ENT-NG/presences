import { Heading } from '@edifice.io/react';
import { Suspense } from 'react';
import { QueryClient, useQuery, useSuspenseQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { LoaderFunctionArgs, useParams } from 'react-router-dom';
import { BackButton } from '~/components/BackButton/BackButton';
import {
  RegisterFooter,
  RegisterSkeleton,
  StudentRow,
  formatCourseGroups,
  formatRegisterDay,
  getDayPeriod,
  readRegisterStart,
  sortStudentsByName,
} from '~/features/register';
import { CourseDay, findCourse } from '~/models/course';
import { Register as RegisterModel } from '~/models/register';
import { HttpError } from '~/services/api';
import { coursesQueryOptions } from '~/services/queries/course';
import { registerQueryOptions } from '~/services/queries/register';

/** The day of the register, where its course is. */
const courseDayOf = (register: RegisterModel): CourseDay => ({
  structureId: register.structure_id,
  date: readRegisterStart(register.start_date).day,
});

export const loader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const registerId = Number(params.registerId);
    if (!Number.isInteger(registerId)) {
      throw new HttpError(404, 'Invalid register identifier');
    }

    // Static: reuse the register already in the cache, fetch it otherwise
    const register = await queryClient.query({
      ...registerQueryOptions(registerId),
      staleTime: 'static',
    });

    await queryClient
      .query(coursesQueryOptions(courseDayOf(register)))
      .catch(() => undefined);
    return null;
  };

const RegisterContent = () => {
  const { t, i18n } = useTranslation('presences');
  const { registerId } = useParams();
  // Already in the cache: the loader waited for it
  const { data: register } = useSuspenseQuery(
    registerQueryOptions(Number(registerId)),
  );
  const { data: course } = useQuery({
    ...coursesQueryOptions(courseDayOf(register)),
    select: (courses) => findCourse(courses, register.course_id),
  });

  const date = formatRegisterDay(register.start_date, i18n.language);
  const groups = course ? formatCourseGroups(course) : '';

  return (
    <div className="register">
      <div>
        <BackButton />
      </div>
      <div className="register__heading">
        <Heading level="h1" headingStyle="h2">
          {groups
            ? t('presences.register.groups.title', { groups })
            : t('presences.register')}
        </Heading>
        <p>
          {t(`presences.register.period.${getDayPeriod(register.start_date)}`, {
            date,
          })}
        </p>
      </div>
      <ul className="register__students">
        {sortStudentsByName(register.students).map((student) => (
          <StudentRow key={student.id} student={student} />
        ))}
      </ul>
      <RegisterFooter />
    </div>
  );
};

export const Register = () => (
  <Suspense fallback={<RegisterSkeleton />}>
    <RegisterContent />
  </Suspense>
);

export default Register;
