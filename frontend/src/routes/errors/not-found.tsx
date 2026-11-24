import {
  ButtonBeta as Button,
  EmptyScreen,
  Flex,
  PageLayout,
} from '@edifice.io/react';
import illuSearch from '@images/emptyscreen/illu-search.svg';
import { t } from 'i18next';
import { useNavigate, useRouteError } from 'react-router-dom';

export const NotFound = () => {
  const error = useRouteError();
  const navigate = useNavigate();
  console.error(error);

  return (
    <Flex as={PageLayout} direction="column">
      <Flex
        as={PageLayout.Content}
        fill
        direction="column"
        align="center"
        justify="center"
        gap="16"
      >
        <EmptyScreen
          imageSrc={illuSearch}
          title={t('oops')}
          text={t('e404.page')}
        />
        <Button
          data-testid="presences-button-not-found-back"
          onClick={() => navigate(-1)}
        >
          {t('back')}
        </Button>
      </Flex>
    </Flex>
  );
};
