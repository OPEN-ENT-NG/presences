# Vérification sur fichier des points ⚠️ prioritaires

Ce document consigne la **relecture directe du code** (le 17/06/2026) des points marqués
`⚠️ À confirmer` dans les specs, jugés prioritaires car ils touchent des affirmations normatives ou le
diff avec le PM ([DIFFERENCES_PM.md](DIFFERENCES_PM.md)). Chaque point a un statut :

- ✅ **Confirmé** : la règle est exacte, source vérifiée à la ligne.
- ✏️ **Corrigé** : la spec contenait une erreur, désormais corrigée (valeur exacte indiquée).
- ❌ **Infirmé** : la règle supposée n'existe pas dans le code.

| # | Point vérifié | Statut | Résultat |
| - | ------------- | ------ | -------- |
| V1 | Absences prévisionnelles (`/planned-absences`) | ✅/❌ | Route front sans aucun back |
| V2 | Correction d'un appel validé (verrou DONE) | ✅ | Pas de réouverture possible |
| V3 | Unicité du registre | ✅ | Contrainte `UNIQUE` confirmée |
| V4 | `register.split_slot` valeur par défaut | ✏️ | `DEFAULT false` (et non `true`) |
| V5 | Seuils d'alerte par défaut | ✅ | `5 / 3 / 3 / 3` |
| V6 | Déclaration parent validée → absence auto | ❌ | Pas de création automatique d'absence |
| V7 | Motif spécial « Présent dans l'établissement » (-2) | ✅ | Confirmé, non supprimable |
| V8 | `DayOfWeek` enum sans THURSDAY | ✅ | Bug confirmé (jeudi manquant) |
| V9 | Restriction enseignant sur les exemptions | ✅ | Filtre par élèves de l'enseignant |

---

## V1 — Absences prévisionnelles : route front orpheline ❌ (back absent)

- **Constat** : `grep -rli "planned"` sur `presences/src/main/java` → **0 résultat**. Aucun
  `PlannedAbsenceController`, aucun service, aucune table `planned_absence`.
- La seule trace est la **route front** `app.ts:36-37` (`when('/planned-absences', {action:'planned-absences'})`).
- **Conclusion** : la feature « absences prévisionnelles » (E22 chez le PM, classée « existante à
  abandonner ») est **déjà non fonctionnelle côté backend** — au mieux une coquille front.
  → À reclasser dans l'inventaire PM en « déjà non opérationnelle » plutôt que « existante ».
- **Source** : `presences/src/main/resources/public/ts/app.ts:36-37` ; absence totale de back vérifiée.

## V2 — Correction d'un appel validé : verrou DONE ✅

- **Constat** : `DefaultRegisterService.updateStatus()` exécute
  `UPDATE presences.register SET state_id = ? WHERE id = ? AND state_id != 3`.
- Une fois l'état `DONE` (=3), **aucune transition n'est appliquée** (0 ligne affectée), quel que soit
  l'état cible → l'appel validé est **verrouillé**.
- Le contrôleur `PUT /registers/:id/status` valide seulement que `state_id ∈ {TODO, IN_PROGRESS, DONE}`
  (`RegisterController.java:110-112`) ; il ne propose pas de réouverture.
- **Conclusion** : il n'existe **pas** d'endpoint de correction/réouverture d'un appel validé. La
  correction passe par les **événements individuels** (table `event`, non verrouillés par l'état du
  registre). La ligne PM E12 « Correction d'un appel validé / commune » est donc à nuancer : ce qui est
  corrigeable, ce sont les événements, pas le registre.
- **Source** : `service/impl/DefaultRegisterService.java:612-613` ; `controller/RegisterController.java:103-129`.

## V3 — Unicité du registre ✅

- **Constat** : `ALTER TABLE ... ADD UNIQUE (course_id, start_date, end_date)`.
- **Conclusion** : l'unicité d'un registre par `(course_id, start_date, end_date)` est garantie en base.
- **Source** : `presences/src/main/resources/sql/004-MA-301-register-notification.sql:3`.

## V4 — `register.split_slot` : DEFAULT false ✏️ (corrigé)

- **Spec initiale** : « défaut `true` ⚠️ ». **Erreur**.
- **Constat** : `ADD COLUMN split_slot boolean NOT NULL DEFAULT false`.
- **Conclusion** : la valeur par défaut est **`false`** (pas de découpe par créneau par défaut).
  Corrigé dans [BUSINESS_RULES.md](BUSINESS_RULES.md) et [DATA_MODEL.md](DATA_MODEL.md).
- **Source** : `presences/src/main/resources/sql/012-MA-369-register-split-courses.sql:2`.

## V5 — Seuils d'alerte par défaut ✅

- **Constat** : à l'init d'une structure, la fonction trigger `presences.init_settings()` insère
  `VALUES (id_etablissement, 5, 3, 3, 3)` pour
  `(alert_absence_threshold, alert_lateness_threshold, alert_incident_threshold, alert_forgotten_notebook_threshold)`.
- **Conclusion** : défauts = **absence 5, retard 3, incident 3, oubli de cahier 3**.
  (NB : la cible primaire du PM évoque un seuil légal de 4 demi-journées — c'est une valeur **cible**,
  différente du défaut actuel 5.)
- **Source** : `presences/src/main/resources/sql/025-init_settings.sql:8-9`.

## V6 — Déclaration parent validée → absence : pas de conversion automatique ❌

- **Constat** : `DefaultStatementAbsenceService.validate()` ne fait que :
  `body.put("treated_at", is_treated ? now : null)` + `body.put("validator_id", userId)` puis
  `statementAbsence.update(handler)`. **Aucun appel** de création d'absence (`create-absences`,
  `createAbsence`, eventbus) n'y figure.
- **Conclusion** : valider une déclaration parent **ne crée pas** automatiquement une absence ; elle est
  seulement marquée « traitée » (`treated_at`/`validator_id`). Cela **corrobore** que la cible PM N03
  « propagation auto d'une absence multi-jours » est bien une **nouvelle** fonctionnalité (le mécanisme
  n'existe pas aujourd'hui).
- **Source** : `service/impl/DefaultStatementAbsenceService.java:99-104`.

## V7 — Motif spécial « Présent dans l'établissement » (-2) ✅

- **Constat** : `INSERT INTO presences.reason (id, structure_id, label, proving, comment, "default",
  "group", hidden, absence_compliance) VALUES (-2, -1, 'presences.in.structure', true, '', false,
  false, true, false)`.
- **Conclusion** : motif système d'id **-2**, `proving=true`, `hidden=true`, rattaché à la
  pseudo-structure `-1` → motif global non supprimable. Confirme la règle d'auto-assignation lorsqu'une
  présence est enregistrée sur le même créneau.
- **Source** : `presences/src/main/resources/sql/042-create-present-in-structure-reason.sql`.

## V8 — `DayOfWeek` enum sans THURSDAY ✅ (bug confirmé)

- **Constat** : l'enum liste `MONDAY, TUESDAY, WEDNESDAY, FRIDAY, SATURDAY, SUNDAY` — **`THURSDAY`
  (jeudi) est absent**.
- **Conclusion** : bug réel dans le code partagé. Impact potentiel sur toute logique s'appuyant sur
  cet enum (exemptions récursives `day_of_week[]`, calculs hebdo). À remonter en correctif.
- **Source** : `common/src/main/java/fr/openent/presences/enums/DayOfWeek.java`.

## V9 — Restriction enseignant sur les exemptions ✅

- **Constat** :
  `boolean hasRestrictedRight = WorkflowActionsCouple.READ_EXEMPTION.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()))`
  puis `restrictedTeacherId = hasRestrictedRight ? user.getUserId() : null` →
  `userService.getStudentsFromTeacher(restrictedTeacherId, structureId)` filtre la liste sur les élèves
  de l'enseignant. Même mécanisme côté écriture (`MANAGE_EXEMPTION`, l.223-228).
- **Conclusion** : un enseignant disposant uniquement du droit restreint ne lit/gère que les exemptions
  de **ses propres élèves**. Confirmé.
- **Source** : `controller/ExemptionController.java:87-91,223-228`.

---

## Points ⚠️ NON revus ici (restent à valider avant usage normatif)

Volontairement hors de cette passe prioritaire (moindre impact ou hors repo) :

- Détail des pipelines Mongo de `statistics-presences` (`GlobalSearch`, `Monthly` multi-classe, formules Weekly) — n° de ligne indicatifs.
- Logique exacte du double modèle Absence↔Event (`DefaultAbsenceService.afterPersistAbsence`).
- Mécanisme de seuil `massmailed` côté presences (trigger `sql/033`).
- Propriétaire réel de la collection Mongo `courses` et nature de la façade `viescolaire`
  (cf. [ADHERENCES_VIESCOLAIRE_EDT.md](ADHERENCES_VIESCOLAIRE_EDT.md) §F) — **hors repo**.
- `rel_teacher_register` : comportement « enseignant vide » si aucun enseignant fourni.
