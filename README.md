# Lucure ElasticSearch Plugin

## Description

The Lucure ElasticSearch Plugin allows field level visibilities on the ElasticSearch index using [Lucure](https://github.com/roshanp/lucure-core). 

## Prerequisites

1. Download and build [Lucure-Core](https://github.com/roshanp/lucure-core)
2. ElasticSearch 1.3.4

## QuickStart

1. Build Lucure ElasticSearch
2. Unzip target/releases/lucure-elasticsearch-{version}.zip into {ES_HOME}/plugins/lucure-elasticsearch
3. Open config/elasticsearch.yml, make sure the plugin.mandatory is set with lucure:
	`plugin.mandatory: lucure`
4. Start ElasticSearch: `/bin/elasticsearch`
5. Create the 'restrictedtest' index with the following command: <pre>	
curl -XPUT http://localhost:9200/restrictedtest -d '{
		"settings" : {		
			"index" : {
			"codec" : "Lucure",
			"engine.type" : "com.lucure.elasticsearch.plugin.LucureEngineElasticSearchModule"
	}
  },
 "mappings" : {
  "_default_" : {
   "_source" : {"enabled" : false},
   "dynamic_templates": [
                { "en": {
                      "match":              "*", 
                      "mapping": {
                          "type":           "restricted",
                          "store":  true
                      }
                }}
            ]
  }
 }
}
';`</pre>
6. Add a document with field visibilities:<pre>
curl -XPUT 'http://localhost:9200/restrictedtest/employee/1' -d '{
    "isActive": true,
    "balance": "$2,814.39",
    "age": {
      "val": 26,
      "vis": "U"
    },
    "eyeColor": {
      "val": "blue",
      "vis": "U&FOUO"
    },
    "name": {
      "val": "Janet Rojas",
      "vis": "U&FOUO"
    }
  }'
</pre>
7. Run a few queries
	<pre>
	Should return document:		
	curl -XPOST 'localhost:9200/restrictedtest/_search?pretty&auth=U' -d '{ "fields" : ["*"], "query": { "restricted": { "auth": "U,FOUO", "query": { "range": { "age": { "gte" : 10, "lte" : 30 } }}} }}'		</pre>
	<pre>
	Should return document:
	curl -XPOST 'localhost:9200/restrictedtest/_search?pretty&auth=U' -d '{ "fields" : ["*"], "query": { "restricted": { "auth": "U,FOUO", "query": { "term": { "eyeColor": "blue"}}} }}'	
	</pre>
	<pre>
	Should not return document:
	curl -XPOST 'localhost:9200/restrictedtest/_search?pretty&auth=U' -d '{ "fields" : ["*"], "query": { "restricted": { "auth": "U", "query": { "term": { "eyeColor": "blue"}}} }}'	
	</pre>
	<pre>
	Should return part of the document:
	curl -XPOST 'localhost:9200/restrictedtest/_search?pretty&auth=U' -d '{ "fields" : ["*"], "query": { "restricted": { "auth": "U", "query": { "term": { "age": 26}}} }}'
	</pre>