import { useRouteError } from 'react-router-dom';
import { ErrorScreen } from '~/components/ErrorScreen/ErrorScreen';
import {
  NotFoundTarget,
  getErrorKind,
} from '~/components/ErrorScreen/getErrorKind';

/**
 * Error element of the register routes, shown inside the page layout: tells
 * the user what failed, depending on the status of the request.
 */
export const RouteError = ({ notFound }: { notFound: NotFoundTarget }) => {
  const error = useRouteError();
  console.error(error);

  return <ErrorScreen kind={getErrorKind(error, notFound)} />;
};
