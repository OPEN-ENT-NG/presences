# Présences — bundle multi-modules (Maven + fronts AngularJS)

Module « vie scolaire » d'Edifice : gestion des absences et des appels, des présences, des incidents / punitions / sanctions, et publipostage. Backend **Java/Vert.x (ent-core)** ; fronts **AngularJS legacy** (webpack + gulp + yarn). Une application **React** de prise d'appel (epic Jira ORGA-493, spec `FS-01`) est en cours de création dans ce même dépôt : elle n'existe pas encore.

## Architecture

Maven multi-projet : `pom.xml` racine (parent `io.edifice:app-parent`, repo `OPEN-ENT-NG/presences`) + un `pom.xml` par module. Chaque module est un module Vert.x avec son `mod.json`.

| Module | Rôle | Port (`mod.json`) |
| --- | --- | --- |
| `common` | Code partagé (Java + TS `@common`) | — |
| `presences` | Absences, appels, présences | 8062 |
| `incidents` | Incidents, punitions, sanctions | 8063 |
| `massmailing` | Publipostage | 8065 |
| `statistics-presences` | Statistiques | 8066 |

- Java : `<module>/src/main/java/fr/openent/<module>/` (controller, service, model, cron, worker, event, security…). SQL dans `src/main/resources/sql`, i18n dans `src/main/resources/i18n`, vues dans `view` / `view-src`.
- Front AngularJS : `<module>/src/main/resources/public/ts/` (+ `sass/` pour `presences`). Alias TS : `@common`, `@presences`, `@incidents`, `@massmailing`, `@statistics`.
- `presences/specs/` : spécifications fonctionnelles (ex. `FS-ORGA-448-prise-appel-1d.md`).
- `build.sh` mélange des cibles Maven et des cibles Gradle historiques (`*:buildGradle`) ; la chaîne actuelle est **Maven** (cf. `pom.xml`).

## Commandes

Gestionnaire front : **yarn** (`yarn.lock`). Le build « officiel » passe par Docker via `build.sh`.

| But | Commande |
| --- | --- |
| Dev local d'un module (watcher + proxy recette) | `yarn dev:presences` · `dev:incidents` · `dev:massmailing` · `dev:statistics` (http://localhost:3000/presences) |
| Tests front (Jest + couverture) | `yarn test` (= `yarn coverage`) ; en watch : `yarn test:dev` |
| Build Sass | `yarn build:sass` |
| Build d'un module (front + Maven) | `./build.sh presences` (idem `incidents`, `massmailing`, `statistics`) |
| Build Maven seul | `./build.sh buildMaven` ou `./build.sh presences:buildMaven` |
| Build front seul | `./build.sh buildGulp buildCss` |
| Tests | `./build.sh test` (front + Maven) · `testNode` · `testMaven` |
| Install local | `./build.sh install` (Maven, `-DskipTests`) |

Dev local : voir le `README.md` (`cp .env.template .env`, `dev-auth-fetcher connect` ou skill `auth-user-frontend`, puis `yarn dev:<module>`). Seul le module lancé est servi depuis le disque ; le reste vient de la recette.

## Travailler dans UN module

1. Identifier le module concerné et rester **circonscrit** à lui (le code partagé va dans `common`).
2. Builder le module ciblé (`./build.sh <module>`) plutôt que tout le bundle.
3. Les modifications de `view-src/` ou de l'i18n ne sont pas visibles en dev local (proxy recette).

## Conventions

- Branche courante de travail : `develop-orga` ; branche principale pour les PR : `dev`.
- Commits : Conventional Commits avec la clé Jira (`#ORGA-XXX`) ; branches `<type>-<ORGA-XXX>-<slug>` (skills `shared-conventions`).
- Le CSS compilé (`*.css`, `dist/`, `public/js/`) n'est pas versionné : c'est un artefact de build.
- Droits d'accès : `rights.ts` côté front, `security/` côté Java ; ne pas contourner.

## À faire / à éviter

- ✅ Maintenance du front AngularJS sans le moderniser, sauf demande explicite.
- ✅ Retrouver dans le code (controller / service / SQL) ce que le back sait faire avant de supposer une capacité.
- ❌ Ne pas unifier les chaînes de build front (webpack legacy vs future app Vite/React).
- ❌ Ne pas modifier l'API Présences pour la refonte React de la prise d'appel : le périmètre de l'epic ORGA-493 est front uniquement.
- ❌ Ne pas présumer qu'un module ressemble à un autre : les fronts et configs diffèrent.
