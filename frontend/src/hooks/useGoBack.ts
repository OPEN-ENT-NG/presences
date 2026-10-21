import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ANGULAR_APP_URL } from '~/config';

/**
 * Goes back in the browser history (the AngularJS page that opened the
 * register), or to the AngularJS application when there is no history, e.g.
 * when the page was opened in a new tab.
 */
export const useGoBack = () => {
  const navigate = useNavigate();

  return useCallback(() => {
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      window.location.assign(ANGULAR_APP_URL);
    }
  }, [navigate]);
};
