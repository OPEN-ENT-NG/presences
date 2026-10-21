import {
  AppHeader,
  Breadcrumb,
  ButtonBeta as Button,
  EmptyScreen,
  Flex,
  PageLayout,
  useEdificeClient,
} from '@edifice.io/react';
import illuError from '@images/emptyscreen/illu-error.svg';
import { useTranslation } from 'react-i18next';
import { useNavigate, useRouteError } from 'react-router-dom';

export const PageError = () => {
  const error = useRouteError();
  const navigate = useNavigate();
  const { appCode, currentApp } = useEdificeClient();
  const { t } = useTranslation(appCode);
  console.error(error);

  return (
    <Flex as={PageLayout} direction="column">
      <PageLayout.Header />
      {currentApp && (
        <PageLayout.Breadcrumb>
          <AppHeader>
            <Breadcrumb app={currentApp} />
          </AppHeader>
        </PageLayout.Breadcrumb>
      )}
      <Flex
        as={PageLayout.Content}
        fill
        direction="column"
        align="center"
        justify="center"
        gap="16"
      >
        <EmptyScreen
          imageSrc={illuError}
          title={t('oops')}
          text={t('presences.notfound.or.unauthorized')}
        />
        <Button
          data-testid="presences-button-page-error-back"
          onClick={() => navigate(-1)}
        >
          {t('back')}
        </Button>
      </Flex>
    </Flex>
  );
};
