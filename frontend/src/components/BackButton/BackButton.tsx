import { ButtonBeta as Button } from '@edifice.io/react';
import { IconRafterLeft } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useGoBack } from '~/hooks/useGoBack';

export const BackButton = () => {
  const { t } = useTranslation('presences');
  const goBack = useGoBack();

  return (
    <Button
      data-testid="presences-button-back"
      leftIcon={<IconRafterLeft />}
      onClick={goBack}
    >
      {t('presences.back')}
    </Button>
  );
};
