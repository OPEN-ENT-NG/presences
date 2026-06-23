# API_CONTRACTS — Application Présences

Endpoints HTTP (contrôleurs Java `@Get/@Post/@Put/@Delete`) et eventbus, mappés aux services Angular
`$http`. Préfixe = nom du module au déploiement (`/presences`, `/incidents`, `/massmailing`,
`/statistics-presences`). `⚠️ À confirmer` = n° de ligne/droit indicatif.

---

# Module `presences` (cœur)

> Source service Angular = `public/ts/services/*.ts`. Liste des contrôleurs `controller/`.

## Registre — RegisterController
- `POST /registers` — créer un registre. ← `RegisterService.ts`.
- `GET /registers/:id` — détail.
- `PUT /registers/:id/status` — valider (TODO/IN_PROGRESS → DONE, verrou).
- `POST /structures/:id/registers/multiple` — création multiple.
- `GET /structures/:id/registers/forgotten` — appels oubliés.

## Registre légal — RegistryController
- `GET /registry`, `GET /registry/export` (ancien `RegistryCSVExport`), `GET /registry/export/new` (`RegistryBoardCSVExport`). Validation mois `[0-9]{4}-[0-9]{1,2}`. ← `RegistryService.ts`.

## Absences — AbsenceController
- `GET /absences` (filtres : structure, dates, student[], classes[], reason[], justified, regularized, followed, noReason, halfBoarder, internal, page).
- `GET /absence/:id`, `POST /absence`, `PUT /absence/:id`, `PUT /absence/reason`, `PUT /absence/regularized`, `PUT /absences/follow`, `DELETE /absence/:id`. ← `AbsenceService.ts`.

## Événements — events/EventController, events/LatenessEventController
- `GET /events` (filtres riches + pagination), exports. ← `EventService.ts`.
- `POST /events/:structureId/lateness`, `PUT /events/:id/lateness` (retards).

## Motifs — ReasonController
- `GET /reasons`, `POST/PUT/DELETE /reason` (schémas `reasonCreate.json`/`reasonUpdate.json`). ← `ReasonService.ts`.

## Exemptions — ExemptionController
- `GET /exemptions`, `GET /exemptions/export`, `POST /exemptions`, `PUT /exemption/:id`, `DELETE /exemption?id=`, `DELETE /exemption/recursive?id=` (schéma `exemption.json`).

## Présences positives — PresencesController
- `GET /presences/presences`, `GET /presences/presences/export`, `POST /presences/presence`, `PUT /presences/presence`, `DELETE /presences/presence?id=`. ← `PresenceService.ts`.

## Oublis de cahier — NotebookController
- `GET /forgotten/notebook`, `GET /forgotten/notebook/student/:id`, `POST /structures/:id/forgotten/notebook/students` (bulk), `POST/PUT/DELETE /forgotten/notebook[/:id]`. ← `ForgottenNotebookService.ts`.

## Absences collectives — CollectiveAbsenceController
- `GET/POST/PUT/DELETE /structures/:id/absences/collectives[/:id]`, `PUT .../:id/students`, `POST .../isAbsent`, `GET .../export` (schéma `collectiveAbsence.json`). ← `CollectiveAbsenceService.ts`.

## Alertes — AlertController
- `GET /structures/:id/alerts`, `GET .../alerts/summary`, `GET .../students/:sid/alerts`, `GET .../alerts/export`, `DELETE /structures/:id/alerts` (reset, `jsonschema/alertDelete.json`), `DELETE .../students/:sid/alerts/reset`. ← `AlertService.ts`.

## Calendrier — CalendarController
- `GET /calendar/courses` (agrège EDT + events + exemptions + incidents), `GET /calendar/groups/:id/students`. ← `CalendarService.ts`, `PeriodService.ts`.

## Recherche — SearchController
- `GET /search/users`, `/search/students`, `/search/groups`, `/search`.

## Déclarations parent — StatementAbsenceController
- `GET /statements/absences`, `GET .../export`, `POST /statements/absences`, `POST .../attachment` (multipart), `PUT .../:id/validate`, `GET .../:idStatement/attachment/:id`. ← `StatementsAbsencesService.ts`.

## Paramétrage — SettingsController, ActionController, DisciplineController, InitController
- `GET/PUT /structures/:id/settings`, `GET .../settings/multiple-slots` (schéma `settings.json`). ← `SettingsService.ts`.
- `GET /actions`, `POST/PUT/DELETE /action`. ← `ActionService.ts`.
- `GET /disciplines`, `POST/PUT/DELETE /discipline`. ← `DisciplineService.ts`.
- `GET/POST /initialization/structures/:id` (schéma `initialization.json`).
- `GET /grouping/structure/:id/list` (GroupingController).
- Autres : CourseController (`GET /courses`, `/courses/export`), StudentController, StatisticsController, ArchiveController, TaskController, ConfigController.

## EventBus EXPOSÉ — `@BusAddress("fr.openent.presences")` (EventBusController.java)
| Action | Handler | Ligne |
| ------ | ------- | ----- |
| `get-count-event-by-student` | EventService.getCountEventByStudent | :63 |
| `get-events-by-student` | EventService.getEventsByStudent | :79 |
| `get-absences` | AbsenceService.getAbsencesBetweenDates | :93 |
| `create-absences` | AbsenceService.create | :99 |
| `update-absence` / `delete-absence` | AbsenceService.update/delete | :115/:120 |
| `get-reasons` | ReasonService.fetchReason | :123 |
| `get-settings` / `update-settings` | SettingsService.retrieve/put | :131/:162 |
| `get-registers-with-groups` | RegisterService.listWithGroups | :135 |
| `init-presences` | InitService | :145 |

---

# Module `incidents` (préfixe `/incidents`)

## Incidents — IncidentsController.java
- `GET ""` (view, `@SecuredAction("view")`), `GET /incidents` (ReadIncidentRight), `GET /incidents/export` (ManageIncidentRight), `GET /incidents/parameter/types`, `POST /incidents` (`@SecuredAction(MANAGE_INCIDENT)`, jsonschema `incidents`), `PUT /incidents/:id`, `DELETE /incidents/:id`. ← `StudentIncident.ts`, `IncidentService.ts`.
- BusAddress `fr.openent.incident` : action `getUserIncident`.

## Punitions — PunishmentController.java
- `GET /punishments` (PunishmentsViewRight), `POST/PUT /punishments` (PunishmentsManageRight), `DELETE /punishments` (id | grouped_punishment_id), `GET /punishments/export`, `POST /punishments/students/absences` (jsonschema `punishmentAbsence`). ← `PunishmentService.ts`.

## Paramétrage (GET = ReadIncidentRight, POST/PUT/DELETE = ManageIncidentRight)
- Types incident, gravités (`seriousnessCreate/Update`), partenaires, lieux, types protagonistes, types punition (`type ∈ {PUNISHMENT,SANCTION}`), catégories punition (`GET /punishments/category` = ManageIncidentRight). Controllers `IncidentsTypeController`, `SeriousnessController`, `PartnerController`, `PlaceController`, `ProtagonistTypeController`, `PunishmentTypeController`, `PunishmentCategoryController`.

## Élèves / Présences
- `GET /students/:id/events` (StudentEventsViewRight), `POST /structures/:structureId/students/events` (StudentsEventsViewRight, jsonschema `studentsEvents`), `GET /structures/:structureId/reasons` (PresencesManage → Présences). `GET /config` (AdminFilter).

## EventBus exposé — `fr.openent.incidents` (EventBusController.java:35)
`get-incidents-users-range`, `init-get-incident-{type|places|protagonist-type|seriousness|partner}-statement`, `init-get-incident-punishment-type`, `get-count-punishment-by-student`, `get-punishment-by-student`, `update-punishments-massmailing`, `get-punishment-type`.

---

# Module `massmailing` (préfixe `/massmailing`)

| Méthode | Path | Java | Service TS |
| ------- | ---- | ---- | ---------- |
| GET | `` (view) | `MassmailingController.view:64` | — |
| GET | `/massmailings/status` | `getMassmailingsStatus:128` | `MassmailingService.getStatus` |
| GET | `/massmailings/anomalies` | `getMassmailingsAnomalies:286` | `MassmailingService.getAnomalies` |
| GET | `/massmailings/prefetch/:mailingType` | `prefetch:441` | `MassmailingService.prefetch` |
| POST | `/massmailings/:mailingType` | `postMassmailing:638` (BodyCanAccessMassMailing, `@Trace`) | `Massmailing.process` |
| GET | `/settings/templates/:type` | `getAllTemplates` | `SettingsService.get` |
| POST/PUT/DELETE | `/settings/templates[/:id]` | `create/update/deleteTemplate` (Manage) | `SettingsService.*` |
| GET | `/mailings`, `/mailings/:id/file/:id` | `MailingController:44,155` | `MailingService.get/downloadFile` |
| GET | `/config` | `ConfigController:15` (AdminFilter) | — |

EventBus consumer : `@BusAddress "fr.openent.massmailing"`, action `init-get-templates-statement`.

---

# Module `statistics-presences` (préfixe `/statistics-presences`, port 8066)

| Méthode | Route | Java | Service TS |
| ------- | ----- | ---- | ---------- |
| GET | `` (vue) | `StatisticsController:54-75` | — |
| POST | `/structures/:structure/indicators/:indicator` | `fetch:81-108` | `indicator.service.ts.fetchIndicator` |
| POST | `/structures/:structure/indicators/:indicator/graph` | `fetchGraph:110-134` | `fetchGraphIndicator` |
| GET | `/structures/:structure/indicators/:indicator/export` | `export:136-175` | `window.open` (`Indicator.ts:200-222`) |
| POST | `/process/statistics/tasks` | `:224-242` (AdminFilter) | — |
| POST | `structures/:structure/process/students/statistics/tasks` | `:244-263` | `refreshStudentsStats` |
| GET | `/user/queue/truncate` | `:265-272` (SuperAdmin) | — |
| POST | `/process/weekly/audiences/tasks` | `StatisticsWeeklyAudiencesController:25-43` | — |
| POST | `api/internal/process` | `TaskController:21-27` | — (interne) |

EventBus `fr.openent.statistics.presences` : `post-users`, `post-weekly-audiences`, `get-statistics-graph`, `get-statistics`, `get-statistics-indicator`.

---

# Clients EventBus inter-modules (module `common`)

| Client | Adresse | Actions principales |
| ------ | ------- | ------------------- |
| `Presences` | `fr.openent.presences` | get-count-event-by-student, get-events-by-student, get-absences, create-absences, update-absence, delete-absence, get-reasons, get-settings, get-registers-with-groups |
| `Incidents` | `fr.openent.incidents` | get-incidents-users-range, init-get-incident-*, get-punishment-by-student, get-count-punishment-by-student, update-punishments-massmailing |
| `Massmailing` | `fr.openent.massmailing` | init-get-templates-statement |
| `StatisticsPresences` | `fr.openent.statistics.presences` | post-users, get-statistics, get-statistics-graph, get-statistics-indicator, post-weekly-audiences |
| `Viescolaire` | `viescolaire` | periode.*, timeslot.*, classe.*, grouping.*, eleve.*, course.*, matiere.*, user.getActivesStructure (cf. ADHERENCES) |

**Source**: `common/.../{presences,incidents,massmailing,statistics_presences,viescolaire}/*.java`.
