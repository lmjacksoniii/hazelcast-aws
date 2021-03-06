/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.nearcache.impl.store;

import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.internal.nearcache.impl.record.NearCacheDataRecord;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.serialization.SerializationService;

import static com.hazelcast.internal.nearcache.NearCache.CACHED_AS_NULL;
import static com.hazelcast.internal.nearcache.NearCacheRecord.TIME_NOT_SET;
import static com.hazelcast.internal.nearcache.impl.record.AbstractNearCacheRecord.NUMBER_OF_INTEGER_FIELD_TYPES;
import static com.hazelcast.internal.nearcache.impl.record.AbstractNearCacheRecord.NUMBER_OF_LONG_FIELD_TYPES;
import static com.hazelcast.util.Clock.currentTimeMillis;

public class NearCacheDataRecordStore<K, V> extends BaseHeapNearCacheRecordStore<K, V, NearCacheDataRecord> {

    public NearCacheDataRecordStore(String name,
                                    NearCacheConfig nearCacheConfig,
                                    SerializationService serializationService,
                                    ClassLoader classLoader) {
        super(name, nearCacheConfig, serializationService, classLoader);
    }

    @Override
    protected long getKeyStorageMemoryCost(K key) {
        if (key instanceof Data) {
            return
                    // reference to this key data inside map ("store" field)
                    REFERENCE_SIZE
                            // heap cost of this key data
                            + ((Data) key).getHeapCost();
        } else {
            // memory cost for non-data typed instance is not supported
            return 0L;
        }
    }

    // TODO: we don't handle object header (mark, class definition) for heap memory cost
    @Override
    protected long getRecordStorageMemoryCost(NearCacheDataRecord record) {
        if (record == null) {
            return 0L;
        }
        Data value = record.getValue();
        return
                // reference to this record inside map ("store" field)
                REFERENCE_SIZE
                        // reference to "value" field
                        + REFERENCE_SIZE
                        // "uuid" ref size + 2 long in uuid
                        + REFERENCE_SIZE + (2 * (Long.SIZE / Byte.SIZE))
                        // heap cost of this value data
                        + (value != null ? value.getHeapCost() : 0)
                        + NUMBER_OF_LONG_FIELD_TYPES * (Long.SIZE / Byte.SIZE)
                        + NUMBER_OF_INTEGER_FIELD_TYPES * (Integer.SIZE / Byte.SIZE);
    }

    @Override
    protected NearCacheDataRecord valueToRecord(V value) {
        Data data = toData(value);
        long creationTime = currentTimeMillis();
        if (timeToLiveMillis > 0) {
            return new NearCacheDataRecord(data, creationTime, creationTime + timeToLiveMillis);
        } else {
            return new NearCacheDataRecord(data, creationTime, TIME_NOT_SET);
        }
    }

    @Override
    protected V recordToValue(NearCacheDataRecord record) {
        if (record.getValue() == null) {
            nearCacheStats.incrementMisses();
            return (V) CACHED_AS_NULL;
        }
        Data data = record.getValue();
        return dataToValue(data);
    }

    @Override
    protected void updateRecordValue(NearCacheDataRecord record, V value) {
        record.setValue(toData(value));
    }

    @Override
    public Object selectToSave(Object... candidates) {
        Object selectedCandidate = null;
        if (candidates != null && candidates.length > 0) {
            for (Object candidate : candidates) {
                // give priority to Data typed candidate, so there will be no extra conversion from Object to Data
                if (candidate instanceof Data) {
                    selectedCandidate = candidate;
                    break;
                }
            }
            if (selectedCandidate != null) {
                return selectedCandidate;
            } else {
                // select a non-null candidate
                for (Object candidate : candidates) {
                    if (candidate != null) {
                        selectedCandidate = candidate;
                        break;
                    }
                }
            }
        }
        return selectedCandidate;
    }
}
