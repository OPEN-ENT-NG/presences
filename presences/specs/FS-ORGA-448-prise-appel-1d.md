# Feature Spec FS-ORGA-448 : Prise d'appel 1D

> Projet: Présences (ONE / NEO)
> Statut: Draft - Phase PO
> Version: v1 - 18/08/2026
> Langue: Français
> Périmètre: Autonome (lot 1 de la refonte front de Présences en React)
> PRD parent: PRD_Prise_appel_1D
> Glossaire: Glossaire.md (Présences)
> EPIC Jira: ORGA-448
> Maquettes: [Présences 1D - Wireframes](https://www.figma.com/design/sXAwT8lyIGZ2HinXhEeRJK/Pr%C3%A9sences-1D---Wireframes?node-id=91-282)

---

## 1. Introduction

*Owner: PO*

Cette FS cadre le lot 1 de la refonte du frontend de Présences en React : la page de prise d'appel du premier degré. La refonte se fait page par page, l'application AngularJS existante redirigeant vers la page React au fil des migrations. Cette FS ne couvre donc ni la coquille de navigation à trois onglets, ni les autres écrans du cadrage produit POC_1D, qui feront leurs propres FS. Ce document sert d'artefact central au développement assisté par IA : les agents qui l'implémentent doivent s'y référer au pied de la lettre. Les décisions non tranchées portent un marqueur explicite (`To be decided later`, `Don't know yet`, `Hypothesis to be validated`) et doivent toutes être levées avant le démarrage du développement. Les chapitres Brief design, scénarios de test et Exigences techniques restent à compléter par leurs owners respectifs.

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

Il peut marquer un élève absent, en retard ou en départ. Une absence porte un motif et peut porter une remarque libre. Un retard comme un départ portent un motif et une heure. Un élève en retard ou en départ reste compté présent, ce sont des sous-états de la présence. Les compteurs de l'appel affichent le nombre de présents, d'absents, de retards et de départs. Chaque marquage est enregistré immédiatement, sans action de sauvegarde, et un bouton de validation explicite déclare l'appel terminé.

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
- Un élève en retard ou en départ reste compté présent : ce sont des sous-états de la présence, pas des états à part.
- Un même élève peut porter à la fois un retard et un départ sur la même demi-journée.
- Le motif d'une absence, comme l'heure d'un retard ou d'un départ, peuvent rester vides au moment du marquage et être renseignés plus tard.
- Un marquage dont le motif, ou l'heure, n'est pas encore renseigné est signalé comme tel dans la liste.
- Aucun champ non renseigné ne bloque la validation de l'appel.
- Les compteurs affichent, pour la demi-journée affichée, le nombre de présents, d'absents, de retards et de départs. La somme des présents et des absents fait l'effectif de la classe ; les retards et les départs sont des sous-ensembles des présents.
- Chaque marquage est enregistré immédiatement, sans action de sauvegarde de la part de l'utilisateur.
- La validation est explicite et ne sert pas à sauvegarder : elle déclare l'appel terminé et fait passer le registre à l'état terminé.
- Valider sans avoir marqué personne est une action valide : cela déclare la classe au complet, et ce n'est pas la même chose qu'un appel jamais validé.
- Un appel validé reste distinguable d'un appel en cours ou jamais fait.
- Le système conserve qui a réalisé l'appel, et qui l'a modifié si ce n'est pas la même personne.

**Scénarios de test :** *(à compléter par le Designer)*

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

**Scénarios de test :** *(à compléter par le Designer)*

### US-3: Basculer entre le matin et l'après-midi

**En tant que** enseignant ou directeur-enseignant
**je veux** passer d'une demi-journée à l'autre sur la même journée et voir où j'en suis
**afin de** faire ou vérifier mon appel de l'après-midi sans repasser par l'application existante

**Critères d'acceptation :**

- Le sélecteur permet de passer d'une demi-journée à l'autre sur la même journée.
- Il signale visuellement l'état d'avancement de chaque demi-journée, terminée ou non, pour les classes de l'utilisateur. Indication visuelle uniquement : le contrôle ne change pas de comportement, ne devient pas inactif et ne déclenche rien d'autre.
- Passer d'une demi-journée à l'autre ne perd aucun marquage, puisque tout est déjà enregistré.
- Aucun ordre n'est imposé : l'après-midi est accessible même si le matin n'a pas été validé.

**Scénarios de test :** *(à compléter par le Designer)*

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

**Scénarios de test :** *(à compléter par le Designer)*

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

**Scénarios de test :** *(à compléter par le Designer)*

### US-6: Consulter la fiche d'un élève

**En tant que** enseignant, directeur-enseignant ou directeur déchargé
**je veux** ouvrir la fiche d'un élève de la classe affichée sans quitter mon appel
**afin de** répondre à une question sur son historique d'absences sans perdre ma saisie en cours

**Critères d'acceptation :**

- Depuis la liste, l'utilisateur peut ouvrir la fiche d'un élève de la classe affichée.
- La consultation est en lecture seule : aucun marquage ni aucune justification depuis cet accès.
- Le retour à l'appel ne perd ni les marquages ni la demi-journée affichée.
- La conception de la fiche élève elle-même ne fait pas partie de cette FS.

**Scénarios de test :** *(à compléter par le Designer)*

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

**Scénarios de test :** *(à compléter par le Designer)*

### US-8: Rattraper un appel oublié

**En tant que** enseignant, directeur-enseignant ou directeur déchargé
**je veux** faire l'appel d'une demi-journée passée qui n'a jamais été fait
**afin de** régulariser le registre légal de la classe

**Critères d'acceptation :**

- Un appel jamais fait sur une demi-journée passée s'ouvre vide, tous les élèves présents par défaut, comme un appel courant.
- Le rattrapage suit exactement les mêmes règles de marquage et de validation que l'appel du jour.
- L'écran n'impose aucune limite de délai : l'accès à un appel passé est déterminé par l'application appelante.
- L'appel rattrapé est enregistré sur la demi-journée à laquelle il correspond, pas sur la date du jour.

**Scénarios de test :** *(à compléter par le Designer)*

### US-9: Revenir à l'application existante

**En tant que** enseignant, directeur-enseignant ou directeur déchargé
**je veux** repartir vers l'application existante depuis l'écran de prise d'appel
**afin de** poursuivre ce que je faisais, ou atteindre une classe qui n'est pas rattachée à mon compte

**Critères d'acceptation :**

- Un retour explicite vers l'application existante est disponible en permanence.
- Le retour ne fait rien perdre, puisque les marquages sont enregistrés au fil de l'eau.
- Le retour ne vaut pas validation de l'appel.

**Scénarios de test :** *(à compléter par le Designer)*

---

## 5. Brief design produit UX/UI

*Owner: Design*

*(à compléter par le Designer)*

Éléments d'entrée pour cette passe : les maquettes `DRAFT` du fichier Figma Présences 1D - Wireframes, les annotations de spec par profil qui y figurent (avec leur priorisation indispensable, majeur, normale, mineur, marginale), et les neuf principes de design du chapitre 3, dont le Designer valide la cohérence.

Points d'attention signalés par le PO, sans préjuger de la solution :

- La distinction entre ce qui est actionnable et ce qui ne l'est pas est le principal grief sur l'écran actuel (principe 8).
- Les trois indicateurs de vigilance de US-5 doivent se lire d'un coup d'oeil sans alourdir la liste pour les élèves sans historique. Une piste de badge avec picto calendrier et flèche, plus un tooltip, a déjà été esquissée dans le Figma pour l'absence sur plusieurs jours.
- Le signalement d'un motif ou d'une heure non renseignés (US-1) doit rester lisible sans donner l'impression d'une erreur bloquante, puisque rien ne bloque la validation.
- L'avertissement d'écrasement de US-2 est une règle fonctionnelle, sa forme est à définir.
- L'indication d'avancement portée par le sélecteur de demi-journée (US-3) est visuelle uniquement.

### Décisions UX/UI ouvertes

*(à compléter par le Designer)*

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
- **Brief design, scénarios de test Gherkin et décisions UX/UI ouvertes** : à produire en phase Design.
- **Exigences techniques et contraintes techniques** : à produire en phase Tech.
