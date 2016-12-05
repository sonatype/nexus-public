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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityBatchEvent;
import org.sonatype.nexus.common.entity.EntityBatchEvent.Batchable;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.orient.entity.EntityAdapter.EventKind;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.security.OSecurityNull;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedList;

/**
 * Hooks into OrientDB to send {@link EntityEvent}s on the behalf of {@link EntityAdapter}s.
 *
 * @since 3.1
 */
@Named
@Singleton
@SuppressWarnings("rawtypes")
public final class EntityHook
    extends DatabaseListenerSupport
{
  private static final Logger log = LoggerFactory.getLogger(EntityHook.class);

  private static final ThreadLocal<String> isRemote = new ThreadLocal<>();

  private final Map<OClass, EntityAdapter<?>> recordingAdapters = new ConcurrentHashMap<>();

  private final Set<String> recordingDatabases = newSetFromMap(new ConcurrentHashMap<>());

  private final Map<ODatabase, Map<ODocument, EventKind>> dbEvents = new ConcurrentHashMap<>();

  private final List<ODatabase> pendingDbs = synchronizedList(new ArrayList<>());

  private final EventManager eventManager;

  @Inject
  public EntityHook(final EventManager eventManager) {
    this.eventManager = checkNotNull(eventManager);
  }

  /**
   * Calls the given {@link Supplier} while flagged as remote.
   */
  public static <T> T asRemote(final String remoteNodeId, final Supplier<T> supplier) {
    isRemote.set(remoteNodeId);
    try {
      return supplier.get();
    }
    finally {
      isRemote.remove();
    }
  }

  /**
   * Calls the given {@link Runnable} while flagged as remote.
   */
  public static void asRemote(final String remoteNodeId, final Runnable runnable) {
    isRemote.set(remoteNodeId);
    try {
      runnable.run(); // NOSONAR
    }
    finally {
      isRemote.remove();
    }
  }

  /**
   * Enables entity events for the given {@link EntityAdapter}.
   */
  public void enableEvents(final EntityAdapter adapter) {
    log.trace("Enable entity events for {}", adapter);
    recordingAdapters.put(adapter.getSchemaType(), adapter);
    recordingDatabases.add(adapter.getDbName());

    pendingDbs.removeIf(db -> startRecording(db));
  }

  /**
   * Disables entity events for the given {@link EntityAdapter}.
   */
  public void disableEvents(final EntityAdapter adapter) {
    log.trace("Disable entity events for {}", adapter);
    recordingAdapters.remove(adapter.getSchemaType());
    // leave database registered as recording
  }

  @Override
  public void onOpen(final ODatabaseInternal db) {
    if (OSecurityNull.class.equals(db.getProperty(ODatabase.OPTIONS.SECURITY.toString()))) {
      return; // ignore maintenance operations which run without security, such as index repair
    }
    if (!startRecording(db)) {
      pendingDbs.add(db);
    }
  }

  @Override
  public void onClose(final ODatabaseInternal db) {
    if (!pendingDbs.remove(db)) {
      stopRecording(db);
    }
  }

  @Override
  public RESULT onTrigger(final TYPE type, final ORecord record) {
    final EventKind eventKind = getEventKind(type);
    if (eventKind != null && record instanceof ODocument && recordEvent((ODocument) record, eventKind)) {
      log.trace("Recorded {} {}", type, record);
    }
    else {
      log.trace("Ignored {} {}", type, record);
    }
    return RESULT.RECORD_NOT_CHANGED;
  }

  @Override
  public void onAfterTxCommit(final ODatabase db) {
    final Map<ODocument, EventKind> events = dbEvents.remove(db);
    if (events != null) {
      final UnitOfWork work = UnitOfWork.pause();
      final String remoteNodeId = isRemote.get();
      try {
        if (remoteNodeId == null) {
          postEvents(db, events, null);
        }
        else {
          // posting events from remote node, mark current thread as replicating
          EventHelper.asReplicating(() -> postEvents(db, events, remoteNodeId));
        }
      }
      finally {
        UnitOfWork.resume(work);
      }
    }
  }

  @Override
  public void onAfterTxRollback(final ODatabase db) {
    dbEvents.remove(db);
  }

  private boolean startRecording(final ODatabase db) {
    if (recordingDatabases.contains(db.getName())) {
      db.registerListener(this);
      // this call must be made with the given db active on this thread
      withActiveDb(db, () -> db.registerHook(this, HOOK_POSITION.LAST));
      return true;
    }
    return false;
  }

  private void stopRecording(final ODatabase db) {
    db.unregisterHook(this);
    db.unregisterListener(this);
    dbEvents.remove(db);
  }

  private static <T> T withActiveDb(final ODatabase db, final Supplier<T> supplier) {
    @SuppressWarnings("resource")
    final ODatabaseDocumentInternal currentDb = ODatabaseRecordThreadLocal.INSTANCE.getIfDefined();
    if (db.equals(currentDb) || !(db instanceof ODatabaseDocumentInternal)) {
      return supplier.get();
    }
    try {
      ODatabaseRecordThreadLocal.INSTANCE.set((ODatabaseDocumentInternal) db);
      return supplier.get();
    }
    finally {
      if (currentDb != null) {
        ODatabaseRecordThreadLocal.INSTANCE.set(currentDb);
      }
      else {
        ODatabaseRecordThreadLocal.INSTANCE.remove();
      }
    }
  }

  private boolean recordEvent(final ODocument document, final EventKind eventKind) {
    final OClass schemaType = document.getSchemaClass();
    if (schemaType != null) {
      final EntityAdapter adapter = recordingAdapters.get(schemaType);
      if (adapter != null) {
        final ODatabaseInternal db = ODatabaseRecordThreadLocal.INSTANCE.get();
        if (db != null) {
          // workaround OrientDB 2.1 issue where in-TX dictionary updates are not replicated
          if (db.getStorage().isDistributed() && adapter instanceof SingletonEntityAdapter) {
            ((SingletonEntityAdapter) adapter).singleton.replicate(document, eventKind);
          }
          Map<ODocument, EventKind> events = dbEvents.get(db);
          if (events == null) {
            events = new LinkedHashMap<>();
            dbEvents.put(db, events);
          }
          // only record the first event on a given document
          if (!events.containsKey(document)) {
            events.put(document, eventKind);
            return true;
          }
        }
      }
    }
    return false;
  }

  private void postEvents(final ODatabase db, final Map<ODocument, EventKind> events, final String remoteNodeId) {
    final List<EntityEvent> batchedEvents = new ArrayList<>();
    for (final Entry<ODocument, EventKind> entry : events.entrySet()) {
      final EntityEvent event = newEntityEvent(entry.getKey(), entry.getValue());
      if (event != null) {
        event.setRemoteNodeId(remoteNodeId);
        eventManager.post(event);
        db.activateOnCurrentThread();
        if (event instanceof Batchable) {
          batchedEvents.add(event);
        }
      }
    }

    if (!batchedEvents.isEmpty()) {
      eventManager.post(new EntityBatchEvent(batchedEvents));
      db.activateOnCurrentThread();
    }
  }

  @Nullable
  private EntityEvent newEntityEvent(final ODocument document, final EventKind eventKind) {
    return recordingAdapters.get(document.getSchemaClass()).newEvent(document, eventKind);
  }

  @Nullable
  private static EventKind getEventKind(final TYPE type) {
    switch (type) {
      case AFTER_CREATE:
        return EventKind.CREATE;
      case AFTER_UPDATE:
        return EventKind.UPDATE;
      case AFTER_DELETE:
        return EventKind.DELETE;
      default:
        return null;
    }
  }
}
