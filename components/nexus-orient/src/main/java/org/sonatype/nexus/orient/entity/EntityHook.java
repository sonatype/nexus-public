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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.orientechnologies.orient.core.metadata.security.OSecurityNull;
import com.orientechnologies.orient.core.query.live.OLiveQueryHook;
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

  private static final ThreadLocal<ODatabase> commitDb = new ThreadLocal<>();

  private final Map<String, EntityAdapter<?>> recordingAdapters = new ConcurrentHashMap<>();

  private final Set<String> recordingDatabases = newSetFromMap(new ConcurrentHashMap<>());

  private final Map<ODatabase, List<Object>> dbEvents = new ConcurrentHashMap<>();

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
    recordingAdapters.put(adapter.getTypeName(), adapter);
    recordingDatabases.add(adapter.getDbName());

    pendingDbs.removeIf(db -> db.isClosed() || startRecording(db));
  }

  /**
   * Disables entity events for the given {@link EntityAdapter}.
   */
  public void disableEvents(final EntityAdapter adapter) {
    log.trace("Disable entity events for {}", adapter);
    recordingAdapters.remove(adapter.getTypeName());
    // leave database registered as recording
  }

  @Override
  public void onOpen(final ODatabaseInternal db) {
    unregisterLiveQueryHook(db);
    if (OSecurityNull.class.equals(db.getProperty(ODatabase.OPTIONS.SECURITY.toString()))) {
      return; // ignore maintenance operations which run without security, such as index repair
    }
    if (!startRecording(db)) {
      pendingDbs.add(db);
    }
    // reload metadata when (re-)opening a DB connection if old schema is gone
    // (can be removed after upgrading to OrientDB 2.2.33 as it does it for us)
    if (db.getMetadata().getSchema().countClasses() == 0) {
      log.debug("Reloading metadata for {} as storage has changed", db.getName());
      db.getMetadata().reload();
    }
  }

  @Override
  public void onClose(final ODatabaseInternal db) {
    if (!pendingDbs.remove(db)) {
      stopRecording(db);
      flushEvents(db);
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
  public void onBeforeTxCommit(final ODatabase db) {
    commitDb.set(db);
  }

  @Override
  public void onAfterTxCommit(final ODatabase db) {
    commitDb.remove();
    flushEvents(db);
  }

  @Override
  public void onAfterTxRollback(final ODatabase db) {
    commitDb.remove();
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
  }

  private static <T> T withActiveDb(final ODatabase db, final Supplier<T> supplier) {
    @SuppressWarnings("resource")
    final ODatabaseDocumentInternal currentDb = ODatabaseRecordThreadLocal.instance().getIfDefined();
    if (db.equals(currentDb) || !(db instanceof ODatabaseDocumentInternal)) {
      return supplier.get();
    }
    try {
      ODatabaseRecordThreadLocal.instance().set((ODatabaseDocumentInternal) db);
      return supplier.get();
    }
    finally {
      if (currentDb != null) {
        ODatabaseRecordThreadLocal.instance().set(currentDb);
      }
      else {
        ODatabaseRecordThreadLocal.instance().remove();
      }
    }
  }

  private boolean recordEvent(final ODocument document, final EventKind eventKind) {
    final String typeName = document.getClassName();
    if (typeName != null) {
      final EntityAdapter adapter = recordingAdapters.get(typeName);
      if (adapter != null) {
        final ODatabaseInternal db = getCurrrentDb();
        if (db != null) {
          // workaround OrientDB 2.1 issue where in-TX dictionary updates are not replicated
          if (db.getStorage().isDistributed() && adapter instanceof SingletonEntityAdapter) {
            ((SingletonEntityAdapter) adapter).singleton.replicate(document, eventKind);
          }
          List<Object> events = dbEvents.get(db);
          if (events == null) {
            events = new ArrayList<>();
            dbEvents.put(db, events);
          }
          upsertEvent(events, document, eventKind);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the current DB connection. Normally this is whichever connection is active on the thread.
   * But when performing a commit we always return the connection which is being committed, so we can
   * track changes even when another connection is used to "fix" records during that commit.
   */
  private ODatabaseDocumentInternal getCurrrentDb() {
    ODatabase db = commitDb.get();
    if (db == null) {
      db = ODatabaseRecordThreadLocal.instance().get();
    }
    return (ODatabaseDocumentInternal) db;
  }

  /**
   * Maintains a paired sequence of ODocument, EventKind, ODocument, EventKind, etc...
   *
   * Handles both insertion and updates; updates to a document event pair does not change its position.
   */
  private static void upsertEvent(final List<Object> events, final ODocument document, final EventKind eventKind) {
    // scan the list to see if we already track this document
    for (int i = 0; i < events.size(); i += 2) {
      if (document.equals(events.get(i))) {
        // we want to maintain the original event kind, except if there's a last minute delete
        if (eventKind == EventKind.DELETE && events.set(i + 1, EventKind.DELETE) == EventKind.CREATE) {
          // delete after create in the same TX implies a phantom entity which can be discarded
          events.remove(i);
          events.remove(i);
        }
        else {
          events.set(i, document); // refresh document in case the instance has been replaced
        }
        return;
      }
    }

    // track new event pair
    events.add(document);
    events.add(eventKind);
  }

  private void flushEvents(final ODatabase db) {
    final List<Object> events = dbEvents.remove(db);
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
      catch (Throwable e) { // NOSONAR
        // exceptions as a result of posting events should not affect the commit,
        // so log and swallow them rather than let them propagate back to Orient
        log.error("Failed to post entity events", e);
      }
      finally {
        UnitOfWork.resume(work);
      }
    }
  }

  /**
   * Posts the given events; this will be a paired sequence of ODocument, EventKind, ODocument, EventKind, etc...
   */
  private void postEvents(final ODatabase db, final List<Object> events, final String remoteNodeId) {
    final List<EntityEvent> batchedEvents = new ArrayList<>();
    for (int i = 0; i < events.size(); i += 2) {
      final EntityEvent event = newEntityEvent((ODocument) events.get(i), (EventKind) events.get(i + 1));
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
    EntityAdapter adapter = recordingAdapters.get(document.getClassName());
    EntityEvent event = adapter.newEvent(document, eventKind);
    if (event != null && eventManager.isAffinityEnabled()) {
      event.setAffinity(adapter.eventAffinity(document));
    }
    return event;
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

  /**
   * Unregisters {@link OLiveQueryHook} from the opened DB to reduce overhead as we don't use this feature.
   */
  private void unregisterLiveQueryHook(final ODatabase db) {
    Optional<OLiveQueryHook> liveQueryHook = db.getHooks().keySet().stream()
        .filter(hook -> hook instanceof OLiveQueryHook)
        .findFirst();

    if (liveQueryHook.isPresent()) {
      log.debug("Unregistering OLiveQueryHook");
      db.unregisterListener(liveQueryHook.get());
      db.unregisterHook(liveQueryHook.get());
    }
  }
}
