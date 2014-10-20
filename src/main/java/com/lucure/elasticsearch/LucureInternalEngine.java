package com.lucure.elasticsearch;

import com.lucure.core.index.LucureIndexSearcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.codec.CodecService;
import org.elasticsearch.index.deletionpolicy.SnapshotDeletionPolicy;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.engine.EngineException;
import org.elasticsearch.index.engine.internal.InternalEngine;
import org.elasticsearch.index.indexing.ShardIndexingService;
import org.elasticsearch.index.merge.policy.MergePolicyProvider;
import org.elasticsearch.index.merge.scheduler.MergeSchedulerProvider;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.settings.IndexSettingsService;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.similarity.SimilarityService;
import org.elasticsearch.index.store.Store;
import org.elasticsearch.index.translog.Translog;
import org.elasticsearch.indices.warmer.IndicesWarmer;
import org.elasticsearch.threadpool.ThreadPool;

public class LucureInternalEngine extends InternalEngine {

    @Inject
    public LucureInternalEngine(
      ShardId shardId, @IndexSettings Settings indexSettings,
      ThreadPool threadPool, IndexSettingsService indexSettingsService,
      ShardIndexingService indexingService, @Nullable IndicesWarmer warmer,
      Store store, SnapshotDeletionPolicy deletionPolicy, Translog translog,
      MergePolicyProvider mergePolicyProvider,
      MergeSchedulerProvider mergeScheduler, AnalysisService analysisService,
      SimilarityService similarityService, CodecService codecService)
      throws EngineException {
        super(shardId, indexSettings, threadPool, indexSettingsService,
              indexingService, warmer, store, deletionPolicy, translog,
              mergePolicyProvider, mergeScheduler, analysisService,
              similarityService, codecService);
    }

    @Override
    protected Searcher newSearcher(
      String source, IndexSearcher searcher, SearcherManager manager) {
        return super.newSearcher(source, new LucureIndexSearcher(searcher), manager);
    }
}
