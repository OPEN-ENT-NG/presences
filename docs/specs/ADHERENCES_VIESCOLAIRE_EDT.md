# Adhérences — Présences ↔ Vie Scolaire & Emploi du temps (EDT)

Analyse des dépendances/intégrations entre l'app **Présences** (5 modules) et deux apps Edifice/ENT
connexes : **Vie Scolaire** (`viescolaire`) et **Emploi du temps / EDT** (`fr.cgi.edt`, l'app CGI de
gestion de l'emploi du temps). Sources `fichier:ligne`. `⚠️ À confirmer` = hors de ce repo.

## Constat structurant

- **Aucune dépendance Maven directe** vers EDT ou Vie Scolaire (`pom.xml:129-156` : uniquement
  `org.entcore`). **Tout le couplage est runtime**, par 3 mécanismes :
  1. **Vert.x event bus** (adresses `viescolaire`, `fr.cgi.edt`, `fr.openent.presences`…),
  2. **appels HTTP directs** (`/viescolaire/*`, `/directory/timetable/*`),
  3. **collection MongoDB partagée `courses`** (alimentée par l'import EDT, lue par Présences).
- **Vie Scolaire est l'adhérence dominante et bidirectionnelle** ; elle sert aussi de **façade** vers
  l'EDT (la quasi-totalité des données de cours transite par l'adresse `viescolaire`, pas par
  `fr.cgi.edt`).

---

## A. Adhérences Emploi du temps (EDT)

Dans cet écosystème, l'EDT (`fr.cgi.edt`) importe les cours (UDT/STSWeb) et les écrit dans la
collection Mongo `courses`. **Présences ne parle quasiment jamais directement à `fr.cgi.edt`** : il
consomme les cours via la façade `viescolaire` ou en lisant directement `courses`.

### A1. Lecture directe de la collection Mongo `courses` — *cœur métier*
- **Direction** : Présences → EDT (consomme la donnée importée).
- **Mécanisme** : lecture directe de la collection partagée `courses`.
- **Donnée** : cours/créneaux (startDate, endDate, structureId, classesIds, groupsIds, teacherIds, subjectId).
- **Usage métier** :
  - `DefaultCourseService.getCourse()` (`presences/.../service/impl/DefaultCourseService.java:47`).
  - `DefaultRegisterService.getCourseTeachers()` — profs d'un cours pour l'appel (`DefaultRegisterService.java:830`).
  - `DefaultRegistryService.buildDailyCourseSchedule()` — planning matin/après-midi pour l'export du registre, commentaire « from the Mongo timetable (courses) » (`DefaultRegistryService.java:460-484`).
  - comptage : `CalendarHelper.java:253`.

### A2. Récupération des cours via event bus `viescolaire` (façade)
- **Direction** : Présences → EDT (via façade Vie Scolaire).
- **Mécanisme** : `eb.request("viescolaire", {action:"course.getCoursesOccurences"...})`.
- **Donnée** : occurrences de cours sur une plage (structure, profs, groupes, dates, créneaux).
- **Usage métier** : **construction de la liste des appels/cours du jour et du calendrier** — point d'entrée principal.
  - `CourseHelper.getCourses(...)` → `course.getCoursesOccurences` (`CourseHelper.java:146,174`).
  - `CourseHelper.getCoursesByIds(...)` → `course.getCoursesByIds` (`CourseHelper.java:197-200`).
  - appelé par `CalendarController.getCalendarCourses()` (`/calendar/courses`, `CalendarController.java:59-95`).

### A3. Tags de cours — *seul appel direct à `fr.cgi.edt`*
- **Mécanisme** : `eb.request("fr.cgi.edt", {action:"get-course-tags"})`.
- **Usage** : `CourseHelper.getCourseTags(structureId)`.
- **Source** : `presences/.../helper/CourseHelper.java:218` — **unique occurrence de `fr.cgi.edt` dans tout le repo**.

### A4. Synchronisation des matières via `directory/timetable`
- **Mécanisme** : `http.get('/directory/timetable/subjects/{structureId}')` (frontend).
- **Donnée** : matières (subjectId, label, code, teacherId) issues de l'EDT importé.
- **Usage** : `Subjects.sync()` — liste des matières (appels, exemptions, détection EPS pour les dispenses).
- **Source** : `presences/.../public/ts/models/Subject.ts:48`.

### A5. Créneaux horaires (timeslots / slot profiles)
- **Mécanisme** : event bus `viescolaire`, actions `timeslot.*` (les créneaux EDT sont portés par la conf Vie Scolaire).
- **Usage** : positionner les appels sur la grille horaire, calculer durées, exports, demi-journées.
- **Source** : `common/.../viescolaire/Viescolaire.java:82-196` ; appelants `DefaultCourseService.java:175,243`, `DefaultEventService.java:1285,1474`, `DefaultLatenessEventService.java:222`, `CalendarController.java:136`, `IndicatorWorker.java:337,406`, `MassMailingProcessor.java:144`.

### A6. Indices i18n EDT
- `presences.init.1d.form.schoolyear.timetable` = « Emploi du temps » (`i18n/fr.json:353`).
- motifs pré-configurés « EDT aménagé » / « Erreur EDT » (`i18n/fr.json:522-523`).
- `presences.init.check.teachers.warning` : « les cours pour ces enseignants ne seront pas générés » (`i18n/fr.json:366`).

---

## B. Adhérences Vie Scolaire (`viescolaire`)

Adhérence la plus forte, **bidirectionnelle**, avec un client dédié
`common/.../common/viescolaire/Viescolaire.java` (adresse `"viescolaire"`, `:24` ; enum
`EventBusActions.VIESCOLAIRE_BUS_ADDRESS` `common/.../enums/EventBusActions.java:25`). Initialisé dans
chaque module (`Presences.java:160`, `Massmailing.java:73`, `StatisticsPresences.java:81`,
`Incidents.java:20`).

### B1. Actions event bus consommées (Présences → VS)
| Action | Donnée | Usage métier | Source |
| ------ | ------ | ------------ | ------ |
| `periode.getExclusionDays` | jours fériés/vacances | exclure du calcul d'assiduité & du registre | `Viescolaire.java:46` ; `DefaultRegistryService.java:392,440` ; `Global.java:239` |
| `periode.getSchoolYearPeriod` | dates année scolaire | bornes stats/alertes/archive | `Viescolaire.java:74` ; `DefaultAlertService.java:90` ; `IndicatorWorker.java:196` |
| `timeslot.getSlotProfileSettings`/`getSlotProfiles`/`getDefaultSlots`/`getAudienceTimeslot`/`getTimeslotFromStudentIds` | créneaux | grille horaire (cf. A5) | `Viescolaire.java:83-196` |
| `classe.getNbElevesGroupe` | effectifs groupes | stats audiences hebdo | `Viescolaire.java:158` ; `DefaultStatisticsWeeklyAudiencesService.java:95` |
| `grouping.getGroupingStructure` | regroupements de classes | `DefaultGroupingService.java:27` | `Viescolaire.java:179` |
| `eleve.getResponsables` | responsables légaux | notifications absence aux parents | `Viescolaire.java:202` ; `DefaultEventService.java:1729` |
| `matiere.getMatieres` / `matiere.getSubjectsAndTimetableSubjects` | matières (+ matières EDT) | libellés, fiche cours | `SubjectHelper.java:33` ; `DefaultCourseService.java:61-65` |
| `eleve.getInfoEleve` / `eleve.getPrimaryRelatives` | infos élève / parents prioritaires | fiche, ciblage mailing | `ExemptionHelper.java:89` ; `DefaultMassmailingService.java:223` |

### B2. Structures actives — `user.getActivesStructure`
- **Mécanisme** : `eb.request("viescolaire", {action:"user.getActivesStructure", module:"presences"})`.
- **Usage** : rendu de la vue principale (filtre des établissements où l'app est activée), activation à l'init.
- **Source** : `PresencesController.java:62`, `MassmailingController.java:73`, `StatisticsController.java:62`, `IncidentsController.java:63`.

### B3. Exemptions / dispenses — écriture (`action:"prepared"`)
- **Direction** : Présences → Vie Scolaire (écriture).
- **Usage** : `ExemptionHelper` propage les dispenses (`ExemptionHelper.java:93,137,351,438,455`).

### B4. Couplage de droits — `viescolaire.search`
- **Usage** : un utilisateur Vie Scolaire (CPE) peut faire des recherches élèves côté Présences.
- **Source** : `Presences.java:51-52` (`SEARCH_VIESCO = "viescolaire.search"` + `.restricted`), `WorkflowActions.java:12-13`, `SearchRight.java:13`.

### B5. Appels HTTP frontend directs `/viescolaire/*`
- Dates année scolaire (`ViescolaireService.ts:20`), profil de créneaux (`:26`), fiche élève (`:34`),
  périodes/trimestres, élèves d'un groupe d'enseignement, recherche élèves
  (`/viescolaire/user/search?...&profile=Student`), gestion créneaux par classe
  (`TimeslotClasseService.ts`), **initialisation de l'établissement**
  (`/viescolaire/structures/{id}/initialization` + `/initialize` — déclenche l'import EDT → cours),
  **photo élève** dans le registre (`/viescolaire/structures/{id}/students/{id}/picture`,
  `register/panel.html:5`).
- **Source** : `common/.../ts/services/ViescolaireService.ts`, `TimeslotClasseService.ts`, divers `*.ts/*.html`.

### B6. Sniplet `memento`
- La coquille de vue charge `<sniplet template="memento" application="viescolaire">` — la fiche élève
  (memento) est un composant **fourni par Vie Scolaire** embarqué dans Présences.
- **Source** : `presences/.../view-src/presences.html`.

### B7. Indices i18n VS
- motif « Rdv vie scolaire » (`i18n/fr.json:511`) ; notification d'appel non saisi signée « La vie
  scolaire de l'établissement » (`i18n/fr.json:573`).

---

## C. Sens inverse — ce que Présences EXPOSE (boucle d'absences)

Présences expose `@BusAddress("fr.openent.presences")` (`EventBusController.java`) avec, notamment,
`create-absences`, `update-absence`, `delete-absence`, `get-absences`, `get-events-by-student`,
`get-registers-with-groups`. Ces actions sont émises par le client commun
`common/.../common/presences/Presences.java` → **c'est typiquement Vie Scolaire qui crée/met à jour
des absences dans Présences** : la donnée absence circule donc **dans les deux sens** entre VS et
Présences. C'est l'adhérence la plus sensible à préserver lors d'un rebuild.

---

## D. Modules ENT communs & event bus (récap)

| Adresse event bus | Rôle | Volume |
| ----------------- | ---- | ------ |
| `viescolaire` | façade EDT + périodes/responsables/créneaux/structures + boucle absences | 17+ appels |
| `fr.cgi.edt` | tags de cours uniquement | 1 appel (A3) |
| `fr.openent.presences` | exposé par Présences (absences/events/registres/settings) | consommé par VS/massmailing/stats |
| `fr.openent.incidents` / `fr.openent.incident` | punitions/incidents | consommé par presences/stats/massmailing |
| `fr.openent.statistics.presences` | file stats | alimenté par incidents/presences |
| `org.entcore.workspace` | dépôt fichiers (exports, PJ) | `EventBusActions.java:24` |
| Neo4j (directory) | utilisateurs/groupes/classes/responsables | requêtes Cypher directes |
| Mongo `courses` | cours EDT (donnée partagée) | lue par Présences (A1) |

---

## E. Synthèse pour un rebuild React

1. **Le front est remplaçable, pas les contrats** : même reconstruit « from scratch », le nouveau
   front devra reconsommer les mêmes endpoints HTTP (`/viescolaire/*`, `/directory/timetable/*`) et
   le backend Présences continuera de dialoguer en eventbus avec `viescolaire`/`fr.cgi.edt`.
2. **Dépendances HTTP non typées** : les routes `/viescolaire/*` et `/directory/timetable/*` sont des
   contrats externes non versionnés dans ce repo — toute évolution de signature côté VS/EDT casse
   Présences **silencieusement**. À sécuriser/documenter dans le rebuild.
3. **La boucle d'absences VS ↔ Présences (C)** doit être préservée à l'identique côté backend.
4. **Le memento (B6) est un composant VS** : à remplacer ou ré-embarquer dans le nouveau front.

---

## F. Zones ⚠️ à confirmer (hors repo)

1. **Propriétaire de `courses`** : Présences lit (A1) mais n'écrit a priori pas ; confirmer que c'est l'import EDT qui peuple la collection.
2. **Façade `viescolaire` vs `fr.cgi.edt`** : confirmer si `viescolaire` relaie vers EDT (action `course.getCoursesOccurences`) ou détient sa propre copie des cours.
3. **`user.getActivesStructure` sur l'adresse `viescolaire`** (B2) : sémantiquement de l'activation d'app ; confirmer côté VS.
4. **`incidents`** : importe `Viescolaire` mais seul usage trouvé = `user.getActivesStructure` (`IncidentsController.java:63`) — adhérence VS minimale, à confirmer comme complète.

> **Conclusion** : la **Vie Scolaire** est l'adhérence dominante (façade EDT + données métier +
> boucle d'absences bidirectionnelle) ; l'**Emploi du temps (EDT, `fr.cgi.edt`)** n'est touché en
> direct qu'une fois (tags de cours), sa donnée principale (les cours) transitant par la façade
> `viescolaire` et la collection Mongo partagée `courses`.
