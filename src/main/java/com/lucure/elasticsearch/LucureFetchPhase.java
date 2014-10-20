package com.lucure.elasticsearch;

import com.lucure.core.AuthorizationsHolder;
import com.lucure.core.query.AuthQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.search.fetch.FetchPhase;
import org.elasticsearch.search.fetch.explain.ExplainFetchSubPhase;
import org.elasticsearch.search.fetch.fielddata.FieldDataFieldsFetchSubPhase;
import org.elasticsearch.search.fetch.matchedqueries.MatchedQueriesFetchSubPhase;
import org.elasticsearch.search.fetch.partial.PartialFieldsFetchSubPhase;
import org.elasticsearch.search.fetch.script.ScriptFieldsFetchSubPhase;
import org.elasticsearch.search.fetch.source.FetchSourceSubPhase;
import org.elasticsearch.search.fetch.version.VersionFetchSubPhase;
import org.elasticsearch.search.highlight.HighlightPhase;
import org.elasticsearch.search.internal.SearchContext;

/**
 */
public class LucureFetchPhase extends FetchPhase{

    @Inject
    public LucureFetchPhase(
      HighlightPhase highlightPhase,
      ScriptFieldsFetchSubPhase scriptFieldsPhase,
      PartialFieldsFetchSubPhase partialFieldsPhase,
      MatchedQueriesFetchSubPhase matchedQueriesPhase,
      ExplainFetchSubPhase explainPhase, VersionFetchSubPhase versionPhase,
      FetchSourceSubPhase fetchSourceSubPhase,
      FieldDataFieldsFetchSubPhase fieldDataFieldsFetchSubPhase) {
        super(highlightPhase, scriptFieldsPhase, partialFieldsPhase,
              matchedQueriesPhase, explainPhase, versionPhase,
              fetchSourceSubPhase, fieldDataFieldsFetchSubPhase);
    }

    @Override
    public void execute(SearchContext context) {
        //find AuthQuery
        final Query query = context.query();
        if(query instanceof AuthQuery) {
            //TODO: What if this is wrapped?
            final AuthQuery authQuery = (AuthQuery) query;
            AuthorizationsHolder.threadAuthorizations.set(
              new AuthorizationsHolder(authQuery.getAuthorizations()));
        }
        super.execute(context);
    }
}
