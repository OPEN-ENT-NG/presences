# À propos de l'application Présences
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright CGI, Région Nouvelle Aquitaine, Département Seine et Marne
* Développeur(s) : CGI, Edifice
* Financeur(s) : CGI, Région Nouvelle Aquitaine, Département Seine et marne, Mairie De Paris, Edifice
* Description : Présences est un module de vie scolaire qui permet de gérer tous les événements ayant
  lieu au sein de l’établissement scolaire:
  - La gestion des absences et des appels,
  - La gestion des présences,
  - La gestion des incidents, des punitions et des sanctions,
  - Le publipostage de tous ces événements. 

## Développement frontend

### 1 - Initialiser les variables d'environnement

* `./build.sh init`
* Un fichier `.env` minimal est créé.

### 2 - Proxifier le backend avec une URL de recette

* Copier dans `.env` les variables présentes dans le template `env.template` (ou les y coller si le fichier existe déjà).
* Dans le fichier `.env`, renseigner ensuite les champs `VITE_*` avec les tokens d'authentification et les valeurs attendues.

### 3 - Installer les dépendances

* `pnpm i`

### 4 - Lancer le serveur proxifié

Le projet est multi-modules (presences, incidents, massmailing, statistics-presences) : **un serveur Vite ne watch qu'un seul module à la fois**, on ne peut pas naviguer d'un module à l'autre depuis le même serveur. Pour développer sur un autre module, lancer son propre script `dev:<module>` (ils tournent tous sur le port `4200`, donc un seul à la fois).

```bash
pnpm dev:presences     # http://localhost:4200/presences
pnpm dev:incidents     # http://localhost:4200/incidents
pnpm dev:massmailing   # http://localhost:4200/massmailing
pnpm dev:statistics    # http://localhost:4200/statistics-presences
```

### Builder le frontend en local

Chaque module dispose d'un script de build Vite indépendant. Pour builder tous les modules d'un coup :

```bash
NO_DOCKER=true ./build.sh buildFrontend
```

Pour builder un seul module :

```bash
pnpm run build:presences
pnpm run build:incidents
pnpm run build:statistics-presences
pnpm run build:massmailing
```

Les fichiers générés sont déposés dans le dossier `public/` de chaque module :

* `dist/application.js` — bundle principal
* `js/behaviours.js` — bundle behaviours
* `css/<module>.css` — styles compilés

---

## Configuration
Le module présences contient plusieurs modules en son sein : incidents, massmailing, presences et statistics-presences.
Seuls massmailing, presences et statistics-presences contiennent des configurations techniques uniques.

### Massmailing
<pre>
{
  "config": {
    ...
    "mailings": {
        "MAIL": true,
        "PDF": false,
        "SMS": true
    },
    "pdf-generator" : {
        "url" : "$pdfGeneratorUrl",
        "auth" : "$pdfGeneratorAuth"
    }
  }
}

</pre>
Dans votre springboard, vous devez inclure des variables d'environnement :
<pre>
pdfGeneratorAuth=${String}
pdfGeneratorUrl=${String}
</pre>
Il est nécessaire de mettre ***massmailing:true*** dans services du module vie scolaire afin de paramétrer les données de configuration de massmailing.
<pre>
"services": {
     ...
     "massmailing" : true,
     ...
 }
</pre>

### Presences
<pre>
{
"config": {
  ...
  "export-cron": "0 0 0 1/3 * ? *",
  "registers-cron": "0 15,45 7-20 * * ? *",
  "cron-check-regularization" : {
            "enabled": true,
            "cron": "0 0 7,10,13,16,19 ? * * *"
  },
  "mails-list-cron": [],
  "mails-list-export": [],
  ...
  "node-pdf-generator" : {
      "pdf-connector-id": "exportpdf",
      "auth": "${nodePdfToken}",
      "url" : "${nodePdfUri}"
  }
}
</pre>
Dans votre springboard, vous devez inclure des variables d'environnement :
<pre>
nodePdfToken=${String}
nodePdfUri=${String}
</pre>

`"mails-list-cron"` est nécessaire pour l'envoie de mail pour le worker création d'appels

`"mails-list-export"` est nécessaire l'envoie de mail via l'API `/event/archives/export`


Il est nécessaire de mettre ***presences:true*** dans services du module vie scolaire afin de paramétrer les données de configuration de presences.
<pre>
"services": {
     ...
     "presences": true,
     ...
 }
</pre>

Se connecter à l'ENT en tant que Personnel, aller sur Vie Scolaire, choisir une grille horaire d'un établissement, 
aller sur l'onglet Présences dans Vie Scolaire, activer le module et initialiser les paramètres.

### Statistics-presences
<pre>
{
  "config": {
    ...
    "processing-cron": "0 0/30 7-20 * * ? *",
    "report-recipients": [],
    "indicators": [
        "Global",
        "Monthly",
        "Weekly"
    ]
  }
}
</pre>

### Archivage

**A faire cette action avant la transition** (le fichier csv se base sur les groupes et matières)

L'événement `transition` n'a pas encore été implémenté
l'API `/presences/event/archives/export` est utilisé à la place pour récupérer par établissement (`"structureId" params`) les événements, 
générer un fichier CSV et envoyer par mail avec la configuration `"mails-list-export"`
