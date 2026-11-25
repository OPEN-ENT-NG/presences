import { Avatar, useDirectory } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import forgoingError from '~/assets/icons/forgoing-error.svg';
import { RegisterStudent } from '~/models/register';

export const StudentRow = ({ student }: { student: RegisterStudent }) => {
  const { t } = useTranslation('presences');
  const { getAvatarURL } = useDirectory();
  const lastCourseAbsent = t('presences.register.last.course.absent');

  return (
    <li
      className="register-student"
      data-testid={`presences-row-student-${student.id}`}
    >
      <div className="register-student__identity">
        {/* Decorative: the name is written next to it */}
        <Avatar
          alt=""
          src={getAvatarURL(student.id, 'user')}
          size="sm"
          variant="circle"
          loading="lazy"
        />
        <span className="register-student__name">{student.name}</span>
        {student.last_course_absent && (
          <img
            className="register-student__last-absent"
            src={forgoingError}
            alt={lastCourseAbsent}
            title={lastCourseAbsent}
          />
        )}
      </div>
    </li>
  );
};
