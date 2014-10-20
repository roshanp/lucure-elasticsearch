package com.lucure.elasticsearch.plugin;

import com.lucure.core.RestrictedField;
import com.lucure.core.security.ColumnVisibility;
import junit.framework.TestCase;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.ParsedDocument;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class LucureRestrictedMapperTest extends TestCase {

    @Test
    public void testRestrictedData() throws Exception {
        // default to normalize
        String mapping = XContentFactory.jsonBuilder().startObject()
                                        .startObject("type")
                                        .startObject("properties")
                                        .startObject("restricteddata")
                                        .field("type", "restricted").endObject()
                                        .endObject().endObject().endObject()
                                        .string();

        final DocumentMapperParser mapperParser = MapperTestUtils.newParser();
        mapperParser
          .putTypeParser("restricted", new LucureRestrictedMapper.TypeParser());
        DocumentMapper defaultMapper = mapperParser.parse(mapping);

        ParsedDocument doc = defaultMapper.parse("type", "1",
                                                 XContentFactory.jsonBuilder()
                                                                .startObject()
                                                                .startObject(
                                                                  "restricteddata")
                                                                .field("val",
                                                                       "data")
                                                                .field("vis",
                                                                       "U&FOUO")
                                                                .endObject()
                                                                .endObject()
                                                                .bytes());

        assertThat(doc.rootDoc().get("restricteddata"), equalTo("data"));
        final IndexableField restricteddata = doc.rootDoc()
                                                 .getField("restricteddata");
        assertTrue(restricteddata instanceof RestrictedField);
        RestrictedField restrictedField = (RestrictedField) restricteddata;
        assertEquals(new ColumnVisibility("U&FOUO"),
                     restrictedField.getColumnVisibility());
    }

    @Test
    public void testRestrictedDataNumericField() throws Exception {
        // default to normalize
        String mapping = XContentFactory.jsonBuilder().startObject()
                                        .startObject("type")
                                        .startObject("properties")
                                        .startObject("restricteddata")
                                        .field("type", "restricted")
                                        .field("valuetype", "float")
                                        .endObject()
                                        .endObject().endObject().endObject()
                                        .string();

        final DocumentMapperParser mapperParser = MapperTestUtils.newParser();
        mapperParser
          .putTypeParser("restricted", new LucureRestrictedMapper.TypeParser());
        DocumentMapper defaultMapper = mapperParser.parse(mapping);

        ParsedDocument doc = defaultMapper.parse("type", "1",
                                                 XContentFactory.jsonBuilder()
                                                                .startObject()
                                                                .startObject(
                                                                  "restricteddata")
                                                                .field("val",
                                                                       10.0)
                                                                .field("vis",
                                                                       "U&FOUO")
                                                                .endObject()
                                                                .endObject()
                                                                .bytes());

        final IndexableField restricteddata = doc.rootDoc()
                                                 .getField("restricteddata");
        assertTrue(restricteddata instanceof RestrictedField);
        RestrictedField restrictedField = (RestrictedField) restricteddata;
        assertEquals(new ColumnVisibility("U&FOUO"),
                     restrictedField.getColumnVisibility());
        assertEquals(10.0f, restrictedField.numericValue());
    }

    @Test
    public void testRestrictedDataDefaultVisibility() throws Exception {
        // default to normalize
        String mapping = XContentFactory.jsonBuilder().startObject()
                                        .startObject("type")
                                        .startObject("properties")
                                        .startObject("restricteddata")
                                        .field("type", "restricted")
                                        .field("defaultvis", "U")
                                        .endObject()
                                        .endObject().endObject().endObject()
                                        .string();

        final DocumentMapperParser mapperParser = MapperTestUtils.newParser();
        mapperParser
          .putTypeParser("restricted", new LucureRestrictedMapper.TypeParser());
        DocumentMapper defaultMapper = mapperParser.parse(mapping);

        ParsedDocument doc = defaultMapper.parse("type", "1",
                                                 XContentFactory.jsonBuilder()
                                                                .startObject()
                                                                .startObject(
                                                                  "restricteddata")
                                                                .field("val",
                                                                       "data")
                                                                .endObject()
                                                                .endObject()
                                                                .bytes());

        assertThat(doc.rootDoc().get("restricteddata"), equalTo("data"));
        final IndexableField restricteddata = doc.rootDoc()
                                                 .getField("restricteddata");
        assertTrue(restricteddata instanceof RestrictedField);
        RestrictedField restrictedField = (RestrictedField) restricteddata;
        assertEquals(new ColumnVisibility("U"),
                     restrictedField.getColumnVisibility());
    }
}