import { ButtonBeta as Button, EmptyScreen } from '@edifice.io/react';
import illuError from '@images/emptyscreen/illu-error.svg';
import illuSearch from '@images/emptyscreen/illu-search.svg';
import { useTranslation } from 'react-i18next';
import { useRevalidator } from 'react-router-dom';
import { BackButton } from '~/components/BackButton/BackButton';
import { ErrorKind } from './getErrorKind';
import { IconRafterLeft, IconRefresh } from '@edifice.io/react/icons';
/** What the user can do from an error screen. */
type ErrorAction = 'back' | 'retry' | 'reconnect';

/** i18n key prefix, illustration and action of each error screen. */
const SCREENS: Record<
  ErrorKind,
  { key: string; image: string; action: ErrorAction }
> = {
  sessionExpired: {
    key: 'presences.error.session',
    image: illuError,
    action: 'reconnect',
  },
  forbidden: {
    key: 'presences.error.forbidden',
    image: illuError,
    action: 'back',
  },
  courseNotFound: {
    key: 'presences.error.course.notfound',
    image: illuSearch,
    action: 'back',
  },
  registerNotFound: {
    key: 'presences.error.register.notfound',
    image: illuSearch,
    action: 'back',
  },
  server: { key: 'presences.error.server', image: illuError, action: 'retry' },
  offline: {
    key: 'presences.error.offline',
    image: illuError,
    action: 'retry',
  },
};

/** Loads the page again: its loaders run once more, and the error goes away if they succeed. */
const RetryButton = () => {
  const { t } = useTranslation('presences');
  const { revalidate, state } = useRevalidator();

  return (
    <Button
      leftIcon={<IconRefresh />}
      isLoading={state === 'loading'}
      onClick={() => revalidate()}
    >
      {t('presences.retry')}
    </Button>
  );
};

/**
 * Reloads the page: the server redirects to the login page, and back to this
 * URL once the user is logged in again.
 */
const ReconnectButton = () => {
  const { t } = useTranslation('presences');

  return (
    <Button
      leftIcon={<IconRafterLeft />}
      onClick={() => window.location.reload()}
    >
      {t('presences.reconnect')}
    </Button>
  );
};

const ACTIONS: Record<ErrorAction, () => JSX.Element> = {
  back: BackButton,
  retry: RetryButton,
  reconnect: ReconnectButton,
};

export const ErrorScreen = ({ kind }: { kind: ErrorKind }) => {
  const { t } = useTranslation('presences');
  const { key, image, action } = SCREENS[kind];
  const Action = ACTIONS[action];

  return (
    <div className="d-flex flex-column gap-16 align-items-center mt-64">
      <EmptyScreen
        imageSrc={image}
        imageAlt=""
        title={t(`${key}.title`)}
        text={t(`${key}.text`)}
      />
      <Action />
    </div>
  );
};
