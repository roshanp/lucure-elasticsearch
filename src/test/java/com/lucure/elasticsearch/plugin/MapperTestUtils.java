package com.lucure.elasticsearch.plugin;

import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.codec.docvaluesformat.DocValuesFormatService;
import org.elasticsearch.index.codec.postingsformat.PostingsFormatService;
import org.elasticsearch.index.fielddata.IndexFieldDataService;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.index.similarity.SimilarityLookupService;
import org.elasticsearch.indices.analysis.IndicesAnalysisModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.indices.fielddata.breaker.NoneCircuitBreakerService;

/**
 *
 */
public class MapperTestUtils {

    public static DocumentMapperParser newParser() {
        return new DocumentMapperParser(new Index("test"), ImmutableSettings.Builder.EMPTY_SETTINGS, newAnalysisService(), new PostingsFormatService(new Index("test")),
                new DocValuesFormatService(new Index("test")), newSimilarityLookupService(), null);
    }

    public static DocumentMapperParser newParser(Settings indexSettings) {
        return new DocumentMapperParser(new Index("test"), indexSettings, newAnalysisService(indexSettings), new PostingsFormatService(new Index("test")),
                new DocValuesFormatService(new Index("test")), newSimilarityLookupService(), null);
    }

    public static MapperService newMapperService() {
        return newMapperService(new Index("test"), ImmutableSettings.Builder.EMPTY_SETTINGS);
    }

    public static MapperService newMapperService(Index index, Settings indexSettings) {
        return new MapperService(index, indexSettings, new Environment(), newAnalysisService(), new IndexFieldDataService(index, new NoneCircuitBreakerService()),
                new PostingsFormatService(index), new DocValuesFormatService(index), newSimilarityLookupService(), null);
    }

    public static AnalysisService newAnalysisService() {
        return newAnalysisService(ImmutableSettings.Builder.EMPTY_SETTINGS);
    }

    public static AnalysisService newAnalysisService(Settings indexSettings) {
        Injector parentInjector = new ModulesBuilder().add(new SettingsModule(indexSettings), new EnvironmentModule(new Environment(ImmutableSettings.Builder.EMPTY_SETTINGS)), new IndicesAnalysisModule()).createInjector();
        Injector injector = new ModulesBuilder().add(
                new IndexSettingsModule(new Index("test"), indexSettings),
                new IndexNameModule(new Index("test")),
                new AnalysisModule(indexSettings, parentInjector.getInstance(IndicesAnalysisService.class))).createChildInjector(parentInjector);

        return injector.getInstance(AnalysisService.class);
    }

    public static SimilarityLookupService newSimilarityLookupService() {
        return new SimilarityLookupService(new Index("test"), ImmutableSettings.Builder.EMPTY_SETTINGS);
    }
}
