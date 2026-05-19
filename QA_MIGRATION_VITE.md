# Tests de non-régression — Migration Webpack → Vite (module `presences`)

A supprimé une fois que ENABLING-764 est en production

## Contexte

Le module **presences** a été migré de Webpack 3 + Gulp vers ViteJS 5 pour son système de build frontend.
Cette migration n'apporte **aucun changement fonctionnel** — seul le pipeline de compilation change.

Cependant, Webpack transpilait silencieusement certains patterns JavaScript (arrow functions → fonctions classiques)
que Vite ne transpile pas. Ces patterns ont été corrigés manuellement dans le code source.
Des régressions sont possibles si une correction a été manquée ou si un comportement implicite de Webpack
n'a pas été identifié.

---

## Ce qui a changé

| Élément | Avant | Après |
|---|---|---|
| Bundler | Webpack 3 + ts-loader (transpilation ES5) | Vite 5 + esbuild (pas de transpilation ES5) |
| Format du bundle | Webpack bundle | UMD |
| CSS | compilé séparément par gulp-sass | importé dans `app.ts`, compilé par Vite |
| Package manager | yarn | yarn (inchangé) |
| Fichiers de build | `webpack.config.js` | `vite.config.shared.ts`, `vite.config.application.ts`, `vite.config.behaviours.ts` |

---

## Risques identifiés

### 1. Services AngularJS non initialisés (risque principal)

Webpack transpilait silencieusement les arrow functions en fonctions classiques via `ts-loader` (target ES5).
Vite ne fait pas cette conversion. Or AngularJS instancie les services avec `new` — les arrow functions
ne peuvent pas être utilisées comme constructeurs et provoquent l'erreur suivante à l'exécution :

```
TypeError: Function.prototype.bind.apply(...) is not a constructor
```

**Correction appliquée** : toutes les arrow functions dans `ng.service()` et `ng.factory()` ont été
converties en fonctions classiques dans les fichiers suivants.

**Module `presences` (migré sur Vite) :**
- `presences/src/main/resources/public/ts/services/AbsenceService.ts`
- `presences/src/main/resources/public/ts/services/ActionService.ts`
- `presences/src/main/resources/public/ts/services/AlertService.ts`
- `presences/src/main/resources/public/ts/services/CalendarService.ts`
- `presences/src/main/resources/public/ts/services/CollectiveAbsenceService.ts`
- `presences/src/main/resources/public/ts/services/DisciplineService.ts`
- `presences/src/main/resources/public/ts/services/EventService.ts`
- `presences/src/main/resources/public/ts/services/ForgottenNotebookService.ts`
- `presences/src/main/resources/public/ts/services/PeriodService.ts`
- `presences/src/main/resources/public/ts/services/PresenceService.ts`
- `presences/src/main/resources/public/ts/services/ReasonService.ts`
- `presences/src/main/resources/public/ts/services/RegisterService.ts`
- `presences/src/main/resources/public/ts/services/RegistryService.ts`
- `presences/src/main/resources/public/ts/services/SettingsService.ts`
- `presences/src/main/resources/public/ts/services/StatementsAbsencesService.ts`
- `presences/src/main/resources/public/ts/services/initialization.service.ts`

**Module `common` (code bundlé dans presences via alias `@common`) :**
- `common/src/main/resources/ts/services/GroupService.ts`
- `common/src/main/resources/ts/services/SearchService.ts`
- `common/src/main/resources/ts/services/TimeslotClasseService.ts`
- `common/src/main/resources/ts/services/UserService.ts`
- `common/src/main/resources/ts/services/ViescolaireService.ts`
- `common/src/main/resources/ts/services/grouping.service.ts`

**Module `incidents` (code bundlé dans presences via alias `@incidents`) :**
- `incidents/src/main/resources/public/ts/services/IncidentService.ts`
- `incidents/src/main/resources/public/ts/services/IncidentTypeService.ts`
- `incidents/src/main/resources/public/ts/services/PartnerService.ts`
- `incidents/src/main/resources/public/ts/services/PlaceService.ts`
- `incidents/src/main/resources/public/ts/services/PresenceService.ts`
- `incidents/src/main/resources/public/ts/services/ProtagonistTypeService.ts`
- `incidents/src/main/resources/public/ts/services/PunishmentCategoryService.ts`
- `incidents/src/main/resources/public/ts/services/PunishmentService.ts`
- `incidents/src/main/resources/public/ts/services/PunishmentTypeService.ts`
- `incidents/src/main/resources/public/ts/services/SeriousnessService.ts`

**Module `massmailing` (code bundlé dans presences via alias `@massmailing`) :**
- `massmailing/src/main/resources/public/ts/services/MailingService.ts`
- `massmailing/src/main/resources/public/ts/services/MassmailingService.ts`
- `massmailing/src/main/resources/public/ts/services/SettingsService.ts`

**Module `statistics-presences` (code bundlé dans presences via alias `@statistics`) :**
- `statistics-presences/src/main/resources/public/ts/services/indicator.service.ts`

> **Important** : les modules `common`, `incidents`, `massmailing` et `statistics-presences` continuent
> d'être buildés par leur propre pipeline gulp/webpack. Les corrections apportées à leurs fichiers
> source n'affectent que le bundle Vite de `presences` — leurs propres builds ne sont pas impactés.

### 2. Ordre et spécificité CSS

L'ordre d'injection des styles peut différer légèrement entre Webpack et Vite.
Des règles CSS pourraient être écrasées différemment selon la priorité des sélecteurs.

### 3. Résolution des dépendances externes

Les librairies `entcore`, `moment`, `angular`, `jquery`, `underscore` sont marquées comme externes
(non bundlées). Si une fonctionnalité dépendait d'un comportement implicite du bundle Webpack,
elle pourrait ne plus fonctionner.

---

## Scénarios de tests recommandés

### Chargement de l'application

- [ ] L'application se charge sans erreur dans la console navigateur (vérifier l'onglet Console — aucune `TypeError`)
- [ ] Les styles visuels sont identiques à l'avant-migration (pas de régression CSS visible)
- [ ] Les icônes et polices s'affichent correctement

### Écrans à tester

- [ ] **Dashboard** — tableau de bord enseignant/admin
- [ ] **Dashboard élève** — vue élève / vue relative (parent)
- [ ] **Appel** (`registers`) — saisie d'un appel, liste des appels
- [ ] **Registre** (`registry`) — consultation du registre
- [ ] **Présences** — liste et saisie des présences
- [ ] **Événements** — liste des événements, filtres
- [ ] **Absences planifiées** (`planned-absences`)
- [ ] **Alertes** — liste et gestion des alertes
- [ ] **Absences collectives** (`collective-absences`) — création et liste
- [ ] **Exemptions** — création et liste
- [ ] **Calendrier** — affichage et navigation
- [ ] **Déclarations des parents** (`statements-absences`) — liste et traitement

### Widgets (tableau de bord)

- [ ] Widget absences
- [ ] Widget cours du jour (`day-course`)
- [ ] Widget registre latéral (`side-register`)

### Fonctionnalités transverses

- [ ] Les formulaires se soumettent correctement (création, modification)
- [ ] Les modales s'ouvrent et se ferment sans erreur
- [ ] Les filtres et recherches fonctionnent (élèves, groupes, dates)
- [ ] Les notifications / toasts s'affichent
- [ ] La pagination fonctionne
- [ ] L'export PDF/CSV fonctionne si applicable

### Modules cross-imports (code source modifié, build inchangé)

Ces modules ont eu des services corrigés pour le bundle Vite de presences.
Leurs propres builds (gulp) sont inchangés, mais il est recommandé de vérifier
leurs fonctionnalités principales pour écarter tout effet de bord inattendu.

- [ ] **Incidents** — saisie d'un incident, liste, punitions
- [ ] **Massmailing** — envoi d'un courrier, historique
- [ ] **Statistiques présences** — affichage des indicateurs, graphiques
