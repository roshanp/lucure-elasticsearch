package com.lucure.elasticsearch.plugin;

import com.lucure.elasticsearch.LucureInternalEngine;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.index.engine.Engine;

/**
 */
public class LucureEngineElasticSearchModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Engine.class).to(LucureInternalEngine.class).asEagerSingleton();
    }
}
