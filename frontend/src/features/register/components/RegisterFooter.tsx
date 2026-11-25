import { ButtonBeta as Button } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';

/**
 * End of the student list: the counters on one side, the validation on the
 * other. TODO: counters and validation come with their own user stories.
 */
export const RegisterFooter = () => {
  const { t } = useTranslation('presences');

  return (
    <div className="register__footer" data-testid="presences-footer-register">
      <div data-testid="presences-counters-register" />
      <Button data-testid="presences-button-validate-register">
        {t('presences.register.validation')}
      </Button>
    </div>
  );
};
