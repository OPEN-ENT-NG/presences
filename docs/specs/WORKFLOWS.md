# WORKFLOWS — Application Présences

Flux métier numérotés. `⚠️ À confirmer` = à valider sur fichier.

---

# Module `presences` (cœur)

## WF-01 — Création d'un registre
- **Déclencheur**: `POST /registers` (manuel) ou cron `CreateDailyRegistersTask` → `CreateDailyPresenceWorker`.
- **États**: (inexistant) → `TODO`.
- **Étapes**: 1. validation `jsonschema/register.json` → 2. vérif unicité `(course_id,start_date,end_date)` → 3. résolution groupes/élèves (Neo4j) → 4. transaction insert `register`(TODO) + `rel_teacher_register` + events absence auto → 5. trace `REGISTER_CREATION`.
- **Cas d'erreur**: registre déjà existant → renvoie l'ID existant.
- **Source**: `RegisterController.java`, `DefaultRegisterService.java`.

## WF-02 — Validation de l'appel
- **Déclencheur**: UI `validRegister()` → `PUT /registers/:id/status`.
- **États**: `TODO`/`IN_PROGRESS` → `DONE`.
- **Étapes**: `UPDATE register SET state_id=3 WHERE id=? AND state_id != 3` → event store `VALID_REGISTER`.
- **Cas d'erreur**: registre déjà `DONE` → 0 ligne (verrou).
- **Source**: `RegisterController.java`, `DefaultRegisterService` (⚠️ l.613).

## WF-03 — Notification d'un appel oublié
- **Déclencheur**: appel détecté oublié (`now > start + 15 min`, ≠ DONE).
- **Étapes**: bouton « notifier » (droit `notify`, délai < 2 jours) → `POST /courses/{id}/notify` → `register.notified=true` + envoi massmailing.
- **Source**: `public/ts/controllers/registers.ts` (⚠️).

## WF-04 — Création d'absence + justification + régularisation
- **Déclencheur**: `POST /absence`.
- **Étapes**: 1. `checkPresenceEvent()` (motif -2 si présent même créneau) → 2. suppression anciennes absences chevauchantes → 3. insert → 4. trigger `regularize_absences()` (proving → régularisé) → 5. sync `event` si `editEvents=true`.
- **Variantes**: justif `PUT /absence/reason` ; régul manuelle `PUT /absence/regularized` ; suivi `PUT /absences/follow`.
- **Source**: `DefaultAbsenceService.java`, `sql/013-MA-403`.

## WF-05 — Exemption / dispense
- **Déclencheur**: `POST /exemptions` (ponctuelle ou récursive selon `is_recursive`).
- **Étapes**: restriction enseignant sur ses élèves → CRUD (`PUT /exemption/:id`, `DELETE /exemption?id=`, `DELETE /exemption/recursive`).
- **Source**: `ExemptionController.java`, `sql/027-MA-412`.

## WF-06 — Oubli de cahier
- **Déclencheur**: `POST /forgotten/notebook`.
- **Étapes**: insert (UNIQUE date/élève/jour) → trigger +1 alerte ; `DELETE` → trigger -1.
- **Source**: `sql/011-MA-384`.

## WF-07 — Absence collective
- **Déclencheur**: `POST .../absences/collectives`.
- **Étapes**: insert `collective_absence` + relations audiences → une absence/élève des audiences ; retrait sélectif d'élèves ; suppression cascade.
- **Source**: `sql/038-MA-289`, `CollectiveAbsenceController.java`.

## WF-08 — Alertes (génération / reset / recalcul)
- **Déclencheur**: INSERT event/notebook (trigger increment, avec exclusions) ; DELETE (decrement + historisation) ; UPDATE reason/régul (recalcul).
- **Reset CPE**: `DELETE /structures/:id/alerts` (`jsonschema/alertDelete.json`).
- **Source**: `sql/046-MA-968` à `050`, `AlertController.java`.

## WF-09 — Calendrier / fiche élève
- **Déclencheur**: `GET /calendar/courses`.
- **Étapes**: agrège cours EDT (eventbus `viescolaire`) + events + exemptions + incidents/punitions (eventbus incidents) + badges alertes.
- **Source**: `CalendarController.java`.

## WF-10 — Déclaration d'absence parent
- **Déclencheur**: `POST /statements/absences[/attachment]` (multipart si PJ, vérif `childrenIds`).
- **États**: déclarée → (CPE) traitée.
- **Étapes**: 1. parent déclare → 2. CPE liste (`GET /statements/absences`) → 3. validation `PUT .../:id/validate` (`treated_at`, `validator_id`) → 4. export CSV / téléchargement PJ.
- **Cas d'erreur**: parent sur un élève non rattaché → 401.
- **Note**: ⚠️ conversion automatique statement → absence non confirmée.
- **Source**: `StatementAbsenceController.java` (⚠️).

## WF-11 — Paramétrage / Initialisation
- **Déclencheur**: `GET/PUT /structures/:id/settings` ; `POST /initialization/structures/:id` (`ONE_D`/`TWO_D`).
- **Étapes**: CRUD actions/disciplines/motifs ; init crée settings/motifs/actions/disciplines par défaut.
- **Source**: `SettingsController.java`, `InitController.java`.

## Routing front (AngularJS) — pas de redirection React
`public/ts/app.ts` (`$routeProvider`), chaque route gardée par `model.me.hasWorkflow(...)` :

| Route | Action / écran | Garde (workflow) |
| ----- | -------------- | ---------------- |
| `/dashboard` | accueil widgets | (défaut) |
| `/events` | événements | — |
| `/planned-absences` | absences prévisionnelles | route front, **pas de back Java** ⚠️ |
| `/collective-absences` | absences collectives | — |
| `/alerts` | alertes | `widget_alerts` |
| `/registers` `/registers/:id` | appel | `readRegister` |
| `/registry` | registre légal | `readRegistry` / `search` |
| `/calendar/:studentId` | calendrier élève | `viewCalendar` |
| `/exemptions` | exemptions | `readExemption` |
| `/presences` | présences positives | `readPresences[Restricted]` |
| `/statements-absences` | déclarations parent | `manageStatementAbsences[Restricted]` |
| `otherwise` | → `/dashboard` | — |

**Source**: `presences/src/main/resources/public/ts/app.ts:30-99`.

> **Note rétrocompat** : `view-src/` ne contient **pas** de table de redirection legacy→React.
> Il héberge la coquille de vue AngularJS (`presences.html`, charge `application.js` + sniplet
> `memento` de viescolaire) et des **templates de notification timeline** (`notify/` :
> `event-creation`, `event-update`, `export_events/incidents/punishments`). `presences/.../view-src/`.

---

# Module `incidents`

## WF-I1 — Création d'incident
- **Déclencheur**: bouton « Saisir un incident ».
- **Étapes**: chargement paramètres (`GET /incidents/parameter/types`) → saisie (owner, description, date+heure, lieu, type, gravité, ≥1 protagoniste) → `POST /incidents/incidents` → batch SQL incident + protagonistes → trigger crée les alertes Présences (BR-1).
- **Source**: `incident-form.ts:80-103`, `DefaultIncidentsService.java:469-490`.

## WF-I2 — Traitement / marquage « traité »
- Toggle `processed` → `PUT /incidents/incidents/:id` → toast « L'incident a été traité ».
- **Source**: `incidentsController.ts:250-268`.

## WF-I4 — Attribution d'une punition/sanction
- **Déclencheur**: « + » (droit `createPunishment` OU `createSanction`).
- **Étapes**: sélection type → `setCategory()` → formulaire spécifique (Duty/Detention/Blame/Exclude) → élève(s)/groupe(s) → `POST /incidents/punishments` → résolution catégorie (BR-4), expansion créneaux (BR-5) & élèves (BR-6), stats (BR-9). Si EXCLUDE+absence → crée des absences Présences (BR-7).
- **Source**: `punishment-form.ts:127-137`, `DefaultPunishmentService.java`.

## WF-I6 — Exclusion + déclaration d'absence
- Toggle « ajouter une absence » → `reason_id` (chargé via `GET /structures/:id/reasons` ← Présences) → détection d'anomalie (`POST /incidents/punishments/students/absences`) ; si absences divergentes → avertissement.
- **Source**: `punishment-exclude-form.ts:229-295`.

## WF-I7 — Export CSV
- Worker async `IncidentsExportWorker` → notif timeline « Export Incidents terminé » + dépôt workspace.
- **Source**: `incidentsController.ts:120-130`.

---

# Module `massmailing`

## WF-M1 — Configuration des templates (sniplet)
- Sniplet `massmailing-manage/sniplet-template-form` (3 sous-formulaires MAIL/SMS/PDF) → `create`/`update`/`delete`.
- **Source**: `behaviours.ts:5-10`, `sniplet-template-form.html`.

## WF-M2 — Sélection des filtres / cible
- `vm.openForm()` → élèves/groupes, status, motifs, types punition/sanction, seuil `start_at` → `vm.checkFilter()` (erreurs TYPE/STATUS/ABSENCES_REASONS/LATENESS_REASONS) → `vm.validForm()`.
- **Source**: `home.ts:224-256,557-588`.

## WF-M3 — Compteurs & anomalies
- `vm.fetchData()` → GET status + GET anomalies ; boutons SMS/MAIL/PDF désactivés si aucun compteur > 0.
- **Source**: `home.ts:215-222,450-475,647-653`.

## WF-M4 — Prévisualisation & envoi
- `vm.prefetch(type)` → choix template + coche élèves/parents → `vm.massmail()` → `POST /massmailing/massmailings/{type}` → `MassMailingProcessor.process()` (agrège 8 futures) → `saveMassmailing()` (INSERT mailing + mailing_event, MAJ massmailed). PDF téléchargé, MAIL/SMS toast.
- **Source**: `home.ts:655-748`, `MassMailingProcessor.java:76-147,784-826`.

## WF-M5 — Historique
- `GET /mailings` paginé, regroupé par date ; détail + téléchargement PDF.
- **Source**: `history.ts:192-256`.

---

# Module `statistics-presences`

## WF-S1 — Consultation d'un indicateur
- **Déclencheur**: `switchIndicator()` → sélection période + filtres → `launchResearch()` → POST fetch → rendu TABLE (Global/Monthly/Weekly) ou GRAPH (Monthly). Pagination infinite-scroll.
- **Source**: `public/ts/controllers/main.ts:357,405`.

## WF-S3 — Export CSV
- `openCSVOptions()` (Monthly → lightbox ALL/AUDIENCES) → `vm.export()` → `window.open()` URL → back `indicator.search()` puis `indicator.export()`.
- **Source**: `main.ts:436-459`, `StatisticsController.java:136-175`.

## WF-S4 — Refresh statistiques élève (Global)
- Bouton → `POST /structures/{id}/process/students/statistics/tasks` → `processStudentsStatisticsPrefetch`.
- **Source**: `StatisticsController.java:244-263`.

## WF-S5 — Processing / synchronisation des données (back)
- **Déclencheurs**: cron `processing-cron` (`ProcessingScheduledTask`) ; `POST api/internal/process` ; admin `POST /process/statistics/tasks` ; bus `post-users` (alimente la file).
- **Étapes**: prefetch SELECT file `{DB_SCHEMA}.user` → fetch par élève/EventType → calcul worker → écriture Mongo `presences.statistics` → vidage file. Verrou anti-concurrence. ⚠️ lignes exactes à confirmer.
- **Source**: `StatisticsPresences.java:86-99`, `TaskController.java:21-27`.
