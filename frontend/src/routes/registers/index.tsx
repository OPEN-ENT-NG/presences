import { LoaderFunctionArgs, replace } from 'react-router-dom';
import { parseCourseContext } from '~/models/course-context';
import { HttpError } from '~/services/api';
import { openRegister } from '~/services/register-opening';

/**
 * Entry point of the AngularJS buttons: opens (and creates if needed) the
 * register of the course given in the URL, then goes to its page.
 * The redirection replaces this intermediate URL in the history, otherwise
 * the back button would come back here, and redirect again: a loop.
 */
export const loader = async ({ request }: LoaderFunctionArgs) => {
  const courseContext = parseCourseContext(new URL(request.url).searchParams);
  if (!courseContext) {
    throw new HttpError(400, 'Invalid course context');
  }

  const registerId = await openRegister(courseContext);
  return replace(`/registers/${registerId}`);
};
