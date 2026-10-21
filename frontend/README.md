# Présences — frontend React

Application React 18 + TypeScript + Vite de Présences (prise d'appel), générée depuis
`edifice-react-boilerplate`. C'est le front unique et centralisé de tout le bundle : les pages des
modules (`presences`, `incidents`, `massmailing`, `statistics-presences`) y seront migrées
progressivement, à la place des fronts AngularJS de chaque module. Le build est servi par le module
`presences`. Elle est indépendante de l'application AngularJS du module : elle est
servie sous `/presences/app/*` (routing réel, sans hash) et ses assets sous
`/presences/public/app/`.

Gestionnaire de paquets : **pnpm** (imposé par `preinstall`). Le reste du dépôt reste en **yarn**.

## Build intégré au module

Depuis la racine du dépôt, `./build.sh buildReact` (appelé aussi par `buildNode` et `presences`)
lance `frontend/build.sh clean init build` (image `node:24-alpine-pnpm`), copie `dist/` dans
`presences/src/main/resources/` (assets dans `public/app/`) et déplace `index.html` dans `view/`.

## Prérequis

- Node `>=20.19` ou `>=22.12` (voir `engines` dans `package.json`) — l'image de build est en Node 24
- pnpm `10.34.5` (voir `packageManager` dans `package.json`)

## Démarrage

```bash
pnpm install   # installe les dépendances ET les hooks git (script `prepare`)
pnpm dev       # serveur de dev Vite
```

## Scripts utiles

| Script          | Rôle                                     |
| --------------- | ---------------------------------------- |
| `pnpm dev`      | Serveur de dev Vite                      |
| `pnpm dev:mock` | Serveur de dev en mode mock (hors ligne) |
| `pnpm build`    | Build de production                      |
| `pnpm lint`     | ESLint sur tout le projet                |
| `pnpm format`   | Prettier `--write` sur tout le projet    |
| `pnpm test`     | Tests Vitest                             |

## Serveur de dev : proxy & i18n local

La config Vite (`vite.config.ts`) s'appuie sur deux plugins maison (`vite/plugins/`) :

- **`createDevProxyConfig`** (`devProxy.ts`) — construit le proxy de dev vers la recette à partir
  d'une simple liste de `routes`. Les identifiants de session sont lus depuis un fichier `.env`
  (`VITE_RECETTE`, `VITE_ONE_SESSION_ID`, `VITE_XSRF_TOKEN`). Sans `.env`, les requêtes ciblent
  `http://localhost:8090` (backend local).
- **`serveLocalI18n`** (`serveLocalI18n.ts`) — voir ci-dessous.

### Travailler sur les traductions sans backend (`serveLocalI18n`)

Par défaut, l'app charge ses traductions depuis la recette (le namespace `presences` est servi sur
`/presences/i18n`, proxifié). Le plugin `serveLocalI18n` permet d'**intercepter cette route en
dev** et de servir un fichier `fr.json` **local** à la place, pour itérer sur les libellés sans
backend qui tourne :

```ts
// vite.config.ts
serveLocalI18n({
  route: '/presences/i18n',
  filePath: resolve(import.meta.dirname, '../presences/src/main/resources/i18n/fr.json'),
}),
```

- Si le fichier `filePath` **existe**, il est servi tel quel sur `route` (priorité sur le proxy).
- S'il **n'existe pas** (fichier absent), le plugin laisse passer la requête
  (`next()`) : elle repart vers le proxy comme avant. Aucun réglage à faire.

Pour activer l'i18n local : déposer un `fr.json` au chemin pointé par `filePath` (ou adapter ce
chemin). Le plugin n'agit qu'en dev (`apply: 'serve'`).

## Mode mock (hors ligne)

`pnpm dev:mock` (`VITE_MOCK=true`) démarre le serveur de dev avec [MSW](https://mswjs.io/) dans le
navigateur : l'API Présences et les routes de session de l'ENT sont mockées, sans recette ni
backend. Les traductions restent servies par `serveLocalI18n`, comme en `pnpm dev`.

- Handlers : `src/mocks/handlers/` ; données : `src/mocks/datas/` (partagés avec les tests Vitest).
- Les réponses de l'API Présences sont retardées de 500 ms pour voir les états de chargement.
- Le worker MSW (`public/mockServiceWorker.js`, généré par `msw init`) n'est servi qu'en dev :
  `public/` n'est pas copié dans le build (`copyPublicDir: false`).

Au démarrage, le terminal affiche les URL qui fonctionnent avec les données mockées et le
navigateur s'ouvre sur la première. Elles reproduisent ce que transmettent les boutons de
l'AngularJS (le contexte du cours dans l'URL) :

| Cas                                      | `courseId`           |
| ---------------------------------------- | -------------------- |
| Matin, prise d'appel créée à l'ouverture | `course-morning`     |
| Après-midi, prise d'appel déjà validée   | `course-afternoon`   |
| Matin, classe sans élève                 | `course-empty-class` |

Exemple : `http://localhost:4200/registers?courseId=course-morning&structureId=structure-1&date=2026-10-13&startTime=08:30&endTime=11:45`.
Les URL sont définies dans `src/mocks/datas/entry-points.ts`, avec le jeu de données (structure
`structure-1`, mardi `2026-10-13`, classe CE1 A).

Le terminal liste aussi des URL qui déclenchent chaque écran d'erreur (cours introuvable, cours
sans prise d'appel autorisée, accès refusé, session expirée, erreur serveur, hors ligne, contexte d'URL invalide,
prise d'appel introuvable). Les structures qui échouent sont `structure-forbidden`,
`structure-session-expired`, `structure-server-error` et `structure-offline`.

Les tests Vitest ne chargent aucune traduction (`src/mocks/i18n.ts`) : ils vérifient les clés i18n.

## Hooks git (Husky + lint-staged)

Les hooks sont gérés par [Husky](https://typicode.github.io/husky/) (v9, format courant : pas de
`husky.sh` ni de shebang) et [lint-staged](https://github.com/lint-staged/lint-staged).

Le projet npm vit dans `frontend/` alors que la racine git est au-dessus. Les hooks sont donc
stockés dans `frontend/.husky/` et installés par le script `prepare` :

```jsonc
"prepare": "cd .. && husky frontend/.husky || true"
```

`prepare` s'exécute automatiquement à chaque `pnpm install` : un **clone frais réinstalle donc les
hooks** sans étape manuelle. Le `|| true` évite de casser l'install dans un environnement sans git
(CI). Pour désactiver temporairement les hooks : `HUSKY=0 git commit ...`.

### Filtre sur le front React

Les hooks sont posés pour tout le dépôt mais **ne s'exécutent que si le front React est concerné** :
`pre-commit` si des fichiers de `frontend/` sont indexés, `pre-push` si les commits poussés
(par rapport à l'upstream, ou `origin/dev` au premier push) en modifient. Un commit AngularJS ou Java
n'est donc pas ralenti.

### `pre-commit` — rapide, ciblé sur le _staged_

```sh
cd frontend && pnpm run pre-commit   # = lint-staged
```

`lint-staged` n'agit que sur les fichiers **indexés (staged)**, pas sur tout le repo :

```jsonc
"lint-staged": {
  "**/*.{js,ts,tsx}": "eslint --no-warn-ignored",   // bloque le commit si erreur de lint
  "**/*": "prettier --write --ignore-unknown"        // formate et ré-indexe les fichiers
}
```

> ESLint tourne **sans `--fix`** : il vérifie et bloque, mais ne modifie pas le code. Prettier, lui,
> formate (`--write`) et lint-staged ré-indexe automatiquement les fichiers corrigés.

### `pre-push` — build complet

```sh
cd frontend && pnpm run pre-push   # = pnpm run build
```

Le build (lent) est volontairement **sorti du pre-commit** pour garder les commits rapides ; il est
déclenché une seule fois, au push.

### Tests

Les tests **ne tournent pas dans les hooks** : ils sont assurés par la CI. Lance-les à la main avec
`pnpm test`.
