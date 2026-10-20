import { Button, Heading } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';

/** URL de l'application AngularJS existante (navigation complète, hors router). */
export const ANGULAR_APP_URL = '/presences';

export const Registers = () => {
  const { t } = useTranslation('presences');

  return (
    <div className="py-16">
      <Heading level="h1" headingStyle="h1">
        {t('presences.register')}
      </Heading>
      <h1>Prise d'appel</h1>
      <Button
        color="tertiary"
        variant="ghost"
        onClick={() => window.location.assign(ANGULAR_APP_URL)}
      >
        {t('presences.react.back')}
      </Button>
    </div>
  );
};

export default Registers;
