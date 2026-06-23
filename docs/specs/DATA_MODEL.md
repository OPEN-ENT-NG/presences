# DATA_MODEL — Application Présences

Entités issues des schémas SQL (`*/src/main/resources/sql`), Mongo, `jsonschema/`, modèles Java
(`model/`) et TS (`public/ts/models`). `⚠️ À confirmer` = à valider sur fichier.

---

# Module `presences` — schéma PostgreSQL `presences.*`

## Entité: register (registre / appel)
| Champ | Type | Obligatoire | Calculé | Description |
| ----- | ---- | ----------- | ------- | ----------- |
| id | bigserial | oui | oui | PK |
| personnel_id | varchar | non | non | Personnel ayant ouvert l'appel |
| course_id | varchar | non | non | Réf. cours **EDT** (Mongo `courses`) |
| state_id | int → register_state | oui | non | TODO/IN_PROGRESS/DONE |
| proof_id | int → register_proof | non | non | Justificatif |
| counsellor_input | bool | non | non | Saisie CPE |
| owner | varchar | non | non | Créateur |
| structure_id, subject_id, start_date, end_date | — | — | non | Ajoutés `003` |
| notified | bool | non | non | Relance envoyée (`004`) |
| split_slot | bool | oui | non | Multi-créneaux, `NOT NULL DEFAULT false` (`012`) |

**Relations**: `rel_teacher_register` (N enseignants), `event` (N événements). **UNIQUE**(course_id, start_date, end_date).
**Source**: `sql/001-init-schema.sql:31-45`, `003`, `004`, `012-MA-369`, `041-MA-855`.

## Entité: register_state / register_proof
- `register_state`: id, structure_id, label (`TODO`/`IN_PROGRESS`/`DONE`). `sql/001-init-schema.sql:17-22`.
- `register_proof`: id, structure_id, label. `sql/001-init-schema.sql:24-29`.

## Entité: event (événement de vie scolaire)
| Champ | Type | Obligatoire | Description |
| ----- | ---- | ----------- | ----------- |
| id | bigserial | oui | PK |
| start_date / end_date | timestamp | oui | Bornes |
| comment | text | non | Commentaire |
| counsellor_input | bool | non | Saisie CPE |
| counsellor_regularisation | bool | non | Régularisé (`013`) |
| followed | bool | non | Suivi (`039`) |
| massmailed | bool | non | Relancé |
| student_id | varchar | oui | Élève |
| register_id | int → register (CASCADE) | non | Registre source |
| type_id | int → event_type | oui | ABSENCE/LATENESS/DEPARTURE/REMARK/INCIDENT |
| reason_id | int → reason | non | Motif |
| owner, created | — | — | Audit |

**Source**: `sql/001-init-schema.sql:68-93`, `013`, `039`.

## Entité: reason (motif) / reason_type
- `reason`: id, structure_id, label, **proving** (probant), comment, default, group, type_id → reason_type, hidden, created. `sql/001-init-schema.sql:54-66`, `034`.
- `reason_type`: id, label, structure_id (`ALL=0`/`ABSENCE=1`/`LATENESS=2`). `sql/001-init-schema.sql:47-52` ; `enums/ReasonType.java:6-8` (common).
- Motif spécial **id -2** « Présent dans l'établissement » non supprimable (`sql/042`) ; `MULTIPLE_REASON=-1`.

## Entité: absence
| Champ | Type | Obligatoire | Description |
| ----- | ---- | ----------- | ----------- |
| id | bigserial | oui | PK |
| start_date / end_date | timestamp | oui | Bornes |
| student_id | varchar | oui | Élève |
| reason_id | int | non | Motif |
| structure_id | varchar | — | (`013`) |
| counsellor_regularisation | bool | non | Régularisé (`013`) |
| owner | varchar | non | (`037`) |
| created | timestamp | non | (`034`) |
| followed | bool | non | (`039`) |
| collective_id | int → collective_absence | non | (`038`) |

**Relations**: synchronisée avec `event` via triggers. **Source**: `sql/006/007-MA-287`, `013`, `037`, `038`, `039`.

## Entité: exemption / exemption_recursive
- `exemption`: id, student_id, structure_id, subject_id, start_date, end_date, comment, **attendance** (présence obligatoire malgré dispense), recursive_id. `sql/001-init-schema.sql:95-105`, `027`.
- `exemption_recursive`: id, student_id, structure_id, dates, **day_of_week[]**, is_every_two_weeks, comment, attendance. `sql/027-MA-412`.
- Vue `exemption_view` (colonne `type` PONCTUAL/RECURSIVE).

## Entité: presence / presence_student
- `presence`: id, start_date, end_date, discipline_id, owner, structure_id.
- `presence_student`: student_id, comment, presence_id (CASCADE).
- **Source**: `sql/022-MA-392`.

## Entité: forgotten_notebook
- id, date, student_id, structure_id. **UNIQUE**(date, student_id, structure_id). Triggers d'alerte. `sql/011-MA-384`.

## Entité: collective_absence / rel_audience_collective
- `collective_absence`: start_date, end_date, counsellor_regularisation, comment, reason_id, owner_id, structure_id, created_at.
- `rel_audience_collective`: collective_id (CASCADE), audience_id.
- **Source**: `sql/038-MA-289`.

## Entité: alerts / alert_history
- `alerts`: student_id, structure_id, type (ABSENCE/LATENESS/INCIDENT/FORGOTTEN_NOTEBOOK), count, exceed_date/date, event_id. **UNIQUE**(structure_id, student_id, type).
- `alert_history`: copie d'une alerte supprimée (audit).
- `reason_alert` / `reason_alert_exclude_rules_type`: config exclusions (structure_id, reason_id, rule_type_id, soft delete).
- **Source**: `sql/011-MA-384`, `013-MA-422`, `017-MA-422`, `046-MA-968`.

## Entité: settings
- structure_id (UNIQUE), `alert_*_threshold` (absence/lateness/incident/forgotten), `event_recovery_method`, `allow_multiple_slots`, `exclude_alert_*`, `initialized`.
- **Source**: `sql/025-init_settings.sql`, `026`, `040`, `046` ; `enums/SettingsFieldEnum.java:6-15` (common).

## Entité: actions / event_actions
- `actions`: structure_id, label, abbreviation (≤10 car.), hidden.
- `event_actions`: event_id, action_id (CASCADE), owner, comment.
- **Source**: `sql/021-MA-395`.

## Entité: statement_absence (déclaration parent)
- start_at, end_at, student_id, structure_id, description, treated_at, validator_id, attachment_id, parent_id, created_at, metadata (json).
- **Source**: `sql/030`, `032`, `036-MA-673`.

## Entité: testimony / testimony_attachment
- Témoignages/déclarations avec PJ. `sql/001-init-schema.sql:107-128`.

---

# Module `incidents` — schéma `incidents.*` + Mongo `presences.punishments`

## Entité: incident
- id (bigserial), owner, structure_id, date, selected_hour (bool), description, created, processed (bool), FK `place_id`/`partner_id`/`type_id`/`seriousness_id` (CASCADE).
- **Source**: `incidents/.../sql/001-init-schema.sql:39-61`.

## Entité: protagonist
- PK (user_id, incident_id) ; FK incident_id (CASCADE), type_id → protagonist_type. Porte les triggers d'alerte.
- **Source**: `sql/001-init-schema.sql:70-79`.

## Entités de paramétrage
- `place` / `partner` / `incident_type` / `protagonist_type` / `seriousness` : id, structure_id, label, hidden, created. `seriousness` ajoute `level` (≤7) et `exclude_alert_seriousness` (def FALSE).
- `punishment_type`: id, structure_id, label, type (PUNISHMENT|SANCTION), hidden, punishment_category_id.
- `punishment_category`: 4 lignes seedées (Modèle 1-4).
- **Source**: `sql/001-init-schema.sql:81-88`, `003`, `004-MA-497`, `017`, `020-MA-968`.

## Entité: Punishment (MongoDB `presences.punishments`)
| Champ | Type | Obligatoire | Description |
| ----- | ---- | ----------- | ----------- |
| id | UUID | oui | PK |
| type_id | — | oui | Type punition |
| owner_id | — | oui | Créateur |
| structure_id | — | oui | Structure |
| student_id | — | oui | Élève |
| incident_id | — | non | Incident lié |
| fields | JsonObject | non | Payload spécifique catégorie (Duty/Detention/Blame/Exclude) |
| grouped_punishment_id | UUID | non | Groupe de retenues |
| type | — | oui | Calculé (joint depuis punishment_type) |
| processed | bool | non | Traité |
| created_at / updated_at | — | — | Calculés |

**Source**: `model/Punishment.java:26-58`. (La table SQL `punishment` a été supprimée, `sql/005-MA-495`.)

---

# Module `massmailing` — schéma `massmailing.*`

## Entité: mailing (un envoi unitaire)
- id, student_id, structure_id, type (PDF/MAIL/SMS), content, recipient_id, recipient, created, file_id + metadata (json, PDF).
- **Source**: `sql/001-MA-349-init-massmailing.sql:10-22`, `006`, `007-MA-694`.

## Entité: mailing_event (lien mailing ↔ événement)
- id, mailing_id (FK), event_id (varchar), event_type. **UNIQUE**(mailing_id, event_id, event_type).
- **Trigger** `auto_massmail_events` → met `presences.event.massmailed=true` (ABSENCE/LATENESS).
- **Source**: `sql/001-MA-349:24-31`, `003`, `008-MA-724`.

## Entité: template
- id, structure_id, name, content, type (PDF/MAIL/SMS), created, owner, category (def `ALL`).
- **Source**: `sql/002-MA-349-massmailing-settings.sql`, `009-MA-771`.

> Pas de table « campagne » : chaque envoi est une ligne `mailing` reliée à ses événements.

---

# Module `statistics-presences` — Mongo + file SQL

## Collections MongoDB
- `presences.statistics`: un document par (structure, user, type) — start_date, end_date, reason, slots[], punishment_type, grouped_punishment_id, audiences[], class_name, name. ⚠️ structure détaillée à confirmer.
- `presences.statistics_weekly_audiences`: {register_id, audience_id}, slot_id, structure_id, student_count, start_at, end_at. ⚠️.
- **Source**: `StatisticsPresences.java:37,39`.

## File d'attente SQL
- Table `{DB_SCHEMA}.user` (id, structure, modified) — INSERT ON CONFLICT, purge après traitement.
- **Source**: `service/impl/DefaultStatisticsPresencesService.java` (⚠️).

## Filtre de recherche (`StatisticsFilter`)
- types, audiences, users, reasons, punishmentTypes, sanctionTypes, structure, userId, start/end, from/to, exportOption, hourDetail, page, rateDisplay, noLatenessReason.
- **Source**: `common/.../statistics_presences/model/StatisticsFilter.java:16-32`.

---

# Module `common` — modèles partagés

## Hiérarchie Person
- `Person` (id, displayName, info, firstName, lastName) ; `Student` (+ classId, className, dayHistory, audiences) ; `User`. `Metadata` (fichier).
- **Source**: `model/Person/{Person,Student,User,Metadata}.java`.

## Événements / motifs
- `EventModel` (id, startDate, endDate, comment, counsellorInput, counsellorRegularisation, followed, massmailed, student, registerId, type, reason, owner, created). `validate()` exige tous les champs non nuls (`EventModel.java:192-207`).
- `ReasonModel` (id, structureId, label, isProving, comment, isDefault, isGroup, isHidden, isAbsenceCompliance, reasonType, 3 flags d'exclusion d'alerte). **NB**: `validate()` retourne toujours `false` (`ReasonModel.java:171-173`).
- **Source**: `model/EventModel.java:10-44` ; `model/ReasonModel.java:8-34`.

## Enums clés
- `EventTypeEnum`: ABSENCE(1), LATENESS(2), DEPARTURE(3), REMARK(4), INCIDENT(5), FORGOTTEN_NOTEBOOK(6), PUNISHMENT(7), SANCTION(8). `enums/EventTypeEnum.java:9-16`.
- `ReasonType`: ALL(0), ABSENCE(1), LATENESS(2).
- `GroupType`: CLASS, GROUP, MANUAL_GROUP.
- `ExportType`: CSV, PDF. `InitTypeEnum`: ONE_D(1), TWO_D(2).
- `EventRecoveryMethodEnum`: HOUR, HALF_DAY, DAY.
- `UserType`: Teacher, Personnel, Relative, Student (`core/constants/UserType.java:4-7`).

## Audience / groupes / EDT
- `Audience` (id, name, countStudents, students[]) ; `Subject` (matière : id, externalId, code, name, rank) ; `Grouping` (regroupement) + `StudentDivision`.
- `TimeslotModel` (id, name, structureId, slots[]) ; `SlotModel` (id, name, startHour, endHour) — créneaux EDT via Vie Scolaire.
- **Source**: `model/Audience.java`, `model/Subject.java`, `model/grouping/`, `model/TimeslotModel.java`, `model/SlotModel.java`.

## Paramétrage structure
- `Settings` (alertAbsenceThreshold, alertLatenessThreshold, alertIncidentThreshold, alertForgottenThreshold, recoveryMethod, multipleSlot, endOfHalfDayTimeSlot).
- **Source**: `model/Settings.java:8-23`.

## Statistiques (partagé)
- `StatisticsUser` (id=studentId, structureId, modified) ; `StructureStatisticsUser`.
- **Source**: `model/StatisticsUser.java`, `model/StructureStatisticsUser.java`.
