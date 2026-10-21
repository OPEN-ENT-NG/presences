import { ButtonBeta as Button } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { useGoBack } from '~/hooks/useGoBack';

export const BackButton = () => {
  const { t } = useTranslation('presences');
  const goBack = useGoBack();

  return (
    <Button color="tertiary" variant="ghost" onClick={goBack}>
      {t('presences.back')}
    </Button>
  );
};
