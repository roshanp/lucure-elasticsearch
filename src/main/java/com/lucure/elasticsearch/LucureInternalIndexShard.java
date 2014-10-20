package com.lucure.elasticsearch;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.aliases.IndexAliasesService;
import org.elasticsearch.index.cache.IndexCache;
import org.elasticsearch.index.cache.filter.ShardFilterCache;
import org.elasticsearch.index.codec.CodecService;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.fielddata.IndexFieldDataService;
import org.elasticsearch.index.fielddata.ShardFieldData;
import org.elasticsearch.index.get.ShardGetService;
import org.elasticsearch.index.indexing.ShardIndexingService;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.merge.scheduler.MergeSchedulerProvider;
import org.elasticsearch.index.percolator.PercolatorQueriesRegistry;
import org.elasticsearch.index.percolator.stats.ShardPercolateService;
import org.elasticsearch.index.query.IndexQueryParserService;
import org.elasticsearch.index.search.stats.ShardSearchService;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.settings.IndexSettingsService;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.service.InternalIndexShard;
import org.elasticsearch.index.store.Store;
import org.elasticsearch.index.suggest.stats.ShardSuggestService;
import org.elasticsearch.index.termvectors.ShardTermVectorService;
import org.elasticsearch.index.translog.Translog;
import org.elasticsearch.index.warmer.ShardIndexWarmerService;
import org.elasticsearch.indices.IndicesLifecycle;
import org.elasticsearch.threadpool.ThreadPool;

/**
 */
public class LucureInternalIndexShard extends InternalIndexShard {

    @Inject
    public LucureInternalIndexShard(
      ShardId shardId, @IndexSettings Settings indexSettings,
      IndexSettingsService indexSettingsService,
      IndicesLifecycle indicesLifecycle, Store store, Engine engine,
      MergeSchedulerProvider mergeScheduler, Translog translog,
      ThreadPool threadPool, MapperService mapperService,
      IndexQueryParserService queryParserService, IndexCache indexCache,
      IndexAliasesService indexAliasesService,
      ShardIndexingService indexingService, ShardGetService getService,
      LucureShardSearchService searchService,
      ShardIndexWarmerService shardWarmerService,
      ShardFilterCache shardFilterCache, ShardFieldData shardFieldData,
      PercolatorQueriesRegistry percolatorQueriesRegistry,
      ShardPercolateService shardPercolateService, CodecService codecService,
      ShardTermVectorService termVectorService,
      IndexFieldDataService indexFieldDataService, IndexService indexService,
      ShardSuggestService shardSuggestService) {
        super(shardId, indexSettings, indexSettingsService, indicesLifecycle,
              store, engine, mergeScheduler, translog, threadPool,
              mapperService, queryParserService, indexCache,
              indexAliasesService, indexingService, getService, searchService,
              shardWarmerService, shardFilterCache, shardFieldData,
              percolatorQueriesRegistry, shardPercolateService, codecService,
              termVectorService, indexFieldDataService, indexService,
              shardSuggestService);
    }
}
