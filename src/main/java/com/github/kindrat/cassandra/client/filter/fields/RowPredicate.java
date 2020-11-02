package com.github.kindrat.cassandra.client.filter.fields;

import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.github.kindrat.cassandra.client.ui.DataObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Predicate;

import static lombok.AccessLevel.PROTECTED;

@Getter(PROTECTED)
@RequiredArgsConstructor
public abstract class RowPredicate implements Predicate<DataObject> {
    @Getter(PROTECTED)
    private static final CodecRegistry codecRegistry = CodecRegistry.DEFAULT;
    private final String field;
    private final String value;

    TypeCodec<?> getColumnCodec(Object actual) {
        return getCodecRegistry().codecFor(actual);
    }
}
