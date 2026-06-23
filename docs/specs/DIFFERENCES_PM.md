# Écarts entre le code découvert et l'état des lieux du PM

**Document PM de référence** : *« Refonte ou migration – Brouillon »* — Confluence `ODE/5570822164`
(dernière modif. 15/06/2026, auteur bassam.elkhoury). Inventaire de **38 fonctionnalités existantes**
+ tableau comparatif de 45 lignes (E01→E40, N01→N07) pondéré en points de complexité, concluant à un
**rebuild React** (94 pts) plutôt qu'une refonte (122 pts).

**Objet de ce document** : confronter, ligne à ligne, l'inventaire PM à ce que le code révèle
réellement. Ce n'est **pas** une remise en cause de la recommandation (rebuild) — elle est cohérente
avec ce que montre le code (legacy AngularJS, couplage fort). C'est un relevé des **écarts de
périmètre, de classification et d'estimation de complexité** susceptibles d'impacter le chiffrage et
le découpage du rebuild.

Légende verdict PM : *Commune* (conservée), *Commune (retravaillée)*, *À abandonner*, *Nouvelle*.

---

## 1. Synthèse des écarts majeurs

| # | Écart | Impact |
| - | ----- | ------ |
| A | **« Incidents » (E33/E34, 2 features, 2 pts, abandon)** = en réalité un module entier (`incidents/`, 69 fichiers Java, MongoDB, 4 catégories de punition, 6 paramétrages, triggers d'alerte, exclusions générant des absences). | Décommissionnement **largement sous-estimé** ; couplage SQL/eventbus à démêler. |
| B | **« Statistiques » (E37, 1 feature, 2 pts, abandon)** = module entier (`statistics-presences/`, 3 indicateurs, workers de processing, crons, file SQL, Mongo). | Idem : abandon non trivial ; ne se « supprime » pas en 2 pts. |
| C | **Publipostage (E35/E36, 2 features)** = module entier (`massmailing/`, 3 canaux, templates multi-catégories, 18 placeholders, anomalies, historique). | La cible N05 « courrier responsables » réutilise une mécanique bien plus riche que « 2 features ». |
| D | **Alertes (E27, « retravaillée », 5 pts)** = système très élaboré : 4 types d'alerte, **exclusions configurables par motif** (MA-966/968), historisation, fonctions SQL dédiées. | Le « suivi mensuel d'assiduité » cible hérite d'une logique d'exclusion complexe — risque de sous-estimation. |
| E | **Absences collectives (E23) et Exemptions (E24)** classées « à abandonner » à **1 pt** = features récentes et complètes dans le code (collective_absence + relations N:N ; exemptions ponctuelles **et récursives** avec `attendance`). | Si abandon : effort de retrait > 1 pt. Si finalement repris : redéveloppement non chiffré. |
| F | **Plusieurs sous-fonctionnalités du cœur absentes de l'inventaire** (cf. §3) : disciplines, types d'action sur événement, archive, motif « Présent dans l'établissement » (-2), témoignages, double modèle absence↔event synchronisé par triggers. | Le périmètre réel du cœur est plus large que les 11+10 features « Appel/Événements ». |
| G | **Front 100 % AngularJS, aucune amorce React dans le repo.** `view-src/` ≠ couche de compat React (c'est la coquille de vue + templates de notification timeline). | Confirme le point de départ « from scratch » du rebuild : il n'y a rien de React à reprendre. |

---

## 2. Réconciliation ligne à ligne du tableau PM

### Domaine Appel
| ID PM | Libellé PM | Verdict PM | Constat code | Écart |
| ----- | ---------- | ---------- | ------------ | ----- |
| E05 | Liste des appels du jour | retravaillée | `RegisterController`, états TODO/IN_PROGRESS/DONE (`register_state`) | OK |
| E06 | Prise d'appel – absent | retravaillée | `event`/`absence` + double modèle synchronisé par triggers | Complexité réelle > UI : sync absence↔event sous-jacente |
| E07 | Prise d'appel – **retard** | **à abandonner (1 pt)** | **Contrôleur dédié `events/LatenessEventController` + type `LATENESS=2`** | ⚠️ Feature bien plus développée qu'un simple champ ; abandon = retrait d'un contrôleur complet |
| E08 | Départ anticipé | à abandonner | type `DEPARTURE=3` (saisi via formulaire event) | OK (sous-cas d'event) |
| E09 | Remarques | à abandonner | type `REMARK=4` + `widget_remarks` (dashboard) | Existe aussi comme widget d'accueil |
| E10 | Validation de l'appel | commune | `PUT /registers/:id/status` (verrou DONE) | OK |
| E11 | Création auto des registres | retravaillée | cron `CreateDailyRegistersTask` + unicité ; PM dit « se crée au 1er élève » (cible) | OK (mécanisme actuel = cron/ouverture) |
| E12 | Correction d'un appel validé | commune | ⚠️ pas d'endpoint de réouverture ; correction au niveau **events** | À clarifier : la « correction » n'existe pas au niveau registre |
| E13 | Classe partagée | retravaillée | `rel_teacher_register` (multi-enseignants) | OK |
| E14 | Rappel appel oublié | retravaillée | `POST /courses/{id}/notify` (15 min < délai < 2 j) | OK |
| E15 | Export CSV appels | commune | `CourseController.exportCourses` + worker | OK |
| N01/N04 | Appel journée / pré-marquage | nouvelles | settings `allow_multiple_slots`/`split_slot` existent déjà | Brique « créneaux » partiellement présente |

### Domaine Événements / Registre / Assiduité
| ID PM | Libellé PM | Verdict PM | Constat code | Écart |
| ----- | ---------- | ---------- | ------------ | ----- |
| E16 | Registre récapitulatif mensuel | retravaillée | `RegistryController` : **2 formats** d'export (`/export` ancien + `/export/new` board, récent ORGA-339) | Deux implémentations coexistent |
| E17 | Liste des événements | à abandonner (2 pts) | `EventController` (filtres riches, pagination, exports) — gros contrôleur | Abandon non trivial |
| E18 | Justifier / régulariser | commune | `PUT /absence/regularized` + trigger `regularize_absences()` | OK |
| E19 | Motif sur absence | commune | `reason`/`reason_type`, flag `proving` | OK |
| E21 | Fiche élève / calendrier | retravaillée | `CalendarController` agrège EDT+events+exemptions+incidents | Agrégation multi-sources (couplage) |
| E22 | **Absences prévisionnelles** | à abandonner | **Route front `/planned-absences` SANS contrôleur Java** | ⚠️ Probablement déjà vestigiale / incomplète — à confirmer |
| E23 | **Absences collectives** | à abandonner (1 pt) | `collective_absence` + `rel_audience_collective` + `CollectiveAbsenceController` (MA-289, récent et complet) | ⚠️ Feature substantielle, retrait > 1 pt |
| E24 | **Exemptions / dispenses** | à abandonner (1 pt) | `exemption` **+ `exemption_recursive`** (`day_of_week[]`, `attendance`), vue dédiée | ⚠️ Deux variantes ; sous-estimé |
| E25 | Carnet / cahier oublié | à abandonner (1 pt) | `forgotten_notebook` + triggers increment/decrement d'alerte | Couplé aux alertes (retrait impacte trigger) |
| E26 | Présences positives | à abandonner (1 pt) | `presence`/`presence_student` (MA-392/1020) | PM : « couvert par appel d'urgence N02 » — base réutilisable pour N02 |
| E27 | Alertes à seuil | retravaillée (5 pts) | 4 types + **exclusions configurables par motif** + historisation + fonctions SQL | ⚠️ Complexité réelle élevée |

### Domaine Déclarations / Parent
| ID PM | Libellé PM | Verdict PM | Constat code | Écart |
| ----- | ---------- | ---------- | ------------ | ----- |
| E28 | Déclaration parent | retravaillée | `statement_absence` + `StatementAbsenceController` (PJ multipart, vérif `childrenIds`) | OK |
| E29 | File de validation | commune | `PUT .../:id/validate` (`treated_at`, `validator_id`) | OK |
| E30 | Export CSV déclarations | à abandonner | `GET /statements/absences/export` existe | PM marque déjà « à confirmer » |
| E31 | Historique enfant (parent) | retravaillée | vue calendrier/fiche en lecture parent | OK |
| E32 | Vue élève | à abandonner | filtres `StudentEventsViewRight` (user=élève) | OK (pas de profil élève en 1D) |
| N03/N06 | Propagation multi-jours / notif parent | nouvelles | templates notif `view-src/notify/event-*` existent | Brique notification partiellement présente |

### Domaines Publipostage / Incidents / Statistiques / Paramétrage
| ID PM | Libellé PM | Verdict PM | Constat code | Écart |
| ----- | ---------- | ---------- | ------------ | ----- |
| E35 | Publipostage multi-canal | retravaillée (3 pts) | module `massmailing` : MAIL/SMS/PDF, templates 4 catégories, 18 placeholders, regroupement, anomalies | Réduit côté cible à « courrier responsables » mais base très riche |
| E36 | Historique des envois | à abandonner (1 pt) | `MailingController` + table `mailing`/`mailing_event` + trigger massmailed | Couplé à `presences.event.massmailed` |
| E33/E34 | Incidents / Punitions | à abandonner (1+1 pt) | **module `incidents` entier** (cf. §1.A) | ⚠️ Très sous-estimé |
| E37 | Statistiques agrégées | à abandonner (2 pts) | **module `statistics-presences` entier** (cf. §1.B) | ⚠️ Très sous-estimé |
| E40 | Regroupements de classes | à abandonner (1 pt) | `Grouping`/`StudentDivision` + `GroupingController` (présent et implémenté) | PM « usage non mesuré » — c'est pourtant codé |

---

## 3. Fonctionnalités présentes dans le code mais absentes de l'inventaire PM

Ces éléments n'apparaissent dans aucune des 45 lignes ; à intégrer au périmètre de décommissionnement
(ou de reprise) :

1. **Types d'action sur événement** (`actions` + `event_actions`, `abbreviation` ≤10) — actions de suivi paramétrables, distinctes des « actions de suivi disciplinaire » E20. `presences/.../sql/021-MA-395`.
2. **Disciplines** (`DisciplineController`, CRUD) — matières/disciplines internes.
3. **Archive** (`ArchiveController`) — archivage de données. ⚠️ portée à confirmer.
4. **Motif spécial « Présent dans l'établissement » (id -2)** non supprimable + `MULTIPLE_REASON=-1`. `sql/042`.
5. **Double modèle Absence ↔ Event synchronisé par triggers PostgreSQL** — logique structurante non visible dans un inventaire « par écran ». `DefaultAbsenceService`, `sql/013-MA-403`.
6. **Témoignages** (`testimony`/`testimony_attachment`).
7. **Infrastructure de processing statistiques** : crons, workers, file d'attente SQL, eventbus `post-users` (au-delà de la simple « consultation de stats »).
8. **8 widgets de dashboard** distincts (`widget_alerts`, `_forgotten_registers`, `_statements`, `_remarks`, `_absences`, `_day_courses`, `_current_course`, `_day_presences`) — l'« accueil à widgets » E01 en regroupe plusieurs non détaillés.
9. **Initialisation 1D/2D** (`InitController`, `initSettings1D/2D`, `presences1D`) — bascule primaire/secondaire déjà câblée (pertinent pour la cible 1D).
10. **Sniplet `memento`** (fiche élève de viescolaire) intégré à la coquille de vue. `view-src/presences.html`.

---

## 4. Écarts de classification / formulation

- **Granularité hétérogène** : le PM compte « Incidents » et « Statistiques » comme **1-2 features** alors que ce sont des **modules entiers**, mais éclate « Appel » en 11 sous-features. La pondération en points par ligne s'en trouve faussée pour ces deux domaines (sous-comptés).
- **Numérotation** : l'inventaire saute des identifiants (pas de E38/E39 ; E40 isolé) — cohérent avec « 38 features » mais à garder en tête si on recolle au code.
- **« Retards à abandonner » (arbitrage 12/06)** : décision produit claire, mais le code montre un contrôleur retard **dédié** (pas un simple champ), donc l'effort de retrait est réel.
- **« Absences prévisionnelles »** : le PM les classe comme une feature existante à abandonner ; le code suggère qu'elles sont **déjà incomplètes** (route front orpheline, pas de back). À reclasser éventuellement en « déjà non fonctionnelle ».

---

## 5. Points où le code corrobore le PM ✅

- **Legacy AngularJS 1.x, rien en React** : confirmé (pas de couche de compat ; `view-src` = coquille + notifications). Conforte la recommandation « from scratch ».
- **Backend réutilisé dans les deux scénarios** : confirmé — toute la logique métier (registre, absences, alertes, déclarations) est côté Java/SQL/eventbus, indépendante du front AngularJS.
- **Complexité du modèle secondaire** : confirmée par les exemptions/dispenses (logique 2D), incidents/punitions (collège/lycée), regroupements de classes — autant de briques sans objet en primaire.
- **« On ne réutilise facilement que ce qui ne coûte rien à refaire »** : les features triviales (recherche, compteurs) sont effectivement de simples endpoints ; les features lourdes (appel, alertes, stats) portent toute la complexité.
- **Couplage fort comme frein à la refonte** : confirmé et même **plus important qu'indiqué** (cf. ADHERENCES — la donnée absence circule par eventbus entre Présences, Incidents, Massmailing, Statistiques **et** Vie Scolaire).

---

## 6. Recommandations de relecture pour le PM

1. **Re-pondérer le décommissionnement d'Incidents et de Statistiques** : ce sont des modules à part entière (couplage SQL/eventbus, données Mongo), pas des features à 1-2 pts.
2. **Clarifier le statut réel de 3 features** marquées « existantes » : absences prévisionnelles (back manquant), correction d'appel validé (pas d'endpoint), regroupements (codés mais usage non mesuré).
3. **Intégrer à l'inventaire** les 10 éléments du §3 (au minimum pour le périmètre de retrait).
4. **Tracer la dépendance Vie Scolaire/EDT** dès le chiffrage du rebuild : même « from scratch », le nouveau front devra reconsommer les mêmes contrats eventbus/HTTP (cf. ADHERENCES).

> Ces écarts sont établis par lecture du code au 17/06/2026. Les éléments marqués `⚠️` dans les specs
> sources doivent être validés sur fichier avant tout usage normatif/contractuel.
