package com.lucure.elasticsearch;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.lucure.core.query.AuthQuery;
import com.lucure.core.security.Authorizations;
import com.lucure.core.security.Constants;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.IndexQueryParserService;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.index.query.QueryParser;
import org.elasticsearch.index.query.QueryParsingException;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;

import java.io.IOException;

/**
 * Query parser for JSON Queries.
 */
public class LucureQueryParser implements QueryParser {

    public static final String NAME = "restricted";
    public static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    @Inject
    public LucureQueryParser(IndicesQueriesRegistry queriesRegistry) {
        queriesRegistry.addQueryParser(this);
    }

    @Override
    public String[] names() {
        return new String[]{NAME};
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException,
                                                              QueryParsingException {
        XContentParser parser = parseContext.parser();

        XContentParser.Token token = parser.nextToken();
        if (token != XContentParser.Token.FIELD_NAME) {
            throw new QueryParsingException(parseContext.index(), "[lucure] query malformed");
        }
        String fieldName = parser.currentName();
        Authorizations authorizations = Authorizations.EMPTY;
        if("auth".equals(fieldName)) {
            parser.nextToken();
            final String[] auths_str = Iterables
              .toArray(COMMA_SPLITTER.split(parser.text()), String.class);
            if(auths_str.length != 0) {
                authorizations = new Authorizations(auths_str);
            }
            parser.nextToken();
            fieldName = parser.currentName();
        }

        if (!"query".equals(fieldName)) {
            throw new QueryParsingException(parseContext.index(), "[lucure] query malformed");
        }
        parseContext.reset(parser);
        Query result = parseContext.parseInnerQuery();
        parser.nextToken();

        return new AuthQuery(result, authorizations);
    }
}
