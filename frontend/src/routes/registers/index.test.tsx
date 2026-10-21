import i18n from 'i18next';
import { HttpResponse, http } from 'msw';
import { RouteObject } from 'react-router-dom';
import { queryClient } from '~/providers';
import { mockCourses } from '~/mocks/datas/courses';
import { renderWithRoutes } from '~/mocks/renderWithRoutes';
import { screen, waitFor } from '~/mocks/setup';
import { server } from '~/mocks/server';
import { baseUrl } from '~/services/api';
import { loader as registerLoader, Register } from '../register';
import { loader } from '.';

const routes: RouteObject[] = [
  { path: '/previous', element: <div>previous page</div> },
  {
    path: '/',
    errorElement: <div>error page</div>,
    children: [
      { path: 'registers', loader },
      {
        path: 'registers/:registerId',
        loader: registerLoader(queryClient),
        Component: Register,
      },
    ],
  },
];

const entry = (courseId: string) =>
  `/registers?courseId=${courseId}&structureId=structure-1&date=2026-10-13&startTime=08:30&endTime=11:45`;

const routeError = (errors: Record<string, unknown> | null) =>
  Object.values(errors ?? {})[0];

afterEach(() => queryClient.clear());

describe('registers entry point', () => {
  it('opens the existing register of the course', async () => {
    const { router } = renderWithRoutes(routes, [entry('course-afternoon')]);

    expect(
      await screen.findByRole('heading', { name: 'presences.register' }),
    ).toBeInTheDocument();
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

    expect(
      await screen.findByRole('heading', { name: 'presences.register' }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/registers/101');
  });

  it('loads the register in the query cache', async () => {
    renderWithRoutes(routes, [entry('course-afternoon')]);

    await screen.findByRole('heading', { name: 'presences.register' });

    expect(queryClient.getQueryData(['register', 102])).toMatchObject({
      course_id: 'course-afternoon',
      state_id: 3,
    });
  });

  it('replaces the intermediate URL in the history', async () => {
    const { router } = renderWithRoutes(routes, [entry('course-morning')]);

    await screen.findByRole('heading', { name: 'presences.register' });

    expect(router.state.historyAction).toBe('REPLACE');
  });

  it('goes back to the calling page, without looping on the intermediate URL', async () => {
    const { router } = renderWithRoutes(routes, [
      '/previous',
      entry('course-morning'),
    ]);
    await screen.findByRole('heading', { name: 'presences.register' });

    await router.navigate(-1);

    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/previous'),
    );
    expect(screen.getByText('previous page')).toBeInTheDocument();
  });

  it.each(['courseId', 'structureId', 'date', 'startTime', 'endTime'])(
    'fails with a 400 when %s is missing from the URL',
    async (missing) => {
      const url = new URL(entry('course-morning'), 'http://localhost');
      url.searchParams.delete(missing);

      const { router } = renderWithRoutes(routes, [
        `${url.pathname}${url.search}`,
      ]);

      expect(await screen.findByText('error page')).toBeInTheDocument();
      expect(routeError(router.state.errors)).toMatchObject({
        name: 'HttpError',
        status: 400,
      });
    },
  );

  it('fails with a 404 when the course is not found', async () => {
    const { router } = renderWithRoutes(routes, [entry('unknown-course')]);

    expect(await screen.findByText('error page')).toBeInTheDocument();
    expect(routeError(router.state.errors)).toMatchObject({ status: 404 });
  });

  it('fails with a 403 when the course does not allow a register', async () => {
    server.use(
      http.get(`${baseUrl}/courses`, () =>
        HttpResponse.json(
          mockCourses.map((course) => ({ ...course, allowRegister: false })),
        ),
      ),
    );

    const { router } = renderWithRoutes(routes, [entry('course-morning')]);

    expect(await screen.findByText('error page')).toBeInTheDocument();
    expect(routeError(router.state.errors)).toMatchObject({ status: 403 });
  });

  it('fails with the status of the API when it refuses the courses', async () => {
    server.use(
      http.get(
        `${baseUrl}/courses`,
        () => new HttpResponse(null, { status: 403 }),
      ),
    );

    const { router } = renderWithRoutes(routes, [entry('course-morning')]);

    expect(await screen.findByText('error page')).toBeInTheDocument();
    expect(routeError(router.state.errors)).toMatchObject({ status: 403 });
  });
});

describe('register page', () => {
  it('fails with a 404 when the register does not exist', async () => {
    const { router } = renderWithRoutes(routes, ['/registers/999']);

    expect(await screen.findByText('error page')).toBeInTheDocument();
    expect(routeError(router.state.errors)).toMatchObject({ status: 404 });
  });

  it('fails with a 404 when the identifier is not a number', async () => {
    const { router } = renderWithRoutes(routes, ['/registers/abc']);

    expect(await screen.findByText('error page')).toBeInTheDocument();
    expect(routeError(router.state.errors)).toMatchObject({ status: 404 });
  });
});
