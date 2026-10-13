import { Button, Heading } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { useCourseContext } from '~/hooks/useCourseContext';

/** URL of the existing AngularJS application (full page navigation, outside the router). */
export const ANGULAR_APP_URL = '/presences';

export const Registers = () => {
  const { t } = useTranslation('presences');
  const courseContext = useCourseContext();

  return (
    <div className="py-16">
      <Heading level="h1" headingStyle="h1">
        {t('presences.register')}
      </Heading>
      <h1>Prise d'appel</h1>
      <p>
        {courseContext
          ? t('presences.register.context', { ...courseContext })
          : t('presences.register.context.invalid')}
      </p>
      <Button
        color="tertiary"
        variant="ghost"
        onClick={() => window.location.assign(ANGULAR_APP_URL)}
      >
        {t('presences.back')}
      </Button>
    </div>
  );
};

export default Registers;
