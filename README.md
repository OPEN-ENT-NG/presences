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
