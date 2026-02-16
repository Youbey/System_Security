import requests
import json

# URL de ton index Elasticsearch (Filebeat)
url = "http://localhost:9200/filebeat-*/_search?size=10000"

# Requête pour ne récupérer QUE les logs de Nextcloud
query = {
    "query": {
        "match": {
            "service.name": "nextcloud"
        }
    }
}

print("Extraction des logs depuis Elasticsearch...")
try:
    response = requests.get(url, json=query)
    response.raise_for_status()
    data = response.json()

    # On extrait uniquement le contenu utile (_source)
    dataset = [hit["_source"] for hit in data["hits"]["hits"]]

    # Sauvegarde dans le fichier final
    output_file = "nextcloud-final-dataset.json"
    with open(output_file, "w") as f:
        json.dump(dataset, f, indent=4)

    print(f"Succès ! {len(dataset)} événements exportés dans {output_file}")

except Exception as e:
    print(f"Erreur de connexion à Elasticsearch : {e}")