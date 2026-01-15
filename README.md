# Mini-Projet Sécurité Systèmes : Dataset Nextcloud

Ce module est dédié à la génération et à l'analyse de logs pour l'application **Nextcloud** dans le cadre du projet de détection d'attaques.

## 1. Description de l'Application
- **Nom** : Nextcloud
- **Architecture** : SOFEA (Frontend JS / Backend PHP API)
- **Image Docker** : `nextcloud:fpm-alpine`
- **Rôle** : Plateforme de collaboration et stockage de fichiers.

## 2. Architecture de Déploiement
Le déploiement repose sur une stack Docker composée de :
* **Nextcloud (App)** : Coeur de l'application.
* **MariaDB** : Base de données pour les utilisateurs et métadonnées.
* **Nginx** : Serveur web / Reverse Proxy.
* **Redis** : Gestion du cache et des verrous de fichiers.
* **Filebeat** : Agent de collecte des logs (`/var/www/html/data/nextcloud.log`).

## 3. Configuration des Logs
Pour garantir un dataset exploitable scientifiquement, la configuration suivante est appliquée dans `config.php` :
```php
'log_type' => 'file',
'logfile' => '/var/www/html/data/nextcloud.log',
'loglevel' => 0, // Mode DEBUG pour capturer tous les événements
'log_query' => true,
```

## 4. Scénarios d'Utilisation
### 4.1 Trafic Légitime (Normal)
Exécution de tests de charge via Gatling et Cypress :

- Navigation dans l'arborescence de fichiers.

- Upload/Download de documents (PDF, Images, Archives).

- Partage de fichiers entre utilisateurs internes.

- Synchronisation via client WebDAV.

### 4.2 Scénarios d'Attaques (Malveillants)

## 5. Pipline de Données (Elastic Stack)

1. **Ingestion** : Filebeat transmet les logs JSON à Logstash.

2. **Filtrage** : Logstash parse le JSON et ajoute les champs de métadonnées.

3. **Mapping MITRE CAR** : Transformation des champs (ex: remoteAddr -> source.ip).

4. **Visualisation** : Dashboards Kibana pour identifier les pics d'erreurs 4xx/5xx.

## 6. Modélisation Graphe (Neo4j)

Les logs sont exportés pour alimenter un graphe de relations :

- Nœuds : User, IP, File, Action.

- Relations :

    - (User)-[:LOGGED_FROM]->(IP)

    - (User)-[:PERFORMED]->(Action)-[:ON]->(File)

## 7. Comment reproduire le Dataset

**Pré-requis**

- Docker & Docker Compose

- Python 3.x (pour les scripts d'attaque)

- Node.js (pour Cypress)

**Installation**
```bash
# Lancer les conteneurs
docker-compose up -d

# Initialiser l'application
# (Configuration admin via http://localhost:8080)

# Lancer la génération de trafic normal
cd tests/load-testing && ./run-gatling.sh

# Lancer les simulations d'attaques
cd tests/security && python3 brute_force_sim.py
```

## 8. Livrables inclus
**docker-compose.yml** : Configuration de la stack.

**scripts/** : Scripts de génération de trafic et d'attaques.

**logs/** : Dataset brut et annoté (format MITRE CAR).

**neo4j/** : Scripts Cypher pour l'importation des données.