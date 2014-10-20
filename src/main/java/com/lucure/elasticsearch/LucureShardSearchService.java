package com.lucure.elasticsearch;

import com.lucure.core.query.AuthQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.search.slowlog.ShardSlowLogSearchService;
import org.elasticsearch.index.search.stats.ShardSearchService;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.search.internal.SearchContext;

/**
 */
public class LucureShardSearchService extends ShardSearchService {

    @Inject
    public LucureShardSearchService(
      ShardId shardId, @IndexSettings Settings indexSettings,
      ShardSlowLogSearchService slowLogSearchService) {
        super(shardId, indexSettings, slowLogSearchService);
    }

    @Override
    public void onPreFetchPhase(
      SearchContext searchContext) {
        final Query query = searchContext.query();
        if (query instanceof AuthQuery) {
            ((AuthQuery) query).loadCurrentAuthorizations();
        }
        super.onPreFetchPhase(searchContext);
    }

    @Override
    public void onFreeContext(
      SearchContext context) {
        final Query query = context.query();
        if (query instanceof AuthQuery) {
            ((AuthQuery) query).clearCurrentAuthorizations();
        }
        super.onFreeContext(context);
    }
}
