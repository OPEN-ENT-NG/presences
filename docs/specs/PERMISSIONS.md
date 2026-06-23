# PERMISSIONS — Application Présences

Droits issus des annotations Java (`@SecuredAction`, `@ResourceFilter`), des workflows
(`rights.ts`) et des conditions front. Rôles ENT : **Teacher** (enseignant), **Personnel**
(CPE/direction), **Relative** (parent), **Student** (élève). `⚠️ À confirmer` = à valider.

> Le mapping rôle ENT ↔ workflow est défini hors-repo (console d'admin ENT) ; ce document liste les
> **droits applicatifs** et la garde de chaque ressource, pas l'attribution finale par profil.

---

# Module `presences` (cœur)

## Workflows déclarés (`public/ts/rights.ts:2-41`)
| Clé front | Workflow (controller\|action) |
| --------- | ----------------------------- |
| access | `PresencesController\|view` |
| readRegister / createRegister | `RegisterController\|getRegister` / `postRegister` |
| readRegistry | `RegistryController\|getRegistry` |
| readEvent / readEventRestricted | `EventController\|getEvents` / `FakeRight\|readEventRestricted` |
| createEvent | `EventController\|postEvent` |
| manageCollectiveAbsences | `CollectiveAbsenceController\|manageCollectiveAbsences` |
| search / searchRestricted / searchStudents | `SearchController\|searchUsers` / `FakeRight\|searchRestricted` / `SearchController\|search` |
| export | `CourseController\|exportCourses` |
| notify | `CourseController\|notify` |
| readExemption[Restricted] / manageExemption[Restricted] | `ExemptionController\|getExemptions` / `createExemptions` (+ FakeRight restricted) |
| readPresences[Restricted] / createPresences | `FakeRight\|readPresence[Restricted]` / `PresencesController\|createPresence` |
| manageStatementAbsences[Restricted] | `StatementAbsenceController\|validate` (+ FakeRight restricted) |
| viewCalendar | `CalendarController\|getCalendarCourses` |
| manageForgottenNotebook | `NotebookController\|worflowManageForgottenNotebook` |
| initSettings1D / initSettings2D / initPopup / presences1D | `FakeRight\|...` |
| widget_* (8 widgets) | `FakeRight\|widget...` (alerts, forgotten_registers, statements, remarks, absences, day_courses, current_course, day_presences) |

## Ressources & filtres
| Ressource / action | Workflow | `@ResourceFilter` | Rôle type |
| ------------------ | -------- | ----------------- | --------- |
| Vue app | `presences.view` | — | Tous habilités |
| Créer registre | `register.create` | `CreateRegisterRight` | Enseignant |
| Lire/valider registre | `register.read` | `RegisterViewRight` (user ∈ structure) | Enseignant/CPE |
| Registre légal | `registry` | `RegistryRight` | CPE/Direction |
| Notifier | `notify` | — | Enseignant |
| Créer event/absence | `event.create` | `CreateEventRight` | Enseignant/CPE |
| Lire events | `event.read` / `.restricted` | filtres event | Enseignant (restreint = ses élèves) |
| Gérer (justif/régul/follow/delete) | `presences.manage` | `Manage` | CPE/Direction |
| Exemptions | `exemption.read[.restricted]` / `.manage[.restricted]` | `ExemptionReadRight` / `ManageExemptionRight` | Enseignant/CPE |
| Présences positives | `presence.read[.restricted]` / `.create` | `PresenceReadRight` / `ManagePresenceRight` | Enseignant/CPE |
| Oublis cahier | `manage.forgotten.notebook` | `ForgottenNotebookManageRight` | Enseignant/CPE |
| Absences collectives | `manage.collective.absences` | `ManageCollectiveAbsences` (user ∈ structure) | CPE/Direction |
| Alertes | (ALERT ⚠️) | `AlertFilter`, `AlertStudentNumber` | CPE/Direction |
| Calendrier | `viewCalendar` | `CalendarViewRight` | Tous habilités |
| Recherche | `search` / `searchStudents` | `SearchRight`, `SearchStudents` | Enseignant (restreint)/Tous |
| Déclaration : créer | `absence.statements.create` | `AbsenceStatementsCreateRight` (`childrenIds`) | Parent |
| Déclaration : voir | `absence.statements.view` | `AbsenceStatementsViewRight` (ses enfants) / MANAGE | Parent / CPE |
| Déclaration : valider | `manage.absence.statements[.restricted]` | — | CPE |
| Déclaration : PJ | — | `AbsenceStatementsGetFileRight` (MANAGE/VIEW) | CPE / Parent |
| Settings | (SETTINGS_GET / MANAGE) | `SettingFilter` | Admin/Gestionnaire |
| Init | `init.settings.1d` / `.2d` | en code | Admin |

**Notes**: les droits `.restricted` (`hasOnlyRestrictedRight(user, isTeacher)`) limitent un enseignant à
ses propres classes/élèves (filtre `teacherId`). Le couplage avec Vie Scolaire passe par
`viescolaire.search` (cf. ADHERENCES).

---

# Module `incidents`

## Workflows (`Incidents.java:21-28`, `enums/WorkflowActions.java:5-13`)
| Constante | Workflow |
| --------- | -------- |
| READ_INCIDENT | `incidents.incident.read` |
| MANAGE_INCIDENT | `incidents.incident.manage` |
| PUNISHMENT_CREATE / SANCTION_CREATE | `incidents.punishment.create` / `incidents.sanction.create` |
| PUNISHMENTS_VIEW / SANCTIONS_VIEW | `incidents.punishments.view` / `incidents.sanction.view` |
| STUDENT_EVENTS_VIEW | `presences.student.events.view` |
| MANAGE | `presences.manage` |

## ResourceFilters (`security/`)
| Filtre | Règle | Source |
| ------ | ----- | ------ |
| ReadIncidentRight | user ∈ structure ET READ_INCIDENT | `ReadIncidentRight.java:14-18` |
| ManageIncidentRight | MANAGE_INCIDENT | `ManageIncidentRight.java:13` |
| PunishmentsViewRight | PUNISHMENTS_VIEW OU SANCTIONS_VIEW | `PunishmentsViewRight.java:13-16` |
| PunishmentsManageRight | PUNISHMENT_CREATE OU SANCTION_CREATE | `PunishmentsManageRight.java:13-17` |
| StudentEventsViewRight | STUDENT_EVENTS_VIEW ET (user = élève OU enfant) | `StudentEventsViewRight.java:13-17` |
| StudentsEventsViewRight | STUDENT_EVENTS_VIEW ET studentIds ⊆ enfants | `StudentsEventsViewRight.java:19-26` |
| PresencesManage | MANAGE | `presence/PresencesManage.java:13` |
| AdminFilter | admin (entcore) sur `/config` | `ConfigController.java:15` |

**Visibilité restreinte** (BR-10) : sans (PUNISHMENTS_VIEW ET SANCTIONS_VIEW), l'utilisateur ne voit
que ses propres punitions ; un élève ne voit que les siennes. Audit `@Trace` sur toutes les mutations
(`constants/Actions.java:3-52`).

---

# Module `massmailing`

## Workflows (`Massmailing.java:33-35`)
- `massmailing.manage` (MANAGE), `massmailing.manage.restricted` (MANAGE_RESTRICTED), `massmailing.view` (VIEW). Couple `MANAGE = (MANAGE, MANAGE_RESTRICTED)`.

## Filtres
| Filtre | Règle | Source |
| ------ | ----- | ------ |
| CanAccessMassMailing | structure ∈ user ET (MANAGE ou MANAGE_RESTRICTED) | `CanAccessMassMailing.java:16-18` |
| BodyCanAccessMassMailing | idem, structure lue dans le body (POST envoi) | `BodyCanAccessMassMailing.java:17-21` |
| Manage | POST/PUT : MANAGE + structure ∈ user ; DELETE : template ∈ structures user | `Manage.java:18-49` |
| AdminFilter | `/config` | `ConfigController.java:15-21` |

Conditions front : onglet Home `manage || manageRestricted` ; Historique `access` ; chips
Punition/Sanction `hasIncidentRight('access')`. Audit `@Trace` (création/maj/suppression template, envoi).

---

# Module `statistics-presences`

## Workflows
- `statistics_presences.view` (VIEW), `statistics_presences.manage` (MANAGE), `statistics_presences.manage.restricted`, `statistics_presences.view.restricted`, `statistics_presences.1d`.

## Filtres
| Ressource | Filtre | Source |
| --------- | ------ | ------ |
| Vue | `@SecuredAction(VIEW)` | `StatisticsController.java:54-55` |
| fetch / fetchGraph / export | `UserInStructure` (user ∈ structure ET couple MANAGE) | `StatisticsController.java:81-138` ; `security/UserInStructure.java:16-18` |
| process global | `AdminFilter` | `StatisticsController.java:225-227` |
| process students | `UserInStructure` | `StatisticsController.java:245-247` |
| truncate file | `SuperAdminFilter` | `StatisticsController.java:266-268` |
| process weekly | `AdminFilter` | `StatisticsWeeklyAudiencesController.java:26-28` |

**Restriction enseignant** : `hasOnlyRestrictedRight` (vrai pour TEACHER) → filtres limités à ses
classes/élèves (`StatisticsController.java:177-222`). Affichage PUNISHMENT/SANCTION en Global gardé par
`statisticsPresences1d` (`public/ts/indicator/Global.ts:29`).

---

# Module `common` (sécurité partagée)
- `WorkflowHelper.hasRight(user, action)` — vérifie `user.getAuthorizedActions()` (`common/helper/WorkflowHelper.java:12-20`).
- `IWorkflowActionsCouple` — couple droit non-restreint / restreint, `hasRight(user, restrictedCondition)` (`common/security/IWorkflowActionsCouple.java:6-45`).
- `ExportRight` (`presences.export`), `SearchRight` (`presences.search`), `UserInStructure` (structure ∈ user) — `common/security/`.
