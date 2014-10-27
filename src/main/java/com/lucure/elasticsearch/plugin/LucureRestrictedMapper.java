package com.lucure.elasticsearch.plugin;

import com.google.common.collect.Iterables;
import com.lucure.core.RestrictedField;
import com.lucure.core.security.ColumnVisibility;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.fielddata.FieldDataType;
import org.elasticsearch.index.mapper.*;
import org.elasticsearch.index.mapper.core.AbstractFieldMapper;
import org.elasticsearch.index.query.QueryParseContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.mapper.core.TypeParsers.parseField;

/**
 * TODO: Have to support non-string types
 */
public class LucureRestrictedMapper extends AbstractFieldMapper<Object> {

    public static final String FIELD_VIS = "vis";
    public static final String FIELD_VAL = "val";
    private static ESLogger logger = ESLoggerFactory.getLogger(LucureRestrictedMapper.class.getName());

    public static final String CONTENT_TYPE = "restricted";
    public static final ColumnVisibility EMPTY_VISIBILITY =
      new ColumnVisibility();

    public static class Defaults {
        public static final ContentPath.Type PATH_TYPE = ContentPath.Type.FULL;
    }

    public static class Builder extends AbstractFieldMapper.Builder<Builder, LucureRestrictedMapper> {

        public static final FieldType FIELD_TYPE = new FieldType();

        static {
            FIELD_TYPE.setIndexed(true);
            FIELD_TYPE.setTokenized(true);
            FIELD_TYPE.setStored(true);
            FIELD_TYPE.setStoreTermVectors(false);
            FIELD_TYPE.setOmitNorms(false);
            FIELD_TYPE.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            FIELD_TYPE.freeze();
        }

        private ContentPath.Type pathType = Defaults.PATH_TYPE;

        private final Mapper.Builder restrictedBuilder;
        private final ColumnVisibility defaultVisibility;

        public Builder(String name, Mapper.Builder builder, String defaultVis) {
            super(name, new FieldType(FIELD_TYPE));
            this.builder = this;
            this.restrictedBuilder = builder;
            this.defaultVisibility = defaultVis != null ?
                                     new ColumnVisibility(defaultVis) :
                                     EMPTY_VISIBILITY;
        }

        public Builder pathType(ContentPath.Type pathType) {
            this.pathType = pathType;
            return this;
        }

        @Override
        public LucureRestrictedMapper build(BuilderContext context) {
            ContentPath.Type origPathType = context.path().pathType();
            context.path().pathType(pathType);

            // create the content mapper under the actual name
            Mapper restrictedMapper = restrictedBuilder.build(context);
            context.path().pathType(origPathType);

            return new LucureRestrictedMapper(buildNames(context), pathType,
                                              restrictedMapper,
                                              fieldType,
                                              defaultVisibility,
                                              multiFieldsBuilder
                                                .build(this, context), copyTo);
        }
    }

    /**
     * type:
     *
     * field1: {
     *     "type" : "restricted",
     *     "valuetype" : "string", #defaults to string
     *     "defaultvis" : "A&B"    #default visibility
     * }
     */
    public static class TypeParser implements Mapper.TypeParser {

        public static final String TYPE_DEFAULT_VIS = "defaultvis";
        public static final String TYPE_VALUETYPE = "valuetype";

        private Mapper.Builder<?, ?> findMapperBuilder(Map<String, Object> propNode, String propName, ParserContext parserContext) {
            String type;
            Object typeNode = propNode.get(TYPE_VALUETYPE);
            if (typeNode != null) {
                type = typeNode.toString();
            } else {
                type = "string";
            }
            Mapper.TypeParser typeParser = parserContext.typeParser(type);
            return typeParser.parse(propName, (Map<String, Object>) propNode, parserContext);
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public Mapper.Builder parse(String name, Map<String, Object> node, ParserContext parserContext) throws MapperParsingException {

            final Object defaultvis = node.get(TYPE_DEFAULT_VIS);
            final Builder builder =
              new Builder(name, findMapperBuilder(node, name, parserContext),
                          defaultvis != null ? defaultvis.toString() : null);
            parseField(builder, name, node, parserContext);
            return builder;
        }
    }

    private final ContentPath.Type pathType;

    private final Mapper restrictedMapper;
    private final ColumnVisibility defaultVisibility;

    public LucureRestrictedMapper(
      Names names, ContentPath.Type pathType, Mapper restrictedMapper,
      FieldType fieldType, ColumnVisibility defaultVisibility, MultiFields multiFields, CopyTo copyTo) {
        super(names, 1.0f, fieldType, false, null, null, null, null, null, null, null,
                ImmutableSettings.EMPTY, multiFields, copyTo);
        this.pathType = pathType;
        this.restrictedMapper = restrictedMapper;
        this.defaultVisibility = defaultVisibility;
    }

    @Override
    public Object value(Object value) {
        return null;
    }

    @Override
    public FieldType defaultFieldType() {
        return fieldType;
    }

    @Override
    public FieldDataType defaultFieldDataType() {
        return null;
    }

    @Override
    protected void parseCreateField(ParseContext context, List<Field> fields) throws IOException {
        XContentParser parser = context.parser();
        XContentParser.Token token = parser.currentToken();
        if (token == XContentParser.Token.VALUE_STRING ||
            token == XContentParser.Token.VALUE_BOOLEAN ||
            token == XContentParser.Token.VALUE_NUMBER) {
            if (fieldType.indexed() || fieldType.stored()) {
                final IndexableField indexableField = parseMapperField(context);
                Field field = new RestrictedField(names.indexName(), RestrictedField.toObject(
                  indexableField), fieldType, defaultVisibility);
                field.setBoost(boost);
                fields.add(field);
            }
        } else if (token == XContentParser.Token.START_OBJECT) {
            /**
             * Expecting:
             * {
             *  val : value
             *  vis : "FieldVisibility"
             * }
             */
            token = parser.nextToken();
            if (token != XContentParser.Token.FIELD_NAME && !FIELD_VAL
              .equals(parser.currentName())) {
                throw new MapperParsingException("failed to parse [" + names.fullName() + "]. Expecting 'val' field");
            }
            parser.nextToken();
            final IndexableField first = parseMapperField(context);
            ColumnVisibility cv = defaultVisibility;
            if(first != null) {
                token = parser.nextToken();
                if(token == XContentParser.Token.FIELD_NAME && FIELD_VIS
                  .equals(parser.currentName())) {
                    token = parser.nextToken();
                    if(token == XContentParser.Token.VALUE_STRING) {
                        cv = new ColumnVisibility(parser.text());
                    }
                    parser.nextToken();
                } else if (token != XContentParser.Token.END_OBJECT) {
                    parser.nextToken();
                }

                if(parser.currentToken() != XContentParser.Token.END_OBJECT) {
                    throw new MapperParsingException("failed to parse [" + names.fullName() + "]. Expecting end of object field");
                }

                if (fieldType.indexed() || fieldType.stored()) {
                    //TODO: Is it ok to ignore the fieldType from the field
                    Field field = new RestrictedField(name(), RestrictedField.toObject(first), fieldType, cv);
                    field.setBoost(boost);
                    fields.add(field);
                }
            }
        }
    }

    private IndexableField parseMapperField(ParseContext context)
      throws IOException {
        ParseContext.Document valDoc
          = new ParseContext.Document();
        final ParseContext.Document restoreDoc = context.switchDoc(valDoc);
        restrictedMapper.parse(context);
        valDoc = context.switchDoc(restoreDoc);
        return Iterables.getFirst(valDoc, null);
    }

    @Override
    public void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
        // ignore this for now
    }

    @Override
    public void traverse(ObjectMapperListener objectMapperListener) {
    }

    @Override
    public void close() {
        restrictedMapper.close();
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public boolean useTermQueryWithQueryString() {
        return true;
    }

}