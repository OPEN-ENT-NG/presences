# BUSINESS_RULES — Application Présences

Règles métier extraites des contrôleurs/services/triggers. Sources `fichier:ligne` relatives à la
racine du repo. `⚠️ À confirmer` = à valider sur fichier.

---

# Module `presences` (cœur)

## Feature: Prise d'appel / Registre

### Règle: Unicité d'un registre
- **Contexte**: ouverture d'un appel pour un cours.
- **Condition**: un registre existe déjà pour `(course_id, start_date, end_date)`.
- **Action**: renvoie l'ID existant — pas de doublon (contrainte `UNIQUE`).
- **Source**: `presences/src/main/resources/sql/004-MA-301-register-notification.sql` ; `presences/.../service/impl/DefaultRegisterService.java` (`fetchIfRegisterExists`, ⚠️ lignes).

### Règle: États d'un registre
- **Contexte**: cycle de vie de l'appel.
- **Valeurs**: `TODO=1` (à faire) → `IN_PROGRESS=2` (en cours) → `DONE=3` (validé).
- **Source**: table `register_state`, `presences/.../sql/001-init-schema.sql:17-22` ; front `public/ts/models/RegisterStatus.ts`.

### Règle: Verrou d'un appel validé
- **Contexte**: validation de l'appel.
- **Condition**: `UPDATE register SET state_id=? WHERE id=? AND state_id != 3`.
- **Action**: un registre `DONE` ne peut plus changer d'état (0 ligne affectée si déjà validé).
- **Source**: `RegisterController.java` PUT `/registers/:id/status` ; `DefaultRegisterService` (⚠️ l.613).

### Règle: Correction d'un appel validé
- **Action**: **vérifié** — pas de réouverture possible au niveau registre. `updateStatus` exécute `UPDATE register SET state_id=? WHERE id=? AND state_id != 3` : une fois `DONE` (3), aucun changement d'état n'est appliqué (0 ligne). La correction se fait uniquement au niveau des **événements individuels** (non verrouillés par l'état du registre).
- **Source**: `DefaultRegisterService.java:612-613` ; `RegisterController.java:103-129`. Voir [VERIFICATION.md](VERIFICATION.md) §V2.

### Règle: Classe partagée / multi-enseignants
- **Contexte**: cours partagé entre plusieurs enseignants.
- **Action**: association via `rel_teacher_register` ; si aucun enseignant fourni, un enseignant « vide » est associé. ⚠️ À confirmer.
- **Source**: `presences/.../sql/041-MA-855-add-register-teachers.sql`.

### Règle: Multi-créneaux (split slot)
- **Condition**: setting `allow_multiple_slots`.
- **Action**: découpe l'appel par créneau ; colonne `register.split_slot` (**`NOT NULL DEFAULT false`** — vérifié `sql/012-MA-369:2`).
- **Source**: `sql/040-MA-817`, `sql/012-MA-369-register-split-courses.sql` ; `settingsService.retrieveMultipleSlots()`.

### Règle: Appel oublié & relance
- **Condition**: `now > start_date + 15 min` et registre ≠ `DONE`.
- **Action**: l'appel est « oublié » ; bouton « notifier » visible si droit `notify`, état ≠ DONE, délai entre 15 min et 2 jours. `POST /courses/{id}/notify` → `register.notified=true` + notification.
- **Message**: i18n `presences.register.notify.subject` / `.body` (signé « La vie scolaire de l'établissement », `i18n/fr.json:573`).
- **Source**: `public/ts/controllers/registers.ts` (≈920-946 ⚠️).

### Règle: Création automatique des registres du jour
- **Contexte**: cron quotidien.
- **Action**: `CreateDailyRegistersTask` → `CreateDailyPresenceWorker` génère les registres `TODO` du jour à partir des cours EDT.
- **Source**: `Presences.java` (déploiement workers).

## Feature: Événements / Absences

### Règle: Types d'événement
- **Valeurs**: `ABSENCE=1`, `LATENESS=2` (retard), `DEPARTURE=3` (départ anticipé), `REMARK=4` (remarque), `INCIDENT=5`.
- **Source**: `sql/001-init-schema.sql:68-72` ; `enums/EventTypeEnum.java:9-16` (common, ajoute `FORGOTTEN_NOTEBOOK=6`, `PUNISHMENT=7`, `SANCTION=8`).

### Règle: Double modèle Absence ↔ Event synchronisé
- **Contexte**: l'absence peut être saisie côté « vie scolaire » (table `absence`) ou côté « registre » (table `event`).
- **Action**: création/modif d'une absence recrée/modifie les `event` correspondants quand `editEvents=true`.
- **Source**: `DefaultAbsenceService.java` (`afterPersistAbsence`, `interactingEvents`, ≈645-885 ⚠️).

### Règle: Motif « Présent dans l'établissement » (id -2)
- **Condition**: une présence est enregistrée sur le même créneau (`checkPresenceEvent()`).
- **Action**: motif spécial -2 auto-assigné, non supprimable. `MULTIPLE_REASON=-1` si plusieurs motifs.
- **Source**: `sql/042-create-present-in-structure-reason.sql`.

### Règle: Régularisation automatique
- **Condition**: trigger `regularize_absences()` — si `reason.proving=true` → régularisé auto ; si `reason_id` NULL → non régularisé.
- **Action manuelle**: `PUT /absence/regularized`.
- **Source**: `sql/013-MA-403-sync-absence-with-events.sql`.

### Règle: Justification (motif)
- **Champs**: `reason.proving` (justificatif probant), `reason_type_id ∈ {ABSENCE, LATENESS}`, flags `default`/`group`/`hidden`.
- **Action**: `PUT /absence/reason`.
- **Source**: table `reason`, `sql/001-init-schema.sql:54-66`.

### Règle: Suivi (`followed`)
- **Action**: marquage « en traitement » via `PUT /absences/follow` (schéma `absenceFollow.json`).
- **Source**: `sql/039-MA-679-add-followed-column.sql`.

## Feature: Retards (lateness) & départs anticipés
- **Retard**: `POST /events/:structureId/lateness`, `PUT /events/:id/lateness` (contrôleur dédié `LatenessEventController`).
- **Départ anticipé**: type d'événement `DEPARTURE=3`, saisi via le formulaire d'événement.
- **Source**: `controller/events/LatenessEventController.java` ; `EventType.ts`.

## Feature: Exemptions / Dispenses

### Règle: Exemption ponctuelle vs récursive
- **Ponctuelle**: table `exemption`. **Récursive**: table `exemption_recursive` (`day_of_week[]`, `is_every_two_weeks`).
- **Vue**: `exemption_view` expose une colonne `type` (PONCTUAL/RECURSIVE).
- **Source**: `sql/027-MA-412-exemption.sql`.

### Règle: Flag `attendance`
- **Contexte**: dispense avec présence obligatoire malgré tout (défaut `false`). Champs `subject_id`, `comment`.
- **Source**: `model/Exemption/`, `sql/001-init-schema.sql:95-105`.

### Règle: Restriction enseignant
- **Condition**: droit `manageExemptionRestricted` → l'enseignant ne gère que ses propres élèves.
- **Source**: `controller/ExemptionController.java` (⚠️ ≈87-88).

## Feature: Présences positives
- **Contexte**: déclaration explicite d'élèves présents sur un créneau/discipline.
- **Action**: tables `presence` + `presence_student` ; `markedStudents[]` obligatoire non vide. Version actuelle `createWithoutUpdateAbsence()` (MA-1020) ne met plus à jour les absences en Java (délégué aux triggers).
- **Source**: `sql/022-MA-392-presences-list.sql` ; `DefaultPresenceService.java` (⚠️).

## Feature: Oublis de cahier (forgotten notebook)
- **Règle**: contrainte `UNIQUE(date, student_id, structure_id)` — un seul oubli/élève/jour.
- **Effet**: triggers `increment_notebook_alert` / `decrement_notebook_alert` → maj compteur d'alertes type `FORGOTTEN_NOTEBOOK`. Droit unique `manageForgottenNotebook` (pas de variante restreinte).
- **Source**: `sql/011-MA-384-notebook-alert.sql`.

## Feature: Absences collectives
- **Contexte**: absence d'un groupe entier (sortie, grève…).
- **Action**: `collective_absence` + `rel_audience_collective` (N:N) + `absence.collective_id` ; génère une absence individuelle par élève des audiences ; `counsellor_regularisation` au niveau collectif ; retrait d'élèves via `PUT .../collectives/:id/students`.
- **Source**: `sql/038-MA-289-collective-absences.sql`.

## Feature: Alertes / Assiduité

### Règle: Types & seuils
- **Types**: `ABSENCE`, `LATENESS`, `INCIDENT`, `FORGOTTEN_NOTEBOOK`.
- **Seuils** (par structure, table `settings`): `alert_absence_threshold`, `alert_lateness_threshold`, `alert_incident_threshold`, `alert_forgotten_notebook_threshold` — **défauts vérifiés `5 / 3 / 3 / 3`** (`sql/025-init_settings.sql:8-9`).
- **Comptage**: selon `event_recovery_method` (`HOUR` / `HALF_DAY` / `DAY`).
- **Affichage**: si `count >= threshold`.
- **Source**: `sql/025-init_settings.sql` ; `constants/Alerts.java` (⚠️).

### Règle: Exclusions d'alerte configurables (MA-966/968)
- **Contexte**: certains motifs n'incrémentent pas l'alerte.
- **Action**: `reason_alert` + `reason_alert_exclude_rules_type` (`REGULARIZED`/`UNREGULARIZED`/`LATENESS`) ; flags `exclude_alert_*_no_reason` ; fonctions SQL `get_alert_thresholder()`, `absence_exclude_alert()`, `lateness_exclude_alert()`, `notebook_exclude_alert()`.
- **Source**: `sql/046-MA-968` à `sql/050`.

### Règle: Reset & historisation
- **Action**: reset manuel CPE → l'alerte est historisée dans `alert_history`.
- **Source**: `sql/017-MA-422-alert-historization.sql`.

## Feature: Déclaration d'absence par le parent (statements)

### Règle: Périmètre parent
- **Condition**: le parent ne déclare/valide que **ses** enfants (`childrenIds.contains(student_id)`), sinon 401.
- **Source**: `controller/StatementAbsenceController.java` (≈119-159 ⚠️).

### Règle: Traitement CPE
- **Action**: `PUT /statements/absences/:id/validate {is_treated}` renseigne `treated_at` + `validator_id`.
- **Note**: ⚠️ conversion automatique statement → absence non confirmée (semble manuelle).

## Feature: Dashboard / Accueil (widgets)
- **Contexte**: page d'accueil composée de widgets, chacun gardé par un droit `widget_*`.
- **Widgets**: `widget_alerts`, `widget_forgotten_registers`, `widget_statements`, `widget_remarks`, `widget_absences`, `widget_day_courses`, `widget_current_course`, `widget_day_presences`.
- **Source**: `public/ts/rights.ts:22-29`.

## Feature: Paramétrage / Init
- **Settings**: seuils, `allow_multiple_slots`, exclusions, `initialized` (schéma `jsonschema/settings.json`).
- **Actions**: types d'action avec `abbreviation` ≤ 10 caractères (table `actions` + `event_actions`).
- **Init**: `ONE_D` (primaire) / `TWO_D` (secondaire) — droits `initSettings1D` / `initSettings2D` ; crée settings/motifs/actions/disciplines par défaut.
- **Source**: `sql/021-MA-395-actions.sql` ; `enums/InitTypeEnum.java:6-7` (common).

---

# Module `incidents`

### BR-1 — Alerte sur incident configurable par gravité
- **Contexte**: ajout d'un protagoniste à un incident.
- **Condition**: `incidents.seriousness.exclude_alert_seriousness = FALSE` pour la gravité.
- **Action**: création d'une alerte Présences `presences.create_alert(incident_id, 'INCIDENT', ...)`.
- **Source**: `incidents/.../sql/022-MA-966` (trigger `add_incident_alert`) + `sql/021-MA-966:14-25`. **Vérifié**.

### BR-2 — MAJ alerte sur changement de gravité/date
- **Action**: date changée → propage à `presences.alerts` ; gravité incluse→exclue → supprime les alertes ; exclue→incluse → crée les alertes.
- **Source**: `sql/024-MA-966:2-33` (trigger `update_incident_alert`).

### BR-3 — Suppression d'alerte
- **Action**: `BEFORE DELETE ON protagonist` → `presences.delete_alert(...)`.
- **Source**: `sql/023-MA-966:24-37`.

### BR-4 — Catégories de punition (4 modèles fixes)
- **Valeurs**: `DUTY=1` (Modèle 1 / Devoir), `DETENTION=2` (Modèle 2 / Retenue), `BLAME=3` (Modèle 3 / Blâme), `EXCLUDE=4` (Modèle 4 / Exclusion).
- **Action**: à la création, `PunishmentCategory.getSpecifiedCategoryFromType` résout la classe concrète ; échec → création avortée.
- **Source**: `DefaultPunishmentService.java:54-76` ; `PunishmentCategory.java:20-28,89-111`. **Vérifié**.

### BR-5 / BR-6 — Expansion retenue & multi-élèves
- **Action**: retenue à N créneaux → N documents partageant un `grouped_punishment_id` ; `student_ids[]` × créneaux → M×N documents.
- **Source**: `DefaultPunishmentService.java:105-148`.

### BR-7 / BR-8 — Exclusion ↔ absence Présences
- **Action**: punition `EXCLUDE` avec champ `absence` → crée une absence Présences (`reason_id`/`followed`) pour chaque élève sans absence sur la période ; à la MAJ, cohérence par `DateHelper.isDateEqual()`.
- **Source**: `DefaultPunishmentService.java:150-175,357-359`.

### BR-9 — Mise à jour des statistiques
- **Action**: après create/update/delete punition → `StatisticsPresences.postStatisticsUsers(...)`.
- **Source**: `DefaultPunishmentService.java:86-100,248,763`.

### BR-10 — Visibilité restreinte des punitions
- **Condition**: sans (`PUNISHMENTS_VIEW` ET `SANCTIONS_VIEW`) → ne voit que ses propres punitions (`owner_id=userId`) ; élève → seulement les siennes.
- **Source**: `PunishmentHelper.java:77-82`.

### BR-11 / BR-12 / BR-13 / BR-14 — Divers
- Type agrégé `PUNISHMENT` prioritaire sur `SANCTION` (`DefaultPunishmentService.java:560-569`).
- Flags `used`/`isUsed` si paramètre référencé (`DefaultSeriousnessService.java:22-65`, `DefaultPunishmentTypeService.java:44-69`).
- Validation front par catégorie : delay_at (Duty), start<end (Detention/Exclude) (`utilities/punishments.ts:107-121`).
- `seriousness.level` borné à 7 (`SeriousnessController.java:41,67`).

---

# Module `massmailing`

### Canaux d'envoi (`MailingType`)
- **MAIL** / **SMS** / **PDF**, activables par config `config.mailings.{MAIL|PDF|SMS}`.
- **SMS**: message tronqué à 157 + "..." si > 160 caractères ; envoi seulement si contact non vide.
- **PDF**: un PDF unique, un `div` page-break par destinataire, stocké en Storage.
- **Source**: `enums/MailingType.java:3-7` ; `Sms.java:65-69,47-53` ; `Pdf.java:70-114`.

### Types de relance (`MassmailingType`)
- `REGULARIZED`, `UNREGULARIZED`, `LATENESS`, `PUNISHMENT`, `SANCTION`, `NO_REASON` — chacun mappe une source d'événements (Présences ou Incidents) et des filtres justified/no_reason.
- **Source**: `enums/MassmailingType.java:3-10` ; `MassMailingProcessor.java:755-782`.

### Filtres & seuil
- Période, **seuil minimal `start_at`** (nb d'événements à partir duquel le mailing est proposé), statut réémission (`massmailed`/`waiting`), motifs, types punition/sanction, élèves/groupes.
- **Source**: `home_filters.html:183-190` ; `massmailing.json:30-33`.

### Templates & placeholders
- Catégories `ALL` / `PUNISHMENT_SANCTION` / `LATENESS` / `ABSENCES` ; le filtrage renvoie la catégorie demandée + `ALL`.
- 18 codes de substitution (`CHILD_NAME`, `LEGAL_NAME`, `CLASS_NAME`, `ABSENCE_NUMBER`, `SUMMARY`, `LAST_ABSENCE` [SMS], …).
- Contrainte template SMS : contenu ≤ 160 caractères sinon rejet.
- **Source**: `enums/MailingCategory.java:3-8` ; `DefaultSettingsService.java:22-45,114-122` ; `enums/TemplateCode.java:3-33`.

### Regroupement & anomalies
- Recovery method défaut `HALF_DAY` ; punitions `isMultiple` → 1 mailing/punition sinon 1 seul ; dédoublonnage destinataires par contact (MAIL/SMS) ou adresse (PDF).
- Anomalies = destinataires sans canal (aucun parent avec email/mobile).
- **Source**: `MassMailingProcessor.java:122-123,388-440,544-568` ; `DefaultMassmailingService.java:99-133`.

---

# Module `statistics-presences`

### Indicateurs déployés
- 3 indicateurs : **Global** (tableau par classe/élève, TABLE), **Monthly** (par mois, TABLE+GRAPH), **Weekly** (par créneau/jour, calendrier).
- **Source**: `StatisticsPresences.java:113-123` ; `indicator/impl/`.

### Types d'événements agrégés
- `DEPARTURE`, `INCIDENT`, `UNREGULARIZED`, `LATENESS`, `PUNISHMENT`, `SANCTION`, `REGULARIZED`, `NO_REASON` (+ `ABSENCE_TOTAL` côté front).
- **Source**: `utils/EventType.java:3-12` ; `public/ts/filter/type.ts:1-12`.

### Calcul taux d'absence (Global)
- Taux = `(slotsAbsence / totalHalfDays) * 100` ; taux moyen/élève divisé par `nbStudents` ; NaN/Inf → 0 ; arrondi `HALF_DOWN` 2 décimales.
- Demi-journées : par date hors week-end/jours exclus, mercredi +1, autres +2 ; si recovery `DAY` total /2.
- Taux affiché seulement si `event_recovery_method ∈ {DAY, HALF_DAY}`.
- **Source**: `indicator/impl/Global.java:117-141,267-285` ; `template/main.html:90`.

### Contrainte filtre (Monthly/Weekly)
- Soit une absence (`NO_REASON`/`UNREGULARIZED`/`REGULARIZED`), soit UN seul type d'événement ; sinon reset à `NO_REASON`.
- **Source**: `public/ts/controllers/main.ts:213-251`.

### Exports
- CSV pour Global et Monthly ; **désactivé pour Weekly**. Monthly : option `ALL` (classes+élèves) ou `AUDIENCES` (classes seules).
- **Source**: `template/main.html:77` ; `core/enums/export-type.enum.ts:1-3`.

---

# Module `common` (règles partagées)

### Calcul demi-journées & regroupement d'événements
- `EventsHelper.mergeEventsByDates(...)` regroupe par jour + demi-journée puis par élève ; demi-journée pilotée par `Settings.endOfHalfDayTimeSlot`.
- **Source**: `common/.../helper/EventsHelper.java:35-92` ; `model/Settings.java:14`.

### Méthode de décompte (recovery)
- `HOUR` / `HALF_DAY` / `DAY`, portée par `Settings.recoveryMethod`.
- **Source**: `enums/EventRecoveryMethodEnum.java:6-8`.

### Statut justifié / régularisé
- `EventModel.counsellorInput` (saisie CPE), `counsellorRegularisation` (régularisé), `followed`, `massmailed` ; `ReasonModel.isProving` (motif probant), `isAbsenceCompliance`.
- **Source**: `model/EventModel.java:14-17` ; `model/ReasonModel.java:11,16`.

### Exclusions d'alerte par motif
- `ReasonModel.regularizedAlertExclude` / `unregularizedAlertExclude` / `latenessAlertExclude` (défaut `true`) ; règles `ReasonAlertExcludeRulesType`.
- **Source**: `model/ReasonModel.java:19-21` ; `enums/ReasonAlertExcludeRulesType.java:6-8`.

> ⚠️ Anomalie repérée : `enums/DayOfWeek.java:3-9` ne liste pas `THURSDAY` (bug probable) — à confirmer.
