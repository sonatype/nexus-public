/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.orient.entity;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.common.exception.OHighLevelException;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.storage.impl.local.paginated.OLocalPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.ORecordOperationMetadata;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperationMetadata;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OAtomicUnitEndRecord;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OLogSequenceNumber;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OWALRecord;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OWriteAheadLog;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.orientechnologies.orient.core.config.OGlobalConfiguration.STORAGE_TRACK_CHANGED_RECORDS_IN_WAL;
import static com.orientechnologies.orient.core.storage.impl.local.paginated.ORecordOperationMetadata.RID_METADATA_KEY;

/**
 * Marks and reports entity changes recorded in the write-ahead-log.
 *
 * @since 3.4
 */
public final class EntityLog
{
  @VisibleForTesting
  static final String ENTITY_LOG_LIMIT_KEY = "nexus.orient.entityLog.limit";

  private final int entityLogLimit = SystemPropertiesHelper.getInteger(ENTITY_LOG_LIMIT_KEY, 1000);

  private final boolean hasRecordIds = STORAGE_TRACK_CHANGED_RECORDS_IN_WAL.getValueAsBoolean();

  private final Provider<DatabaseInstance> databaseProvider;

  private final List<EntityAdapter> adapters;

  private OLocalPaginatedStorage storage;

  /**
   * Creates a new log, tracking entities owned by the given entity adapters.
   */
  public EntityLog(final Provider<DatabaseInstance> databaseProvider, final EntityAdapter... adapters) {
    this.databaseProvider = checkNotNull(databaseProvider);
    this.adapters = Arrays.asList(adapters);
  }

  /**
   * Returns a marker representing the current end of the database's write-ahead-log.
   */
  public OLogSequenceNumber mark() {
    return storage().getWALInstance().end();
  }

  /**
   * Returns records changed since the given marker, along with their entity adapters.
   *
   * @throws UnknownDeltaException when a change exists, but details aren't available
   */
  public Map<ORID, EntityAdapter> since(final OLogSequenceNumber marker) {
    checkNotNull(marker);
    return storage().callInLock(() -> {
      try {
        return doSince(marker);
      }
      catch (Exception e) {
        throw new UnknownDeltaException(marker, e);
      }
    }, false);
  }

  /**
   * Thrown when there has been a change since the marker, but details aren't available.
   */
  public static class UnknownDeltaException
      extends RuntimeException
      implements OHighLevelException
  {
    public UnknownDeltaException(final OLogSequenceNumber since, final Throwable cause) {
      super(String.format("Changes exist since %s but details are not available", since), cause);
    }

    public UnknownDeltaException(final String message) {
      super(message);
    }
  }

  private OLocalPaginatedStorage storage() {
    if (storage == null || storage.isClosed()) {
      // use temp TX to get local storage; note we don't need a TX when reading write-ahead-log
      ODatabaseDocumentInternal currentDb = ODatabaseRecordThreadLocal.instance().getIfDefined();
      try (ODatabaseDocumentInternal db = databaseProvider.get().acquire()) {
        storage = (OLocalPaginatedStorage) db.getStorage().getUnderlying();
      }
      finally {
        ODatabaseRecordThreadLocal.instance().set(currentDb);
      }
    }
    return storage;
  }

  private Map<ORID, EntityAdapter> doSince(final OLogSequenceNumber marker) throws IOException {
    OWriteAheadLog wal = storage.getWALInstance();

    OLogSequenceNumber end = wal.end();

    if (marker.equals(end)) {
      return ImmutableMap.of(); // nothing to extract
    }

    checkArgument(marker.compareTo(end) <= 0, "Sequence number cannot be after end");
    OLogSequenceNumber firstChange = checkNotNull(wal.next(marker), "Dangling sequence number");

    AdapterIndex adapterIndex = new AdapterIndex(adapters);

    wal.addCutTillLimit(firstChange);
    try {
      Map<ORID, EntityAdapter> result = new HashMap<>();
      // re-check position in case write-ahead-log was truncated before we could preserve it
      OLogSequenceNumber lsn = checkNotNull(wal.next(marker), "Dangling sequence number");
      while (lsn != null && lsn.compareTo(end) <= 0) {
        OWALRecord record = wal.read(lsn);
        if (record instanceof OAtomicUnitEndRecord) {
          extractDelta((OAtomicUnitEndRecord) record).forEach(rid -> result.computeIfAbsent(rid, adapterIndex::lookup));
          if (entityLogLimit >= 0 && result.size() > entityLogLimit) {
            throw new IOException("Too many changes to return");
          }
        }
        lsn = wal.next(lsn);
      }
      return result;
    }
    finally {
      wal.removeCutTillLimit(firstChange);
    }
  }

  private Set<ORID> extractDelta(final OAtomicUnitEndRecord record) {
    OAtomicOperationMetadata<?> ops = record.getAtomicOperationMetadata().get(RID_METADATA_KEY);
    if (ops instanceof ORecordOperationMetadata) {
      return ((ORecordOperationMetadata) ops).getValue();
    }
    // are the ids missing because we weren't recording?
    checkState(hasRecordIds, "Record ids not available");
    return ImmutableSet.of();
  }

  /**
   * Provides a quick mapping from records to their {@link EntityAdapter}.
   */
  private static class AdapterIndex
  {
    private final Map<Integer, EntityAdapter> index = new HashMap<>();

    public AdapterIndex(final List<EntityAdapter> adapters) {
      adapters.forEach(adapter -> {
        for (int clusterId : adapter.getSchemaType().getClusterIds()) {
          index.put(clusterId, adapter);
        }
      });
    }

    public EntityAdapter lookup(final ORID rid) {
      return index.get(rid.getClusterId());
    }
  }
}
