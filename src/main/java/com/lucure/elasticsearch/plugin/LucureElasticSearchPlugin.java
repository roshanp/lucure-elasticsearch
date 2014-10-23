package com.lucure.elasticsearch.plugin;

import com.lucure.elasticsearch.LucureInternalIndexShard;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import com.lucure.elasticsearch.LucureQueryParser;
import org.elasticsearch.index.shard.service.InternalIndexShard;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;
import java.util.Collections;

/**
 */
public class LucureElasticSearchPlugin extends AbstractPlugin {
    @Override
    public String name() {
        return "lucure";
    }

    @Override
    public String description() {
        return "Provides field level visibility in ElasticSearch";
    }

    @Override
    public Collection<Module> indexModules(
      Settings settings) {
        return Collections.singleton((Module) new AbstractModule() {
            @Override
            protected void configure() {
                bind(RegisterLucureRestrictedType.class).asEagerSingleton();
                bind(LucureQueryParser.class).asEagerSingleton();
            }
        });
    }

    @Override
    public Collection<Module> shardModules(
      Settings settings) {
        return Collections.singleton((Module) new AbstractModule() {
            @Override
            protected void configure() {
                bind(InternalIndexShard.class).to(LucureInternalIndexShard.class).asEagerSingleton();
            }
        });
    }
}
