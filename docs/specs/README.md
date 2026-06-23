# Spécifications métier — Application Présences (OPEN-ENT-NG/presences)

Documentation métier **extraite du code** (rétro-ingénierie) du mono-repo `presences`, à des fins
d'onboarding, QA, refonte et conformité. Générée le **2026-06-17** par lecture du code
(front AngularJS 1.x + back Java/Vert.x), sources tracées `fichier:ligne`.

## Périmètre

Mono-repo Maven multi-modules :

| Module | Rôle métier | Persistance |
| ------ | ----------- | ----------- |
| `presences/` | **Cœur** : appel/registre, absences & événements, retards, départs, remarques, exemptions, présences positives, oublis de cahier, absences collectives, alertes/assiduité, fiche élève/calendrier, déclarations parent, paramétrage | PostgreSQL `presences.*` + Mongo `courses` (lu) |
| `incidents/` | Incidents, punitions, sanctions, comportements (collège/lycée) | PostgreSQL `incidents.*` + Mongo `presences.punishments` |
| `massmailing/` | Publipostage / relances de masse (mail, SMS, PDF) | PostgreSQL `massmailing.*` |
| `statistics-presences/` | Indicateurs & statistiques d'assiduité (Global, Monthly, Weekly) | Mongo `presences.statistics*` + file SQL |
| `common/` | Code Java **partagé** : modèles, enums, helpers, clients eventbus inter-apps | — |

## Documents

| Fichier | Contenu | Lire en priorité si… |
| ------- | ------- | -------------------- |
| [BUSINESS_RULES.md](BUSINESS_RULES.md) | Règles métier par feature (validations, conditions, calculs) | vous implémentez/testez un comportement |
| [DATA_MODEL.md](DATA_MODEL.md) | Entités, champs, relations (SQL / Mongo / jsonschema / modèles) | vous touchez au schéma ou à une migration |
| [WORKFLOWS.md](WORKFLOWS.md) | Flux numérotés (déclencheur, états, étapes, erreurs) | vous cartographiez un parcours utilisateur |
| [PERMISSIONS.md](PERMISSIONS.md) | Droits, rôles, `@SecuredAction` / `@ResourceFilter`, conditions front | vous travaillez sécurité / habilitations |
| [API_CONTRACTS.md](API_CONTRACTS.md) | Endpoints HTTP + eventbus, mappés aux services Angular | vous appelez/refondez une API |
| [DIFFERENCES_PM.md](DIFFERENCES_PM.md) | **Écarts** entre le code et l'état des lieux du PM (Confluence ODE/5570822164) | vous chiffrez/arbitrez la refonte vs rebuild |
| [ADHERENCES_VIESCOLAIRE_EDT.md](ADHERENCES_VIESCOLAIRE_EDT.md) | **Adhérences** avec Vie Scolaire et Emploi du temps (EDT) | vous évaluez le couplage inter-apps |
| [VERIFICATION.md](VERIFICATION.md) | Relecture sur fichier des points `⚠️` prioritaires (confirmés / corrigés / infirmés) | vous voulez le niveau de fiabilité d'une règle sensible |

## Comment exploiter cette documentation

### Selon votre rôle
- **Onboarding (nouveau dev/PM)** : commencer par ce README (périmètre + modules), puis `WORKFLOWS.md`
  pour la vue d'ensemble des parcours, puis `BUSINESS_RULES.md` du module qui vous concerne.
- **QA / rédaction de tests** : `BUSINESS_RULES.md` (chaque règle = un cas de test candidat) croisé avec
  `PERMISSIONS.md` (matrice de droits à couvrir) et `API_CONTRACTS.md` (contrats à valider).
- **Décision refonte vs rebuild (PM)** : `DIFFERENCES_PM.md` (§1 synthèse + §6 recommandations) puis
  `ADHERENCES_VIESCOLAIRE_EDT.md` §E (impact sur le rebuild).
- **Architecte / refonte technique** : `DATA_MODEL.md` + `API_CONTRACTS.md` + `ADHERENCES_*` (les
  contrats eventbus/HTTP à préserver même en repartant de zéro côté front).

### Niveau de confiance d'une affirmation
1. **Source `fichier:ligne` sans marqueur** → relue directement, fiable.
2. **`⚠️ À confirmer`** → issue d'agents d'exploration ou ambiguë ; n° de ligne indicatif. **Ne pas
   traiter comme normatif** sans relecture.
3. **Mention « vérifié » ou renvoi à `VERIFICATION.md §Vx`** → relu sur fichier dans cette passe (statut
   ✅ confirmé / ✏️ corrigé / ❌ infirmé).

Avant tout usage contractuel d'une règle marquée `⚠️`, ouvrir le fichier source cité et valider — ou
demander une passe de vérification ciblée (cf. `VERIFICATION.md` pour le format).

### Maintenir à jour
Ces specs sont un **instantané du code au 17/06/2026**. Pour les régénérer après évolution :
la skill `pm-toolkit:extract-business-docs` rejoue l'extraction ; pensez à relancer une passe de
vérification (`VERIFICATION.md`) sur les règles modifiées. Conserver la convention de sourçage
`fichier:ligne` et le marqueur `⚠️`.

## Notes transverses importantes

- **Pas de refonte React en cours dans ce repo** : `view-src/` ne contient que la coquille de vue
  AngularJS (`presences.html`) et des templates de notification timeline (`notify/`). Le routing est
  100 % AngularJS (`public/ts/app.ts`, `$routeProvider`). Il n'y a donc **pas** de table de
  redirections legacy→React à documenter — le rebuild cible partirait bien « from scratch ».
- **Couplage inter-apps fort** : la donnée d'absence circule par eventbus entre Présences, Incidents,
  Massmailing, Statistiques **et** Vie Scolaire (boucle bidirectionnelle). Voir `ADHERENCES_*`.
- **Corrections issues de la vérification** : `register.split_slot` est `DEFAULT false` (et non `true`) ;
  les déclarations parent validées **ne créent pas** d'absence automatiquement ; pas de réouverture d'un
  appel validé ; bug `DayOfWeek` (jeudi manquant). Détails dans `VERIFICATION.md`.
