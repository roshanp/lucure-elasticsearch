package com.lucure.elasticsearch;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.lucure.core.index.DelegatingRestrictedFieldVisitor;
import com.lucure.core.index.LucureIndexSearcher;
import com.lucure.core.security.ColumnVisibility;
import com.lucure.core.security.VisibilityEvaluator;
import com.lucure.elasticsearch.plugin.LucureRestrictedMapper;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.AlreadyClosedException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.codec.CodecService;
import org.elasticsearch.index.deletionpolicy.SnapshotDeletionPolicy;
import org.elasticsearch.index.engine.EngineException;
import org.elasticsearch.index.engine.internal.InternalEngine;
import org.elasticsearch.index.fieldvisitor.FieldsVisitor;
import org.elasticsearch.index.indexing.ShardIndexingService;
import org.elasticsearch.index.mapper.internal.SourceFieldMapper;
import org.elasticsearch.index.mapper.internal.UidFieldMapper;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.lucure.core.AuthorizationsHolder.threadAuthorizations;
import static org.apache.lucene.index.StoredFieldVisitor.Status.YES;

public class LucureInternalEngine extends InternalEngine {

    private static final Function<AtomicReaderContext, SourceBuildingIndexReader>
      WRAP_ATOMIC_READER_FUNCTION =
      new Function<AtomicReaderContext, SourceBuildingIndexReader>() {
          @Override
          public SourceBuildingIndexReader apply(
            AtomicReaderContext atomicReaderContext) {
              return new SourceBuildingIndexReader(atomicReaderContext.reader());
          }
      };

    private static final Function<IndexReader, IndexReader> WRAP_READER_FUNCTION =
      new Function<IndexReader, IndexReader>() {
          @Override
          public IndexReader apply(IndexReader indexReader) {
              if (indexReader instanceof SourceBuildingIndexReader) {
                  return indexReader;
              }

              if(indexReader instanceof AtomicReader) {
                  return new SourceBuildingIndexReader((AtomicReader) indexReader);
              }

              try {
                  CompositeReader compositeReader
                    = (CompositeReader) indexReader;
                  List<SourceBuildingIndexReader> lucureReaders = Lists
                    .transform(compositeReader.leaves(),
                               WRAP_ATOMIC_READER_FUNCTION);
                  return new MultiReader(lucureReaders.toArray(
                    new SourceBuildingIndexReader[lucureReaders.size()]));
              } catch (AlreadyClosedException e) {
                  // ignore
              }
              return new MultiReader(new SourceBuildingIndexReader[0]);
          }
      };

    static class SourceBuildingDelegatingFieldsVisitor extends DelegatingRestrictedFieldVisitor {

        private Map<String, List<Map<String, Object>>> fieldsValues;

        SourceBuildingDelegatingFieldsVisitor(
          StoredFieldVisitor fieldsVisitor) {
            super(fieldsVisitor);
        }

        @Override
        public void binaryField(
          FieldInfo fieldInfo, byte[] value, ColumnVisibility columnVisibility)
          throws IOException {
            if(!SourceFieldMapper.NAME.equals(fieldInfo.name)) {
                addValue(fieldInfo.name, value, columnVisibility);
                super.binaryField(fieldInfo, value, columnVisibility);
            }
        }

        @Override
        public void stringField(
          FieldInfo fieldInfo, String value, ColumnVisibility columnVisibility)
          throws IOException {
            if(!UidFieldMapper.NAME.equals(fieldInfo.name)) {
                addValue(fieldInfo.name, value, columnVisibility);
            }
            if(super.needsField(fieldInfo, columnVisibility) == YES) {
                super.stringField(fieldInfo, value, columnVisibility);
            }
        }

        @Override
        public void intField(
          FieldInfo fieldInfo, int value, ColumnVisibility columnVisibility)
          throws IOException {
            addValue(fieldInfo.name, value, columnVisibility);
            if(super.needsField(fieldInfo, columnVisibility) == YES) {
                super.intField(fieldInfo, value, columnVisibility);
            }
        }

        @Override
        public void longField(
          FieldInfo fieldInfo, long value, ColumnVisibility columnVisibility)
          throws IOException {
            addValue(fieldInfo.name, value, columnVisibility);
            if(super.needsField(fieldInfo, columnVisibility) == YES) {
                super.longField(fieldInfo, value, columnVisibility);
            }
        }

        @Override
        public void floatField(
          FieldInfo fieldInfo, float value, ColumnVisibility columnVisibility)
          throws IOException {
            addValue(fieldInfo.name, value, columnVisibility);
            if(super.needsField(fieldInfo, columnVisibility) == YES) {
                super.floatField(fieldInfo, value, columnVisibility);
            }
        }

        @Override
        public void doubleField(
          FieldInfo fieldInfo, double value, ColumnVisibility columnVisibility)
          throws IOException {
            addValue(fieldInfo.name, value, columnVisibility);
            if(super.needsField(fieldInfo, columnVisibility) == YES) {
                super.doubleField(fieldInfo, value, columnVisibility);
            }
        }

        @Override
        public Status needsField(
          FieldInfo fieldInfo, ColumnVisibility columnVisibility)
          throws IOException {
            if(SourceFieldMapper.NAME.equals(fieldInfo.name)) {
                return Status.NO;
            }
            return YES;
        }

        void addValue(String name, Object value, ColumnVisibility columnVisibility) {
            if (fieldsValues == null) {
                fieldsValues = newHashMap();
            }

            List<Map<String, Object>> values = fieldsValues.get(name);
            if (values == null) {
                values = new ArrayList<>(2);
                fieldsValues.put(name, values);
            }
            values.add(produceVisibilityMap(value, columnVisibility));
        }

        private Map<String, Object> produceVisibilityMap(Object object, ColumnVisibility columnVisibility) {
            Map<String, Object> visMap = newHashMap();
            visMap.put(LucureRestrictedMapper.FIELD_VAL, object);
            visMap.put(LucureRestrictedMapper.FIELD_VIS, new String(columnVisibility.getExpression()));
            return visMap;
        }

        public BytesReference produceSource() throws IOException {
            if(fieldsValues == null) {
                return null; //no fields added
            }

            XContentBuilder builder = XContentFactory
              .contentBuilder(XContentType.JSON);
            builder.startObject();
            for(Map.Entry<String, List<Map<String, Object>>> entry : fieldsValues.entrySet()) {
                    builder.field(entry.getKey());
                    List<Map<String, Object>> value = entry.getValue();
                    if (value == null || value.size() == 0) {
                        builder.nullValue();
                    } else {
                        if(value.size() == 1) {
                            builder.map(value.get(0));
                        } else {
                            builder.value(value);
                        }
                    }
            }
            builder.endObject();
            return builder.bytes();
        }
    }

    private static class SourceBuildingIndexReader extends FilterAtomicReader {

        public static final FieldInfo SOURCE_FIELD_INFO = new FieldInfo(
          SourceFieldMapper.NAME, false, 0, false, false, false,
          FieldInfo.IndexOptions.DOCS_ONLY, FieldInfo.DocValuesType.BINARY,
          FieldInfo.DocValuesType.BINARY, 0l, null);

        private SourceBuildingIndexReader(AtomicReader in) {
            super(in);
        }

        @Override
        public void document(
          int docID, StoredFieldVisitor visitor) throws IOException {
            //We can assume at this layer the visitor is a FieldsVisitor
            final SourceBuildingDelegatingFieldsVisitor sourceBuildingDelegatingFieldsVisitor
              = new SourceBuildingDelegatingFieldsVisitor(visitor);
            super.document(docID, sourceBuildingDelegatingFieldsVisitor);

            if(visitor.needsField(SOURCE_FIELD_INFO) == YES) {
                final BytesReference bytesReference
                  = sourceBuildingDelegatingFieldsVisitor.produceSource();
                if(bytesReference != null) {
                    visitor
                      .binaryField(SOURCE_FIELD_INFO, bytesReference.toBytes());
                }
            }
        }
    }

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
        return super.newSearcher(source, new LucureIndexSearcher(
          WRAP_READER_FUNCTION.apply(searcher.getIndexReader())), manager);
    }
}
