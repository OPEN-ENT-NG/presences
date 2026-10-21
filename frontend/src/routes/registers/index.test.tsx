import i18n from 'i18next';
import { HttpResponse, http } from 'msw';
import { RouteObject } from 'react-router-dom';
import { mockCourses } from '~/mocks/datas/courses';
import { renderWithRoutes } from '~/mocks/renderWithRoutes';
import { server } from '~/mocks/server';
import { fireEvent, screen, waitFor } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { baseUrl } from '~/services/api';
import { RouteError } from '../errors/route-error';
import { Register, loader as registerLoader } from '../register';
import { loader } from '.';

const routes: RouteObject[] = [
  { path: '/previous', element: <div>previous page</div> },
  {
    path: '/',
    children: [
      {
        path: 'registers',
        loader,
        errorElement: <RouteError notFound="course" />,
      },
      {
        path: 'registers/:registerId',
        loader: registerLoader(queryClient),
        Component: Register,
        errorElement: <RouteError notFound="register" />,
      },
    ],
  },
];

const entry = (courseId: string) =>
  `/registers?courseId=${courseId}&structureId=structure-1&date=2026-10-13&startTime=08:30&endTime=11:45`;

/** The error screen shown for each kind of failure, found by its title. */
const errorTitle = (kind: string) =>
  screen.findByRole('heading', { name: `presences.error.${kind}.title` });

const registerTitle = () =>
  screen.findByRole('heading', { name: 'presences.register' });

beforeEach(() => {
  // The error elements log the error
  vi.spyOn(console, 'error').mockImplementation(() => {});
});

afterEach(() => queryClient.clear());

describe('registers entry point', () => {
  it('opens the existing register of the course', async () => {
    const { router } = renderWithRoutes(routes, [entry('course-afternoon')]);

    expect(await registerTitle()).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/registers/102');
  });

  it('displays the day and the time of the register', async () => {
    i18n.addResource(
      'fr',
      'presences',
      'presences.register.context',
      'Cours du [[date]] de [[startTime]] à [[endTime]]',
    );

    renderWithRoutes(routes, [entry('course-afternoon')]);

    expect(
      await screen.findByText('Cours du 2026-10-13 de 13:30 à 16:30'),
    ).toBeInTheDocument();
    i18n.removeResourceBundle('fr', 'presences');
  });

  it('creates the register when the course has none yet', async () => {
    const { router } = renderWithRoutes(routes, [entry('course-morning')]);

    expect(await registerTitle()).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/registers/101');
  });

  it('loads the register in the query cache', async () => {
    renderWithRoutes(routes, [entry('course-afternoon')]);

    await registerTitle();

    expect(queryClient.getQueryData(['register', 102])).toMatchObject({
      course_id: 'course-afternoon',
      state_id: 3,
    });
  });

  it('replaces the intermediate URL in the history', async () => {
    const { router } = renderWithRoutes(routes, [entry('course-morning')]);

    await registerTitle();

    expect(router.state.historyAction).toBe('REPLACE');
  });

  it('goes back to the calling page, without looping on the intermediate URL', async () => {
    const { router } = renderWithRoutes(routes, [
      '/previous',
      entry('course-morning'),
    ]);
    await registerTitle();

    await router.navigate(-1);

    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/previous'),
    );
    expect(screen.getByText('previous page')).toBeInTheDocument();
  });
});

describe('registers entry point errors', () => {
  it.each(['courseId', 'structureId', 'date', 'startTime', 'endTime'])(
    'shows the course not found screen when %s is missing from the URL',
    async (missing) => {
      const url = new URL(entry('course-morning'), 'http://localhost');
      url.searchParams.delete(missing);

      renderWithRoutes(routes, [`${url.pathname}${url.search}`]);

      expect(await errorTitle('course.notfound')).toBeInTheDocument();
    },
  );

  it('shows the course not found screen when the course is not in the day', async () => {
    renderWithRoutes(routes, [entry('unknown-course')]);

    expect(await errorTitle('course.notfound')).toBeInTheDocument();
  });

  it('shows the forbidden screen when the course does not allow a register', async () => {
    renderWithRoutes(routes, [entry('course-not-allowed')]);

    expect(await errorTitle('forbidden')).toBeInTheDocument();
  });

  it('shows the forbidden screen when the API refuses the courses', async () => {
    server.use(
      http.get(
        `${baseUrl}/courses`,
        () => new HttpResponse(null, { status: 403 }),
      ),
    );

    renderWithRoutes(routes, [entry('course-morning')]);

    expect(await errorTitle('forbidden')).toBeInTheDocument();
  });

  it('shows the forbidden screen, not the session one, when the ENT answers a 401 for a missing right', async () => {
    server.use(
      http.get(`${baseUrl}/courses`, () =>
        HttpResponse.html('<html>Erreur</html>', { status: 401 }),
      ),
    );

    renderWithRoutes(routes, [entry('course-morning')]);

    expect(await errorTitle('forbidden')).toBeInTheDocument();
  });

  it('shows the session expired screen when the API answers with the login page', async () => {
    server.use(
      http.get(`${baseUrl}/courses`, () =>
        HttpResponse.html('<html>login</html>'),
      ),
    );

    renderWithRoutes(routes, [entry('course-morning')]);

    expect(await errorTitle('session')).toBeInTheDocument();
  });

  it('reloads the page to log in again from the session expired screen', async () => {
    server.use(
      http.get(`${baseUrl}/courses`, () =>
        HttpResponse.html('<html>login</html>'),
      ),
    );
    const reload = vi.fn();
    vi.stubGlobal('location', { ...window.location, reload });
    renderWithRoutes(routes, [entry('course-morning')]);

    fireEvent.click(await screen.findByTestId('presences-button-reconnect'));

    expect(reload).toHaveBeenCalledOnce();
    vi.unstubAllGlobals();
  });

  it('shows the server error screen when the API fails to give the courses', async () => {
    server.use(
      http.get(
        `${baseUrl}/courses`,
        () => new HttpResponse(null, { status: 500 }),
      ),
    );

    renderWithRoutes(routes, [entry('course-morning')]);

    expect(await errorTitle('server')).toBeInTheDocument();
  });

  it('shows the server error screen when the creation fails', async () => {
    server.use(
      http.post(
        `${baseUrl}/registers`,
        () => new HttpResponse(null, { status: 500 }),
      ),
    );

    renderWithRoutes(routes, [entry('course-morning')]);

    expect(await errorTitle('server')).toBeInTheDocument();
  });

  it('shows the offline screen when the network is down', async () => {
    server.use(http.get(`${baseUrl}/courses`, () => HttpResponse.error()));

    renderWithRoutes(routes, [entry('course-morning')]);

    expect(await errorTitle('offline')).toBeInTheDocument();
  });

  it('shows the forbidden screen when a loader throws a response with a 403', async () => {
    renderWithRoutes(
      [
        {
          path: '/registers',
          loader: () => {
            throw new Response('Forbidden', { status: 403 });
          },
          errorElement: <RouteError notFound="course" />,
        },
      ],
      ['/registers'],
    );

    expect(await errorTitle('forbidden')).toBeInTheDocument();
  });

  it('lets the user go back from a not found screen, with nothing to retry', async () => {
    renderWithRoutes(routes, [entry('unknown-course')]);

    expect(
      await screen.findByTestId('presences-button-back'),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId('presences-button-retry'),
    ).not.toBeInTheDocument();
  });

  it('lets the user retry from the server error screen, instead of going back', async () => {
    server.use(
      http.get(
        `${baseUrl}/courses`,
        () => new HttpResponse(null, { status: 500 }),
      ),
    );

    renderWithRoutes(routes, [entry('course-morning')]);

    expect(
      await screen.findByTestId('presences-button-retry'),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId('presences-button-back'),
    ).not.toBeInTheDocument();
  });

  it('opens the register when the server answers at the second try', async () => {
    let calls = 0;
    server.use(
      http.get(`${baseUrl}/courses`, () => {
        calls++;
        return calls === 1
          ? new HttpResponse(null, { status: 500 })
          : HttpResponse.json(mockCourses);
      }),
    );
    const { router } = renderWithRoutes(routes, [entry('course-morning')]);
    fireEvent.click(await screen.findByTestId('presences-button-retry'));

    expect(await registerTitle()).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/registers/101');
  });

  it('keeps the error screen when the server still fails at the second try', async () => {
    server.use(
      http.get(
        `${baseUrl}/courses`,
        () => new HttpResponse(null, { status: 500 }),
      ),
    );
    renderWithRoutes(routes, [entry('course-morning')]);
    fireEvent.click(await screen.findByTestId('presences-button-retry'));

    expect(await errorTitle('server')).toBeInTheDocument();
    expect(await screen.findByTestId('presences-button-retry')).toBeEnabled();
  });

  it('lets the user retry from the offline screen', async () => {
    server.use(http.get(`${baseUrl}/courses`, () => HttpResponse.error()));

    renderWithRoutes(routes, [entry('course-morning')]);

    expect(
      await screen.findByTestId('presences-button-retry'),
    ).toBeInTheDocument();
  });
});

describe('register page errors', () => {
  it('shows the register not found screen when the register does not exist', async () => {
    renderWithRoutes(routes, ['/registers/999']);

    expect(await errorTitle('register.notfound')).toBeInTheDocument();
  });

  it('shows the register not found screen when the identifier is not a number', async () => {
    renderWithRoutes(routes, ['/registers/abc']);

    expect(await errorTitle('register.notfound')).toBeInTheDocument();
  });

  it('shows the forbidden screen when the API refuses the register', async () => {
    server.use(
      http.get(
        `${baseUrl}/registers/:id`,
        () => new HttpResponse(null, { status: 403 }),
      ),
    );

    renderWithRoutes(routes, ['/registers/102']);

    expect(await errorTitle('forbidden')).toBeInTheDocument();
  });

  it('shows the server error screen when the API fails to give the register', async () => {
    server.use(
      http.get(
        `${baseUrl}/registers/:id`,
        () => new HttpResponse(null, { status: 500 }),
      ),
    );

    renderWithRoutes(routes, ['/registers/102']);

    expect(await errorTitle('server')).toBeInTheDocument();
  });
});
