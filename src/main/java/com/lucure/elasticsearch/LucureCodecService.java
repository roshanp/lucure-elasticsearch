package com.lucure.elasticsearch;

import com.lucure.core.codec.LucureCodec;
import org.apache.lucene.codecs.Codec;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.codec.CodecService;
import org.elasticsearch.index.settings.IndexSettings;

/**
 * Service will only allow the specific Lucure Codec to be returned
 */
public class LucureCodecService extends CodecService {

    private final LucureCodec lucureCodec;

    public LucureCodecService(CodecService codecService) {
        this(codecService, ImmutableSettings.Builder.EMPTY_SETTINGS);
    }

    @Inject
    public LucureCodecService(CodecService codecService, @IndexSettings Settings indexSettings) {
        super(codecService.index(), indexSettings);
        lucureCodec = new LucureCodec();
    }

    @Override
    public Codec codec(String name)
      throws ElasticsearchIllegalArgumentException {
        return lucureCodec;
    }
}
