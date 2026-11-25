import { ButtonBeta as Button } from '@edifice.io/react';
import { IconArrowLeft } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useGoBack } from '~/hooks/useGoBack';

export const BackButton = () => {
  const { t } = useTranslation('presences');
  const goBack = useGoBack();

  return (
    <Button
      color="tertiary"
      variant="ghost"
      data-testid="presences-button-back"
      leftIcon={<IconArrowLeft />}
      onClick={goBack}
    >
      {t('presences.back')}
    </Button>
  );
};
