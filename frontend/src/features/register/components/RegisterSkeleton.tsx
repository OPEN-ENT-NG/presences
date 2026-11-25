import { Skeleton } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';

/** Rows of the mockup; the mobile layout only shows the first five. */
const ROWS = Array.from({ length: 10 }, (_, index) => index);

/** Register page while it opens, as drawn in the Figma mockups. */
export const RegisterSkeleton = () => {
  const { t } = useTranslation('presences');

  return (
    <div
      className="register register--skeleton"
      role="status"
      aria-busy="true"
      aria-label={t('presences.register.loading.text')}
    >
      <Skeleton variant="block" className="register__skeleton-back" />
      <div className="register__heading">
        <Skeleton variant="block" className="register__skeleton-title" />
        <Skeleton variant="block" className="register__skeleton-text" />
      </div>
      <ul className="register__students register__students--skeleton">
        {ROWS.map((row) => (
          <li key={row} className="register-student register-student--skeleton">
            <div className="register-student__identity">
              <Skeleton variant="circle" tone="strong" width={32} />
              <Skeleton
                variant="block"
                className="register-student__skeleton-name"
              />
            </div>
            <div className="register-student__actions">
              <Skeleton
                variant="block"
                className="register-student__skeleton-toggle"
              />
              <Skeleton
                variant="block"
                className="register-student__skeleton-menu"
              />
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
};
