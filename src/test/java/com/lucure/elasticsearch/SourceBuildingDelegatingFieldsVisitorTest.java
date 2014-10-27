package com.lucure.elasticsearch;

import com.lucure.core.security.ColumnVisibility;
import org.apache.lucene.index.FieldInfo;
import org.elasticsearch.index.fieldvisitor.AllFieldsVisitor;
import org.junit.Test;

import static com.lucure.elasticsearch.LucureInternalEngine
  .SourceBuildingDelegatingFieldsVisitor;
import static org.junit.Assert.assertEquals;

public class SourceBuildingDelegatingFieldsVisitorTest {

    @Test
    public void testSourceBuild() throws Exception {
        final SourceBuildingDelegatingFieldsVisitor visitor
          = new SourceBuildingDelegatingFieldsVisitor(new AllFieldsVisitor());
        visitor.stringField(fieldInfoFactory("field1"), "val11", new ColumnVisibility("A&B"));
        visitor.stringField(fieldInfoFactory("field1"), "val12", new ColumnVisibility("A&B"));
        visitor.stringField(fieldInfoFactory("field2"), "val2", new ColumnVisibility("A"));

        assertEquals("{\"field2\":{\"val\":\"val2\",\"vis\":\"A\"}," +
                     "\"field1\":[{\"val\":\"val11\",\"vis\":\"A&B\"}," +
                     "{\"val\":\"val12\",\"vis\":\"A&B\"}]}", new String(visitor.produceSource().toBytes()));
    }

    private FieldInfo fieldInfoFactory(String fieldName) {
        return new FieldInfo(fieldName, true, 0, true, true, true,
                                          FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS,
                                          FieldInfo.DocValuesType.SORTED_SET,
                                          FieldInfo.DocValuesType.SORTED_SET, 0l, null);
    }
}