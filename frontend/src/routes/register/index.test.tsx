import i18n from 'i18next';
import { HttpResponse, http } from 'msw';
import { RouteObject } from 'react-router-dom';
import {
  MOCK_REGISTER_AFTERNOON_ID,
  MOCK_REGISTER_MORNING_ID,
  mockCourses,
} from '~/mocks/datas/courses';
import { renderWithRoutes } from '~/mocks/renderWithRoutes';
import { server } from '~/mocks/server';
import { act, fireEvent, screen, within } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { baseUrl } from '~/services/api';
import { RouteError } from '../errors/route-error';
import { Register, loader } from '.';

const routes: RouteObject[] = [
  {
    path: '/registers/:registerId',
    loader: loader(queryClient),
    Component: Register,
    errorElement: <RouteError notFound="register" />,
  },
];

/** The photo of a student row: decorative, so without an accessible name. */
const photoOf = (row: HTMLElement) =>
  row.querySelector('.avatar img') as HTMLImageElement;

const openRegister = (registerId: number) =>
  renderWithRoutes(routes, [`/registers/${registerId}`]);

beforeAll(() => {
  i18n.addResources('fr', 'presences', {
    'presences.register': 'Appels',
    'presences.register.groups.title': 'Appels des [[groups]]',
    'presences.register.period.morning': 'Matinée du [[date]]',
    'presences.register.period.afternoon': 'Après-midi du [[date]]',
    'presences.register.last.course.absent': 'Absent au cours précédent',
    'presences.register.validation': 'Valider appel',
  });
});

afterAll(() => i18n.removeResourceBundle('fr', 'presences'));

afterEach(() => queryClient.clear());

describe('register page', () => {
  it('shows the classes and groups of the course in the title', async () => {
    openRegister(MOCK_REGISTER_MORNING_ID);

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Appels des CE1 A',
      }),
    ).toBeInTheDocument();
  });

  it('falls back to a generic title when the course is not in the day', async () => {
    server.use(http.get(`${baseUrl}/courses`, () => HttpResponse.json([])));

    openRegister(MOCK_REGISTER_MORNING_ID);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Appels' }),
    ).toBeInTheDocument();
  });

  it('falls back to a generic title when the courses cannot be read', async () => {
    server.use(
      http.get(
        `${baseUrl}/courses`,
        () => new HttpResponse(null, { status: 500 }),
      ),
    );

    openRegister(MOCK_REGISTER_MORNING_ID);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Appels' }),
    ).toBeInTheDocument();
  });

  it('falls back to a generic title when the course has no class nor group', async () => {
    server.use(
      http.get(`${baseUrl}/courses`, () =>
        HttpResponse.json(
          mockCourses.map((course) => ({ ...course, classes: [], groups: [] })),
        ),
      ),
    );

    openRegister(MOCK_REGISTER_MORNING_ID);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Appels' }),
    ).toBeInTheDocument();
  });

  it('labels a morning register', async () => {
    openRegister(MOCK_REGISTER_MORNING_ID);

    expect(
      await screen.findByText('Matinée du mardi 13 octobre 2026'),
    ).toBeInTheDocument();
  });

  it('labels an afternoon register', async () => {
    openRegister(MOCK_REGISTER_AFTERNOON_ID);

    expect(
      await screen.findByText('Après-midi du mardi 13 octobre 2026'),
    ).toBeInTheDocument();
  });

  it('lists the students in alphabetical order, each with a photo', async () => {
    openRegister(MOCK_REGISTER_MORNING_ID);

    const rows = await screen.findAllByTestId(/^presences-row-student-/);

    expect(rows.map((row) => row.textContent)).toStrictEqual([
      'ANDRÉ Lucas',
      'BERNARD Hugo',
      'FOURNIER Adam',
      'GIRARD Chloé',
      'LEFEBVRE Nathan',
      'MOREAU Jade',
      'PETIT Léa',
      'ROUX Inès',
    ]);
    expect(photoOf(rows[0])).toHaveAttribute(
      'src',
      '/userbook/avatar/student-4?thumbnail=100x100',
    );
    // Decorative: screen readers only read the name once
    expect(photoOf(rows[0])).toHaveAttribute('alt', '');
  });

  it('shows the default avatar when the photo cannot be loaded', async () => {
    openRegister(MOCK_REGISTER_MORNING_ID);

    const photo = photoOf(
      await screen.findByTestId('presences-row-student-student-4'),
    );
    fireEvent.error(photo);

    expect(photo).not.toHaveAttribute(
      'src',
      '/userbook/avatar/student-4?thumbnail=100x100',
    );
  });

  it('flags only the students absent from the previous course', async () => {
    openRegister(MOCK_REGISTER_MORNING_ID);

    const flags = await screen.findAllByRole('img', {
      name: 'Absent au cours précédent',
    });

    expect(flags).toHaveLength(1);
    expect(
      screen.getByTestId('presences-row-student-student-2'),
    ).toContainElement(flags[0]);
  });

  it('ends the list with the footer and its validation button', async () => {
    openRegister(MOCK_REGISTER_MORNING_ID);

    const footer = await screen.findByTestId('presences-footer-register');

    expect(
      within(footer).getByRole('button', { name: 'Valider appel' }),
    ).toBeInTheDocument();
  });

  it('shows the skeleton when the register is loaded again', async () => {
    openRegister(MOCK_REGISTER_MORNING_ID);
    await screen.findByRole('heading', { level: 1, name: 'Appels des CE1 A' });
    // Holds the second load until the skeleton is checked
    let releaseRegister = () => {};
    const registerHeld = new Promise<void>((resolve) => {
      releaseRegister = resolve;
    });
    server.use(
      http.get(`${baseUrl}/registers/:id`, async () => {
        await registerHeld;
      }),
    );

    // Back to a register with no data, like the "Trigger loading" of the devtools
    act(() => {
      queryClient.resetQueries({
        queryKey: ['register', MOCK_REGISTER_MORNING_ID],
      });
    });

    expect(
      await screen.findByRole('status', {
        name: 'presences.register.loading.text',
      }),
    ).toBeInTheDocument();
    releaseRegister();
    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Appels des CE1 A',
      }),
    ).toBeInTheDocument();
  });
});
