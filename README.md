# Mini-Projet Sécurité Systèmes : Dataset Nextcloud

Ce module est dédié à la génération et à l'analyse de logs pour l'application **Nextcloud** dans le cadre du projet de détection d'attaques.

## 1. Prérequis
- Docker et Docker Compose installés.
- Les ports `8080` (Nextcloud), `9200` (Elasticsearch) et `5601` (Kibana) doivent être libres.
- Python 3 et le module `requests` (`pip install requests`) pour l'exportation finale.

## 2. Déploiement et Configuration de l'Application

1. **Démarrez l'infrastructure :**
   ```bash
   docker-compose up -d
   ```
   
## 2. Finalisez l'installation via l'interface web :
Accédez à http://localhost:8080 et remplissez le formulaire :

- Nom d'utilisateur administrateur : admin_cyber (Note : si le nom "admin" est bloqué par des fichiers préexistants, utilisez admin_cyber).

- Mot de passe : admin_pwd

- Base de données : Sélectionnez MySQL/MariaDB (Database user: nextcloud, Database password: nextcloud_pwd, Database name: nextcloud, Database host: db).

## 3.Activer le logging de sécurité avancé :
Exécutez ces commandes pour configurer Nextcloud afin qu'il génère des logs d'audit complets au format JSON (indispensable pour la détection Brute Force et SQLi) et pour désactiver temporairement la protection anti-bruteforce afin de permettre la génération du dataset :
```bash
docker-compose exec -u 33 nextcloud php occ config:system:set loglevel --value=0 --type=integer
docker-compose exec -u 33 nextcloud php occ config:system:set log_auth_failures --value=true --type=boolean
docker-compose exec -u 33 nextcloud php occ config:system:set logtimezone --value="UTC"
docker-compose exec -u 33 nextcloud php occ config:system:set auth.bruteforce.protection.enabled --value=false --type=boolean
```

## 3. Préparation de l'Outil de Simulation (Gatling)

Afin d'assurer la compatibilité des scripts de simulation Java, construisez une image Docker personnalisée de Gatling :

1. Construisez l'image :
```bash
docker build --network=host -t my-gatling -f Dockerfile.gatling .
```
2. Créez le répertoire de bibliothèques requis par Gatling :
```bash
mkdir -p ./traffic_gen/gatling/user-files/lib
```

## 4. Configuration de l'Ingest Pipeline (Elasticsearch)

Avant de lancer le trafic, configurez Elasticsearch pour qu'il annote automatiquement les logs bruts avec les techniques MITRE ATT&CK associées.

1. Ouvrez Kibana (http://localhost:5601).

2. Naviguez vers Management > Dev Tools.

3. Exécutez la requête suivante pour créer le pipeline nextcloud-mitre :
```JSON
PUT _ingest/pipeline/nextcloud-mitre
{
  "description": "Pipeline pour annoter les logs Nextcloud avec MITRE ATT&CK",
  "processors": [
    {
      "set": {
        "if": "ctx.message != null && ctx.message.contains('Login failed')",
        "field": "mitre.technique.id",
        "value": "T1110"
      }
    },
    {
      "set": {
        "if": "ctx.message != null && (ctx.message.contains('OR 1=1') || ctx.message.contains('UNION SELECT') || ctx.message.contains('DROP TABLE'))",
        "field": "mitre.technique.id",
        "value": "T1190"
      }
    },
    {
      "set": {
        "if": "ctx.message != null && (ctx.message.contains('OR 1=1') || ctx.message.contains('UNION SELECT') || ctx.message.contains('DROP TABLE'))",
        "field": "mitre.technique.name",
        "value": "Exploit Public-Facing Application (SQLi)"
      }
    },
    {
      "set": {
        "if": "ctx.message != null && (ctx.message.contains('OR 1=1') || ctx.message.contains('UNION SELECT') || ctx.message.contains('DROP TABLE'))",
        "field": "mitre.tactic",
        "value": "Initial Access"
      }
    }
  ]
}
```

## 5. Exécution des Scénarios de Trafic:
Ouvrez plusieurs terminaux pour générer simultanément du trafic légitime et des attaques.

**Terminal 1: Traffic Légitime (Baseline)**
Génère une utilisation WebDAV normale en boucle (PROPFIND, PUT, GET, DELETE).
```bash
while true; do
  sudo docker run --rm --network host -v ./traffic_gen/gatling/user-files:/opt/gatling/user-files my-gatling -rm local -s simulations.NormalUsage
  sleep 5
done
```

**Terminal 2 : Attaque par Brute Force (T1110)**
Lancez l'attaque ciblant l'authentification WebDAV.
```bash
sudo docker run --rm --network host -v ./traffic_gen/gatling/user-files:/opt/gatling/user-files my-gatling -rm local -s simulations.BruteForce
```

**Terminal 3 : Attaque par Injection SQL (T1190)**
Lancez l'attaque par injection SQL ciblée sur les endpoints d'authentification WebDAV.
```bash
sudo docker run --rm --network host -v ./traffic_gen/gatling/user-files:/opt/gatling/user-files my-gatling -rm local -s simulations.SqlInjection
```

## 6. Exportation du Dataset Final

Une fois le trafic généré et les logs annotés en temps réel par Elasticsearch, exportez le dataset sous forme de fichier statique .json.

1. Exécutez le script d'extraction Python :
```bash
python3 export_dataset.py
```
2. Le fichier nextcloud-final-dataset.json sera généré à la racine du projet. Ce fichier contient les événements complets, incluant les champs bruts de Nextcloud et les métadonnées MITRE injectées.