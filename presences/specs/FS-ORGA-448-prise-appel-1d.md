# Feature Spec FS-ORGA-448 : Prise d'appel 1D

> Projet: Présences (ONE / NEO)
> Statut: Draft - Phase Design
> Version: v2 - 18/08/2026
> Langue: Français
> Périmètre: Autonome (lot 1 de la refonte front de Présences en React)
> PRD parent: PRD_Prise_appel_1D
> Glossaire: Glossaire.md (Présences)
> EPIC Jira: ORGA-448
> Maquettes: [Présences 1D - Wireframes](https://www.figma.com/design/sXAwT8lyIGZ2HinXhEeRJK/Pr%C3%A9sences-1D---Wireframes?node-id=91-282)

**Historique des versions**

| Version | Date | Phase | Auteur | Contenu |
|---|---|---|---|---|
| v1 | 18/08/2026 | PO | Bassam Elkhoury | Chapitres 1 à 4 et contraintes fonctionnelles |
| v2 | 18/08/2026 | Design | Bassam Elkhoury (en l'absence de designer dédié) | Scénarios de test sur les 9 cas d'usage, brief design, décisions UX ouvertes |

---

## 1. Introduction

*Owner: PO*

Cette FS cadre le lot 1 de la refonte du frontend de Présences en React : la page de prise d'appel du premier degré. La refonte se fait page par page, l'application AngularJS existante redirigeant vers la page React au fil des migrations. Cette FS ne couvre donc ni la coquille de navigation à trois onglets, ni les autres écrans du cadrage produit POC_1D, qui feront leurs propres FS. Ce document sert d'artefact central au développement assisté par IA : les agents qui l'implémentent doivent s'y référer au pied de la lettre. Les décisions non tranchées portent un marqueur explicite (`To be decided later`, `Don't know yet`, `Hypothesis to be validated`) et doivent toutes être levées avant le démarrage du développement. Les chapitres Exigences techniques et Contraintes techniques restent à compléter par leur owner.

---

## 2. Problématique

*Owner: PO*

La prise de présence est l'acte central de Présences : c'est le point d'entrée de l'application, et toutes les autres fonctionnalités tournent autour de lui. C'est aussi aujourd'hui le plus mal servi. L'application a été conçue pour le second degré, puis forcée et contournée pour répondre au premier degré, et l'UX comme les fonctionnalités portent encore cette origine.

Concrètement, l'enseignant arrive sur une page d'accueil peu ergonomique, y choisit un créneau d'appel à faire, puis atterrit sur un écran de prise de présence daté et confus. On ne distingue pas les zones cliquables des zones inertes, et la répartition des composants ne guide pas le geste. Trois profils accèdent à cet écran, l'enseignant, le directeur-enseignant qui représente 90 % des directeurs en 1D, et le directeur déchargé quand une correction est nécessaire, sans qu'il soit taillé pour aucun d'eux.

Le geste est répété deux fois par jour, tous les jours de classe, en classe et devant les élèves, dans le temps très court où l'enseignant lance sa séance. Il coûte donc plus de temps et d'attention qu'il ne devrait, alors qu'il est l'un des tout premiers contacts quotidiens des enseignants avec l'ENT. Le besoin est documenté : la prise d'appel est le premier bloc de Présences en importance client dans Productboard, 55 points sur les 279 de l'application.

---

## 3. Aperçu de la solution

*Owner: PO*

L'utilisateur arrive sur la page React depuis l'application AngularJS existante, au moment où un appel doit être fait ou corrigé. L'écran s'ouvre sur l'appel qui lui a été désigné, une classe et une demi-journée, sans qu'aucune sélection préalable soit nécessaire. La liste des élèves de la classe est immédiatement prête à marquer, tous les élèves étant présents par défaut : l'utilisateur ne déclare que les exceptions.

Il peut marquer un élève absent, en retard ou en départ. Une absence porte un motif et peut porter une remarque libre. Un retard comme un départ portent un motif et une heure, cette heure étant pré-remplie avec l'heure du marquage. Un élève en retard ou en départ reste compté présent : ce sont des sous-états de la présence. Les compteurs de l'appel affichent le nombre de présents, d'absents, de retards et de départs. Chaque marquage est enregistré immédiatement, sans action de sauvegarde, et un bouton de validation explicite déclare l'appel terminé.

L'écran signale ce qui mérite l'attention de l'utilisateur : un élève couvert par une déclaration de responsable déjà validée arrive pré-marqué absent, un élève absent au dernier appel de sa classe porte un indicateur d'absence récurrente, et un élève dont une absence sur plusieurs jours a été validée porte un indicateur avec la période concernée. Depuis la liste, la fiche d'un élève est consultable en lecture seule.

Un utilisateur qui a plusieurs classes rattachées dispose d'un sélecteur pour naviguer entre elles et voir l'état de prise d'appel de chacune. Un sélecteur de demi-journée permet de passer du matin à l'après-midi. Une option facultative permet de valider l'appel pour la journée entière en une seule fois, ce qui reste deux demi-journées dans le registre. L'utilisateur peut enfin corriger un appel déjà validé, rattraper un appel oublié d'une demi-journée passée, et revenir à tout moment vers l'application existante.

Sont dans le périmètre de cette FS : l'écran de prise d'appel d'une demi-journée pour une classe, en 1D et sur ordinateur, le marquage et ses motifs, le sélecteur de classes, la bascule matin et après-midi, les compteurs, les trois indicateurs de vigilance, l'accès en lecture à la fiche élève, la sauvegarde au fil de l'eau, la validation explicite, la validation à la journée, la correction d'un appel validé, le rattrapage d'un appel oublié et le retour vers l'application existante. Le hors périmètre est détaillé au chapitre 7.

### Principes de design

*Owner: PO (intentions) - validé par Design*

1. **Le geste le plus court possible.** L'appel de la demi-journée désignée est prêt à marquer dès l'arrivée sur l'écran, sans sélection préalable.
2. **Tous les élèves sont présents par défaut.** L'utilisateur ne déclare que les exceptions.
3. **Rien ne se perd.** Chaque marquage est enregistré immédiatement ; la validation ne sert pas à sauvegarder mais à déclarer l'appel terminé.
4. **L'écran ne demande jamais deux fois la même information.** Une absence déjà déclarée par un responsable et validée arrive pré-marquée.
5. **L'écran signale ce qui mérite l'attention de l'utilisateur** (absence récurrente, absence sur plusieurs jours) sans alourdir la lecture pour les élèves qui n'ont aucun historique.
6. **L'appel affiché est celui qui a été désigné.** L'écran affiche la classe et la demi-journée qui lui sont transmises à l'ouverture, et ne les déduit pas des classes de l'utilisateur connecté. Il reste utilisable pour un utilisateur sans classe rattachée, et pour un appel passé que l'on vient corriger.
7. **Le retour vers l'application existante est toujours possible et explicite.**
8. **On ne doit jamais hésiter sur ce qui est actionnable.** La distinction entre ce qui est cliquable et ce qui ne l'est pas doit être immédiate : c'est le principal grief sur l'écran actuel.
9. **Le confort de saisie ne change pas la donnée légale.** Valider la journée entière d'un coup reste, en base et dans le registre, deux demi-journées.

---

## 4. Cas d'usage

*Owner: PO (cas d'usage + critères fonctionnels) - Owner: Design (scénarios de test Gherkin)*

### US-1: Faire l'appel de la demi-journée désignée

**En tant que** enseignant, directeur-enseignant ou directeur déchargé
**je veux** marquer les exceptions sur la liste de ma classe pour la demi-journée affichée, puis valider
**afin de** déclarer l'assiduité de mes élèves en quelques secondes, sans avoir à chercher ni à confirmer ce qui est déjà l'état normal

**Critères d'acceptation :**

- L'écran s'ouvre sur la classe et la demi-journée désignées, sans sélection préalable de l'utilisateur.
- La liste affiche tous les élèves de la classe, tous marqués présents par défaut.
- L'utilisateur peut marquer un élève absent, et revenir en arrière en le remarquant présent.
- Une absence porte un motif, et peut porter une remarque libre.
- Un retard porte un motif et une heure. Un départ porte un motif et une heure.
- L'heure d'un retard ou d'un départ est pré-remplie avec l'heure du marquage. Elle est modifiable mais ne peut pas être supprimée. En saisie différée, la valeur proposée peut se situer hors de la demi-journée concernée : c'est assumé, l'ajustement est à la main de l'utilisateur.
- Un élève en retard ou en départ reste compté présent : ce sont des sous-états de la présence, pas des états à part.
- Un même élève peut porter à la fois un retard et un départ sur la même demi-journée.
- Seul le motif peut rester vide au moment du marquage et être renseigné plus tard.
- Un marquage dont le motif n'est pas encore renseigné est signalé comme tel dans la liste.
- Aucun motif non renseigné ne bloque la validation de l'appel.
- Les compteurs affichent, pour la demi-journée affichée, le nombre de présents, d'absents, de retards et de départs. La somme des présents et des absents fait l'effectif de la classe ; les retards et les départs sont des sous-ensembles des présents.
- Chaque marquage est enregistré immédiatement, sans action de sauvegarde de la part de l'utilisateur.
- La validation est explicite et ne sert pas à sauvegarder : elle déclare l'appel terminé et fait passer le registre à l'état terminé.
- Valider sans avoir marqué personne est une action valide : cela déclare la classe au complet, et ce n'est pas la même chose qu'un appel jamais validé.
- Un appel validé reste distinguable d'un appel en cours ou jamais fait.
- Le système conserve qui a réalisé l'appel, et qui l'a modifié si ce n'est pas la même personne.

**Scénarios de test :**

```gherkin
Scénario: Ouverture de l'appel désigné
  Étant donné une classe et une demi-journée m'ont été désignées à l'ouverture de l'écran
  Quand l'écran s'affiche
  Alors la classe et la demi-journée désignées DOIVENT être celles affichées
  Et tous les élèves de la classe DOIVENT apparaître marqués présents
  Et je NE DOIS PAS avoir à sélectionner une classe ou une demi-journée pour commencer
```

```gherkin
Scénario: Marquer un élève absent avec son motif
  Étant donné l'appel de la demi-journée est affiché
  Et l'élève est marqué présent
  Quand je le marque absent et que je renseigne un motif
  Alors l'absence et son motif DOIVENT être enregistrés sans action de sauvegarde de ma part
  Et le compteur d'absents DOIT augmenter de un
  Et le compteur de présents DOIT diminuer de un
  Et l'appel NE DOIT PAS être considéré comme terminé
```

```gherkin
Scénario: Annuler un marquage en remarquant l'élève présent
  Étant donné un élève a été marqué absent sur la demi-journée affichée
  Quand je le marque à nouveau présent
  Alors il DOIT redevenir présent
  Et les compteurs DOIVENT refléter immédiatement ce retour en arrière
  Et aucun motif NE DOIT rester attaché à cet élève pour cette demi-journée
```

```gherkin
Scénario: Marquer un retard sans renseigner le motif
  Étant donné l'appel de la demi-journée est affiché
  Quand je marque un élève en retard sans renseigner de motif
  Alors l'heure du retard DOIT être pré-remplie avec l'heure du marquage
  Et l'élève DOIT rester compté présent
  Et le compteur de retards DOIT augmenter de un
  Et le caractère non renseigné du motif DOIT être signalé sur cet élève
  Et la validation de l'appel NE DOIT PAS être empêchée
```

```gherkin
Scénario: Modifier puis tenter de supprimer l'heure d'un retard
  Étant donné un élève est marqué en retard avec une heure pré-remplie
  Quand je remplace cette heure par une autre valeur
  Alors la nouvelle heure DOIT être enregistrée
  Et quand je tente de vider ce champ
  Alors l'heure NE DOIT PAS pouvoir être supprimée
  Et le marquage DOIT conserver une heure à tout moment
```

```gherkin
Scénario: Un même élève en retard puis en départ
  Étant donné un élève a été marqué en retard sur la demi-journée affichée
  Quand je le marque également en départ
  Alors les deux marquages DOIVENT coexister sur cet élève
  Et l'élève DOIT rester compté présent
  Et le compteur de retards comme celui de départs DOIVENT valoir un
  Et le compteur d'absents NE DOIT PAS être modifié
```

```gherkin
Scénario: Valider l'appel sans avoir marqué personne
  Étant donné l'appel de la demi-journée est affiché et aucun élève n'a été marqué
  Quand je valide l'appel
  Alors l'appel DOIT être enregistré comme terminé
  Et la classe DOIT être déclarée au complet
  Et le système NE DOIT PAS refuser la validation au motif qu'aucune saisie n'a été faite
```

```gherkin
Scénario: Distinguer un appel validé d'un appel jamais validé
  Étant donné deux demi-journées de la même classe, l'une validée et l'autre jamais validée
  Quand je consulte l'une puis l'autre
  Alors l'état terminé de la première DOIT être visible
  Et l'absence de validation de la seconde DOIT être visible
  Et les deux NE DOIVENT PAS être présentées de façon identique, même si aucun élève n'y est marqué
```

### US-2: Valider l'appel pour la journée entière en une seule fois

**En tant que** enseignant ou directeur-enseignant
**je veux** valider l'appel pour la journée entière plutôt que demi-journée par demi-journée
**afin de** éviter de refaire le même geste l'après-midi quand la situation de ma classe n'a pas changé

**Critères d'acceptation :**

- L'utilisateur peut choisir de valider la journée entière plutôt que la seule demi-journée affichée.
- L'option est facultative, le mode par défaut reste la validation de la demi-journée.
- La validation de la journée entière vaut validation des deux demi-journées, matin et après-midi.
- Dans le registre et dans les compteurs légaux, cela reste deux demi-journées, jamais une journée unique.
- Seules les absences sont dupliquées sur la seconde demi-journée, avec leur motif.
- Un retard n'est pas dupliqué : il reste attaché à la demi-journée où il a été saisi, et l'élève y reste présent.
- Un départ n'est pas dupliqué en tant que départ : il produit une absence sur la seconde demi-journée, le motif du départ étant repris comme motif de cette absence.
- Un élève parti en cours de matinée compte donc une demi-journée de présence avec départ, et une demi-journée d'absence.
- Si la seconde demi-journée a déjà été saisie ou déjà validée, l'utilisateur est averti que la validation à la journée écrasera l'appel existant.
- S'il confirme, la validation à la journée s'applique et écrase les marquages de la seconde demi-journée : la dernière validation fait foi.
- Sans saisie existante sur la seconde demi-journée, aucun avertissement n'est nécessaire.

**Scénarios de test :**

```gherkin
Scénario: Valider la journée entière sur une seconde demi-journée vierge
  Étant donné l'appel du matin est affiché avec des élèves marqués absents et leurs motifs
  Et l'appel de l'après-midi de la même journée n'a fait l'objet d'aucune saisie
  Quand je valide l'appel pour la journée entière
  Alors les deux demi-journées DOIVENT être validées
  Et chaque élève marqué absent le matin DOIT être également absent l'après-midi, avec le même motif
  Et le registre DOIT comptabiliser deux demi-journées, jamais une journée unique
```

```gherkin
Scénario: Un retard n'est pas dupliqué sur la seconde demi-journée
  Étant donné un élève est marqué en retard sur l'appel du matin
  Quand je valide l'appel pour la journée entière
  Alors le retard DOIT rester attaché à la seule demi-journée du matin
  Et l'élève DOIT rester compté présent le matin
  Et aucun retard NE DOIT être créé sur l'après-midi
```

```gherkin
Scénario: Un départ produit une absence sur la seconde demi-journée
  Étant donné un élève est marqué en départ sur l'appel du matin, avec un motif
  Quand je valide l'appel pour la journée entière
  Alors l'élève DOIT rester compté présent le matin, avec son départ
  Et il DOIT être compté absent l'après-midi
  Et le motif du départ DOIT être repris comme motif de cette absence
  Et aucun départ NE DOIT être créé sur l'après-midi
```

```gherkin
Scénario: Renoncer à écraser une seconde demi-journée déjà saisie
  Étant donné l'appel du matin est affiché
  Et l'appel de l'après-midi de la même journée a déjà été saisi ou validé
  Quand je demande la validation de la journée entière
  Alors je DOIS être averti que l'appel de l'après-midi existe déjà et sera écrasé
  Et quand je renonce
  Alors aucun marquage de l'après-midi NE DOIT être modifié
  Et l'appel du matin DOIT rester dans l'état où il était
```

```gherkin
Scénario: Confirmer l'écrasement d'une seconde demi-journée déjà saisie
  Étant donné j'ai été averti que l'appel de l'après-midi existe déjà et sera écrasé
  Quand je confirme la validation de la journée entière
  Alors les marquages de l'après-midi DOIVENT être remplacés par ceux issus du matin
  Et les deux demi-journées DOIVENT être validées
  Et l'appel de l'après-midi NE DOIT PAS conserver de marquage antérieur à cette confirmation
```

### US-3: Basculer entre le matin et l'après-midi

**En tant que** enseignant ou directeur-enseignant
**je veux** passer d'une demi-journée à l'autre sur la même journée et voir où j'en suis
**afin de** faire ou vérifier mon appel de l'après-midi sans repasser par l'application existante

**Critères d'acceptation :**

- Le sélecteur permet de passer d'une demi-journée à l'autre sur la même journée.
- Il signale visuellement l'état d'avancement de chaque demi-journée, terminée ou non, pour les classes de l'utilisateur. Indication visuelle uniquement : le contrôle ne change pas de comportement, ne devient pas inactif et ne déclenche rien d'autre.
- Passer d'une demi-journée à l'autre ne perd aucun marquage, puisque tout est déjà enregistré.
- Aucun ordre n'est imposé : l'après-midi est accessible même si le matin n'a pas été validé.

**Scénarios de test :**

```gherkin
Scénario: Passer d'une demi-journée à l'autre sans perdre sa saisie
  Étant donné j'ai marqué des élèves sur l'appel du matin sans le valider
  Quand je bascule sur l'après-midi puis reviens sur le matin
  Alors tous mes marquages du matin DOIVENT être présents
  Et aucune confirmation de sauvegarde NE DOIT m'être demandée lors de la bascule
```

```gherkin
Scénario: Signalement d'une demi-journée entièrement terminée
  Étant donné toutes les classes qui me sont rattachées ont un appel validé pour le matin
  Quand je consulte le sélecteur de demi-journée
  Alors l'achèvement du matin DOIT être signalé
  Et le contrôle DOIT rester utilisable exactement comme avant
  Et il NE DOIT PAS devenir inactif ni déclencher une action différente
```

```gherkin
Scénario: Accéder à l'après-midi avant d'avoir validé le matin
  Étant donné l'appel du matin n'est pas validé
  Quand je bascule sur l'après-midi
  Alors l'appel de l'après-midi DOIT être accessible et modifiable
  Et le système NE DOIT PAS imposer de valider le matin d'abord
```

### US-4: Naviguer entre mes classes

**En tant que** enseignant ou directeur-enseignant rattaché à plusieurs classes
**je veux** passer d'une de mes classes à l'autre et voir l'état de prise d'appel de chacune
**afin de** enchaîner mes appels sans repasser par l'application existante, et repérer celui qui manque

**Critères d'acceptation :**

- Le sélecteur de classes n'apparaît que si l'utilisateur a plusieurs classes rattachées.
- Il ne liste que les classes rattachées à l'utilisateur.
- Il indique l'état de prise d'appel de chacune pour la demi-journée affichée.
- Changer de classe conserve la demi-journée affichée.
- Pour un utilisateur sans classe rattachée, aucun sélecteur n'est affiché et l'écran reste utilisable sur l'appel désigné.

**Scénarios de test :**

```gherkin
Scénario: Utilisateur rattaché à plusieurs classes
  Étant donné je suis rattaché à trois classes
  Quand l'appel s'affiche
  Alors un sélecteur DOIT me proposer mes trois classes
  Et l'état de prise d'appel de chacune pour la demi-journée affichée DOIT être visible
  Et aucune classe à laquelle je ne suis pas rattaché NE DOIT apparaître dans ce sélecteur
```

```gherkin
Scénario: Changer de classe conserve la demi-journée
  Étant donné l'appel de l'après-midi d'une de mes classes est affiché
  Quand je sélectionne une autre de mes classes
  Alors l'appel affiché DOIT être celui de l'après-midi de cette autre classe
  Et la demi-journée NE DOIT PAS revenir au matin ni à la demi-journée en cours
```

```gherkin
Scénario: Utilisateur rattaché à une seule classe
  Étant donné je suis rattaché à une seule classe
  Quand l'appel s'affiche
  Alors aucun sélecteur de classe NE DOIT être présenté
  Et l'appel de ma classe DOIT être directement prêt à marquer
```

```gherkin
Scénario: Utilisateur sans classe rattachée
  Étant donné je n'ai aucune classe rattachée et un appel m'a été désigné à l'ouverture
  Quand l'écran s'affiche
  Alors l'appel désigné DOIT être affiché et modifiable
  Et aucun sélecteur de classe NE DOIT être présenté
  Et l'écran NE DOIT PAS afficher d'erreur ni de liste vide
```

### US-5: Repérer les élèves qui méritent mon attention

**En tant que** enseignant, directeur-enseignant ou directeur déchargé
**je veux** voir directement dans la liste les élèves dont la situation sort de l'ordinaire
**afin de** ne pas ressaisir une absence déjà connue, et repérer une absence qui s'installe

**Critères d'acceptation :**

- Un élève couvert par une déclaration d'absence d'un responsable, validée, sur la demi-journée affichée, apparaît déjà marqué absent, avec l'indication que l'absence est déclarée et validée.
- L'utilisateur n'a pas à la ressaisir, et peut la modifier comme un marquage ordinaire.
- Un élève absent lors du dernier appel de cette classe porte un indicateur d'absence récurrente.
- Un élève dont une absence couvrant plusieurs jours a été validée par le directeur porte un indicateur d'absence sur plusieurs jours, avec la période concernée, du premier au dernier jour.
- Ces indicateurs sont des signaux de lecture : hormis la pré-saisie décrite ci-dessus, ils ne préremplissent et n'empêchent aucun marquage.

**Scénarios de test :**

```gherkin
Scénario: Élève couvert par une déclaration de responsable validée
  Étant donné une déclaration d'absence d'un responsable a été validée pour un élève sur la demi-journée affichée
  Quand l'appel s'affiche
  Alors cet élève DOIT apparaître déjà marqué absent
  Et le caractère déclaré et validé de cette absence DOIT être visible
  Et je NE DOIS PAS avoir à saisir cette absence moi-même
```

```gherkin
Scénario: Modifier une absence issue d'une déclaration validée
  Étant donné un élève apparaît pré-marqué absent au titre d'une déclaration validée
  Quand je le marque présent
  Alors le marquage DOIT être modifiable comme n'importe quel autre
  Et les compteurs DOIVENT refléter immédiatement ce changement
  Et le système NE DOIT PAS refuser la modification au motif que l'absence était déclarée
```

```gherkin
Scénario: Indicateur d'absence récurrente
  Étant donné un élève était absent lors du dernier appel de cette classe
  Quand l'appel de la demi-journée s'affiche
  Alors un indicateur d'absence récurrente DOIT être visible sur cet élève
  Et cet indicateur NE DOIT PAS préremplir son marquage sur la demi-journée affichée
```

```gherkin
Scénario: Indicateur d'absence sur plusieurs jours
  Étant donné une absence couvrant plusieurs jours a été validée par le directeur pour un élève, et la demi-journée affichée est dans cette période
  Quand l'appel s'affiche
  Alors un indicateur d'absence sur plusieurs jours DOIT être visible sur cet élève
  Et la période concernée, du premier au dernier jour, DOIT être connaissable depuis cet indicateur
```

```gherkin
Scénario: Élève sans historique particulier
  Étant donné un élève n'a aucune absence au dernier appel, aucune déclaration validée et aucune absence sur plusieurs jours en cours
  Quand l'appel s'affiche
  Alors aucun indicateur de vigilance NE DOIT apparaître sur cet élève
  Et sa ligne DOIT rester lisible comme celle de n'importe quel élève présent
```

### US-6: Consulter la fiche d'un élève

**En tant que** enseignant, directeur-enseignant ou directeur déchargé
**je veux** ouvrir la fiche d'un élève de la classe affichée sans quitter mon appel
**afin de** répondre à une question sur son historique d'absences sans perdre ma saisie en cours

**Critères d'acceptation :**

- Depuis la liste, l'utilisateur peut ouvrir la fiche d'un élève de la classe affichée.
- La consultation est en lecture seule : aucun marquage ni aucune justification depuis cet accès.
- Le retour à l'appel ne perd ni les marquages ni la demi-journée affichée.
- La conception de la fiche élève elle-même ne fait pas partie de cette FS.

**Scénarios de test :**

```gherkin
Scénario: Ouvrir la fiche d'un élève en lecture seule
  Étant donné l'appel de la demi-journée est affiché
  Quand j'ouvre la fiche d'un élève de cette classe
  Alors son historique d'absences DOIT être consultable
  Et aucune action de marquage ou de justification NE DOIT être proposée depuis cette fiche
```

```gherkin
Scénario: Revenir à l'appel après consultation
  Étant donné j'ai marqué des élèves sans valider l'appel, puis ouvert la fiche d'un élève
  Quand je reviens à l'appel
  Alors tous mes marquages DOIVENT être présents
  Et la demi-journée et la classe affichées DOIVENT être celles que je consultais
```

```gherkin
Scénario: Fiche limitée aux élèves de la classe affichée
  Étant donné l'appel d'une classe est affiché
  Quand je cherche à consulter la fiche d'un élève
  Alors seuls les élèves de la classe affichée DOIVENT être accessibles depuis cet écran
  Et aucun élève d'une autre classe NE DOIT être atteignable depuis la liste d'appel
```

### US-7: Corriger un appel déjà validé

**En tant que** enseignant sur ses propres appels, directeur-enseignant ou directeur déchargé
**je veux** reprendre un appel déjà validé pour en modifier les marquages, puis le revalider
**afin de** corriger une erreur ou intégrer une information arrivée après l'appel, sans créer de doublon

**Critères d'acceptation :**

- L'appel s'ouvre avec ses marquages existants.
- L'utilisateur peut modifier, ajouter ou retirer des marquages, puis revalider.
- Un enseignant peut corriger ses propres appels, un directeur peut corriger un appel déjà validé.
- Le système conserve qui a réalisé l'appel et qui l'a corrigé, et distingue une correction faite par un profil direction d'une correction faite par l'enseignant lui-même.
- Le nom de la personne ayant réalisé la validation, et le cas échéant celui de la personne ayant réalisé la dernière correction, sont visibles sur l'appel.
- La correction met à jour le même registre, elle ne crée pas un second appel.

**Scénarios de test :**

```gherkin
Scénario: Un enseignant corrige son propre appel validé
  Étant donné j'ai validé l'appel d'une de mes classes
  Quand je rouvre cet appel, modifie un marquage et revalide
  Alors l'appel DOIT refléter la modification
  Et l'appel DOIT rester à l'état terminé après la revalidation
  Et le système NE DOIT PAS créer un second appel pour cette demi-journée
```

```gherkin
Scénario: Un directeur corrige un appel validé par un enseignant
  Étant donné un appel a été validé par un enseignant
  Quand un directeur en modifie un marquage et revalide
  Alors la modification DOIT être enregistrée
  Et la trace DOIT distinguer une correction faite par un profil direction d'une correction faite par l'enseignant lui-même
  Et l'auteur initial de l'appel NE DOIT PAS être remplacé par le correcteur
```

```gherkin
Scénario: Visibilité de l'auteur de la validation et de la dernière correction
  Étant donné un appel a été validé puis corrigé par une autre personne
  Quand je consulte cet appel
  Alors le nom de la personne ayant validé l'appel DOIT être visible
  Et le nom de la personne ayant réalisé la dernière correction DOIT être visible
```

```gherkin
Scénario: Retirer un marquage lors d'une correction
  Étant donné un appel validé comporte un élève marqué absent
  Quand je le remarque présent et revalide
  Alors l'absence NE DOIT plus être comptée pour cette demi-journée
  Et les compteurs DOIVENT refléter la correction
  Et aucune absence orpheline NE DOIT subsister sur cet élève pour cette demi-journée
```

### US-8: Rattraper un appel oublié

**En tant que** enseignant, directeur-enseignant ou directeur déchargé
**je veux** faire l'appel d'une demi-journée passée qui n'a jamais été fait
**afin de** régulariser le registre légal de la classe

**Critères d'acceptation :**

- Un appel jamais fait sur une demi-journée passée s'ouvre vide, tous les élèves présents par défaut, comme un appel courant.
- Le rattrapage suit exactement les mêmes règles de marquage et de validation que l'appel du jour.
- L'écran n'impose aucune limite de délai : l'accès à un appel passé est déterminé par l'application appelante.
- L'appel rattrapé est enregistré sur la demi-journée à laquelle il correspond, pas sur la date du jour.

**Scénarios de test :**

```gherkin
Scénario: Ouvrir un appel jamais fait sur une demi-journée passée
  Étant donné une demi-journée passée d'une classe n'a jamais fait l'objet d'un appel
  Quand cet appel m'est désigné et que l'écran s'affiche
  Alors tous les élèves DOIVENT apparaître marqués présents
  Et l'appel DOIT être modifiable et validable comme un appel du jour
  Et l'écran NE DOIT PAS refuser l'accès au motif que la demi-journée est passée
```

```gherkin
Scénario: Le rattrapage s'enregistre sur la demi-journée concernée
  Étant donné je rattrape l'appel d'une demi-journée passée
  Quand je marque des élèves et valide
  Alors les marquages DOIVENT être enregistrés sur la demi-journée rattrapée
  Et aucun marquage NE DOIT être enregistré sur la demi-journée du jour
```

```gherkin
Scénario: Heure par défaut d'un retard saisi en différé
  Étant donné je rattrape l'appel d'une demi-journée passée
  Quand je marque un élève en retard
  Alors l'heure proposée DOIT être l'heure du marquage, même si elle est hors de la demi-journée concernée
  Et cette heure DOIT être modifiable
  Et elle NE DOIT PAS pouvoir être supprimée
```

### US-9: Revenir à l'application existante

**En tant que** enseignant, directeur-enseignant ou directeur déchargé
**je veux** repartir vers l'application existante depuis l'écran de prise d'appel
**afin de** poursuivre ce que je faisais, ou atteindre une classe qui n'est pas rattachée à mon compte

**Critères d'acceptation :**

- Un retour explicite vers l'application existante est disponible en permanence.
- Le retour ne fait rien perdre, puisque les marquages sont enregistrés au fil de l'eau.
- Le retour ne vaut pas validation de l'appel.

**Scénarios de test :**

```gherkin
Scénario: Revenir sans perdre sa saisie
  Étant donné j'ai marqué des élèves sans valider l'appel
  Quand je reviens à l'application existante
  Alors mes marquages DOIVENT être conservés
  Et l'appel NE DOIT PAS être considéré comme validé
```

```gherkin
Scénario: Le retour est toujours disponible
  Étant donné l'écran de prise d'appel est affiché, que l'appel soit validé ou non
  Quand je cherche à revenir à l'application existante
  Alors le retour DOIT être disponible
  Et il NE DOIT PAS être conditionné à la validation de l'appel
```

---

## 5. Brief design produit UX/UI

*Owner: Design*

**Avertissement sur cette passe.** Elle a été portée par le PM en l'absence de designer dédié sur la squad. Les scénarios de test du chapitre 4 sont volontairement **agnostiques des patterns d'interaction** : ils décrivent ce que l'utilisateur fait et observe, jamais comment il le fait. Aucun bouton, aucune modale, aucun survol, aucun libellé n'y est nommé. Les scénarios restent donc valides quelle que soit la forme retenue, et le Lead Product Designer conserve l'entière latitude sur les patterns, les composants et les états visuels. En contrepartie, les décisions listées ci-dessous ne sont pas tranchées.

**Intentions retenues et éléments d'entrée.** Les maquettes de référence sont les frames `DRAFT` du fichier Figma Présences 1D - Wireframes, dont les annotations portent déjà une priorisation par profil (indispensable, majeur, normale, mineur, marginale) qui a servi à établir le périmètre de cette FS. Les neuf principes de design du chapitre 3 sont les intentions à respecter.

Points d'attention pour la passe design, sans préjuger de la solution :

- La distinction entre ce qui est actionnable et ce qui ne l'est pas est le principal grief sur l'écran actuel (principe 8). C'est le premier critère de réussite visuelle de cet écran.
- La liste doit rester lisible pour une classe entière dont la quasi-totalité des élèves est présente : l'exception doit ressortir, la normalité ne doit pas occuper d'espace.
- Les trois indicateurs de vigilance de US-5 doivent se lire d'un coup d'oeil sans alourdir les lignes des élèves sans historique. Une piste de badge avec picto calendrier et flèche, plus un tooltip explicatif, a déjà été esquissée dans le Figma pour l'absence sur plusieurs jours.
- Le signalement d'un motif non renseigné (US-1) doit rester lisible sans donner l'impression d'une erreur bloquante, puisque rien ne bloque la validation.
- L'heure d'un retard ou d'un départ est toujours renseignée et ne peut pas être vidée : le traitement visuel doit rendre l'édition évidente sans suggérer la suppression.
- L'avertissement d'écrasement de US-2 est une règle fonctionnelle. Sa forme est à définir, avec deux exigences : la conséquence doit être explicite avant confirmation, et le renoncement doit être aussi accessible que la confirmation.
- L'indication d'avancement portée par le sélecteur de demi-journée (US-3) est visuelle uniquement : elle ne doit ni désactiver le contrôle ni suggérer qu'il est devenu inopérant.
- Le sélecteur de classes (US-4) apparaît et disparaît selon le nombre de classes rattachées : les deux états, avec et sans sélecteur, doivent être traités comme deux mises en page valides, pas comme un état dégradé.
- Le retour vers l'application existante (US-9) doit être permanent et ne jamais être confondu avec la validation.

### Décisions UX/UI ouvertes

- **Patterns d'interaction du marquage** : geste par élève, saisie du motif, saisie de l'heure. `To be decided later` par le Lead Product Designer.
- **Forme des trois indicateurs de vigilance** de US-5, et articulation entre eux quand un même élève en porte plusieurs. `To be decided later`
- **Forme du signalement d'un motif non renseigné**. `To be decided later`
- **Forme de l'avertissement d'écrasement** de US-2. `To be decided later`
- **Forme de l'indication d'achèvement** sur le sélecteur de demi-journée. `To be decided later`
- **Traitement des noms de valideur et de correcteur** de US-7 : emplacement et niveau de mise en avant. `To be decided later`
- **Conformité au design system** : les maquettes n'ont pas été confrontées à la librairie de référence sur ce cycle. `Hypothesis to be validated`

---

## 6. Exigences techniques

*Owner: Tech lead*

*(à compléter par le Tech lead)*

Avis macro du PO, à valider et approfondir en phase Tech :

- **Point d'entrée et retour** : la redirection depuis l'application AngularJS vers la page React, avec transmission de l'appel désigné (classe et demi-journée), et un retour vers l'application appelante.
- **Encapsulation** : la page doit être un composant sans coquille propre, pour devenir plus tard un onglet de la future page d'accueil sans réécriture.
- **Contrat d'interface** : le back est du legacy Java Vert.x sans test unitaire métier sur le registre, avec un `RegistryController` monolithique, et le front actuel dépend de globaux `window.item` et `window.structure`. Un contrat d'interface explicite est donc un prérequis, sinon les payloads seront inventés.
- **Données consommées, à confirmer disponibles** : les déclarations de responsables validées couvrant la demi-journée affichée, l'information qu'un élève était absent au dernier appel de sa classe, et la plage de dates d'une absence validée sur plusieurs jours. `Hypothesis to be validated`
- **Traçabilité** : qui a réalisé l'appel, qui l'a corrigé, et la distinction entre une correction direction et une correction enseignant. Le champ `counsellor_input` existe déjà pour cela sur `register` et sur `event`.
- **Écriture au fil de l'eau** : chaque marquage déclenche une écriture, avec un point d'attention sur le volume de requêtes et sur le comportement en cas de perte de réseau en classe.
- **Duplication à la journée** : la règle de US-2 transforme un départ en absence sur la seconde demi-journée. La transformation est fonctionnelle, son implémentation côté serveur ou côté client reste à trancher.
- **Sécurité** : le filtre `RegistryRight` ne regarde pas la requête et ne compare pas la structure présente dans l'URL avec celles de l'utilisateur. Les nouveaux points d'entrée ne doivent pas reproduire ce schéma.
- **Prérequis d'outillage** : une recette locale exécutable de la stack ENT que l'agent sait déployer lui-même, et des mocks front pour découpler le développement du back.
- **Dépendances avec d'autres FS** : la coquille de navigation à trois onglets et la vue direction listant l'état des appels de toutes les classes, toutes deux à venir.

---

## 7. Contraintes

### Contraintes fonctionnelles

*Owner: PO*

Hors périmètre de cette FS :

- La coquille de navigation à trois onglets, l'accueil, la page Appels et le Registre, qui feront leurs propres FS.
- La vue direction listant l'état des appels de toutes les classes de l'école.
- La conception de la fiche élève.
- L'appel d'urgence et l'évacuation.
- Le mobile, déjà refondu.
- L'initialisation et le paramétrage : motifs, seuils, templates.
- Le second degré.
- La déclaration d'absence côté parent, la validation des déclarations par le directeur, les notifications, le publipostage, les alertes et les statistiques.
- L'affichage des autres enseignants de la classe.
- Le mécanisme de redirection depuis l'application AngularJS, qui relève du chapitre 6.

Règles métier bloquantes :

- La donnée légale reste comptée en demi-journées, quel que soit le confort de saisie offert.
- L'écran n'invente jamais l'appel affiché : la classe et la demi-journée lui sont désignées à l'ouverture.
- Un appel non validé n'est pas un appel fait, même si tous les élèves sont présents par défaut.
- Un retard et un départ sont des sous-états d'une présence, jamais des états d'absence, sur la demi-journée où ils sont saisis.
- L'heure d'un retard ou d'un départ ne peut jamais être vide.
- En 1D, un appel correspond à une demi-journée entière, EDT ne posant que deux créneaux par jour.
- En cas de conflit sur la validation à la journée, la dernière validation fait foi, après avertissement de l'utilisateur.

Dépendances hors de cette FS :

- Les déclarations d'absence des responsables et leur validation par le directeur, produites ailleurs, dont cette FS ne consomme que le résultat.
- EDT, source des deux créneaux de la journée et des classes.
- L'application AngularJS existante, à la fois point d'entrée et cible du retour.

### Contraintes techniques

*Owner: Tech lead*

*(à compléter par le Tech lead)*

### Questions ouvertes

- **Les réglages `allow_multiple_slots` et `split_slot` sont-ils sans effet en 1D ?** L'appel correspondant à une demi-journée entière, ils sont supposés sans objet. Mais ils sont lus par le code actuel, `allow_multiple_slots` vaut `true` par défaut pour toutes les structures (migration 040-MA-817) et rien ne garantit qu'ils n'influencent pas le regroupement ou l'affichage. À confirmer par la Tech. `Hypothesis to be validated`
- **Les trois données consommées par US-5 sont-elles disponibles en base et exposables ?** Déclarations de responsables validées sur la demi-journée, absence de l'élève au dernier appel de sa classe, plage de dates d'une absence multi-jours validée. À confirmer par la Tech. `Hypothesis to be validated`
- **Les décisions UX/UI listées au chapitre 5** restent à trancher par le Lead Product Designer, la passe Design ayant été portée par le PM.
- **Exigences techniques et contraintes techniques** : à produire en phase Tech.
