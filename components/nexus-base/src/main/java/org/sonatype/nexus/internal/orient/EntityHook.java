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
package org.sonatype.nexus.internal.orient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.orient.entity.EntityAdapter;
import org.sonatype.nexus.orient.entity.EntityBatchEvent;
import org.sonatype.nexus.orient.entity.EntityBatchEvent.Batchable;
import org.sonatype.nexus.orient.entity.EntityEvent;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.orient.core.command.OCommandExecutor;
import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseInternal;
import com.orientechnologies.orient.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.orient.core.db.ODatabaseListener;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Hooks into OrientDB to send {@link EntityEvent}s on the behalf of {@link EntityAdapter}s.
 *
 * @since 3.0
 */
@Named
@Singleton
@SuppressWarnings("rawtypes")
public class EntityHook
    extends ComponentSupport
    implements ODatabaseLifecycleListener, ODatabaseListener, ORecordHook
{
  private final Map<ODatabase, Map<ODocument, TYPE>> operations = new ConcurrentHashMap<>();

  private final EventBus eventBus;

  private final List<EntityAdapter<?>> adapters;

  @Inject
  public EntityHook(final EventBus eventBus, final List<EntityAdapter<?>> adapters) {
    this.eventBus = checkNotNull(eventBus);
    this.adapters = checkNotNull(adapters);
  }

  public PRIORITY getPriority() {
    return PRIORITY.LAST;
  }

  public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
    return DISTRIBUTED_EXECUTION_MODE.BOTH;
  }

  public void onOpen(final ODatabaseInternal db) {
    // ignore operations which have a null user, such as index repair
    if (db instanceof ODatabaseDocumentTx && db.getUser() != null) {
      for (final EntityAdapter<?> adapter : adapters) {
        if (adapter.sendEvents() && adapter.isRegistered((ODatabaseDocumentTx) db)) {
          db.registerListener(this);
          db.registerHook(this, HOOK_POSITION.LAST);
          break;
        }
      }
    }
  }

  public void onClose(final ODatabaseInternal db) {
    if (db instanceof ODatabaseDocumentTx) {
      db.unregisterHook(this);
      db.unregisterListener(this);
      operations.remove(db);
    }
  }

  public RESULT onTrigger(final TYPE type, final ORecord record) {
    if (recordEntityEvent(type) && record instanceof ODocument) {
      final ODatabase db = ODatabaseRecordThreadLocal.INSTANCE.get();
      if (db != null) {
        Map<ODocument, TYPE> ops = operations.get(db);
        if (ops == null) {
          ops = new LinkedHashMap<>();
          operations.put(db, ops);
        }
        if (!ops.containsKey(record)) {
          log.trace("Recording {} {}", type, record);
          ops.put((ODocument) record, type);
          return RESULT.RECORD_NOT_CHANGED;
        }
      }
    }

    log.trace("Ignoring {} {}", type, record);
    return RESULT.RECORD_NOT_CHANGED;
  }

  public void onAfterTxCommit(final ODatabase db) {
    final Map<ODocument, TYPE> ops = operations.remove(db);
    if (ops != null) {
      final UnitOfWork work = UnitOfWork.pause();
      try {
        List<EntityEvent> batchedEvents = new ArrayList<>();
        for (final Entry<ODocument, TYPE> entry : ops.entrySet()) {
          final EntityEvent event = newEntityEvent(entry.getKey(), entry.getValue());
          if (event != null) {
            eventBus.post(event);
            if (event instanceof Batchable) {
              batchedEvents.add(event);
            }
          }
        }
        if (!batchedEvents.isEmpty()) {
          eventBus.post(new EntityBatchEvent(batchedEvents));
        }
      }
      finally {
        db.activateOnCurrentThread();
        UnitOfWork.resume(work);
      }
    }
  }

  public void onAfterTxRollback(final ODatabase db) {
    operations.remove(db);
  }

  private static boolean recordEntityEvent(final TYPE type) {
    switch (type) {
      case AFTER_CREATE:
      case AFTER_UPDATE:
      case AFTER_DELETE:
        return true;
      default:
        return false;
    }
  }

  private EntityEvent newEntityEvent(final ODocument document, final TYPE eventType) {
    final OClass schemaType = document.getSchemaClass();
    if (schemaType != null) {
      for (final EntityAdapter adapter : adapters) {
        if (adapter.sendEvents() && schemaType.equals(adapter.getSchemaType())) {
          return adapter.newEvent(document, eventType);
        }
      }
    }
    return null;
  }

  public void onCreate(final ODatabaseInternal db) {
    // no-op
  }

  public void onDrop(final ODatabaseInternal db) {
    // no-op
  }

  public void onCreateClass(final ODatabaseInternal db, final OClass type) {
    // no-op
  }

  public void onDropClass(final ODatabaseInternal db, final OClass type) {
    // no-op
  }

  public void onBeforeTxBegin(final ODatabase db) {
    // no-op
  }

  public void onBeforeTxCommit(final ODatabase db) {
    // no-op
  }

  public void onBeforeTxRollback(final ODatabase db) {
    // no-op
  }

  public void onOpen(final ODatabase db) {
    // no-op
  }

  public void onClose(final ODatabase db) {
    // no-op
  }

  public void onCreate(final ODatabase db) {
    // no-op
  }

  public void onDelete(final ODatabase db) {
    // no-op
  }

  public void onBeforeCommand(final OCommandRequestText cmd, final OCommandExecutor executor) {
    // no-op
  }

  public void onAfterCommand(final OCommandRequestText cmd, final OCommandExecutor executor, final Object result) {
    // no-op
  }

  public boolean onCorruptionRepairDatabase(final ODatabase db, final String reason, final String action) {
    return false;
  }

  public void onUnregister() {
    // no-op
  }
}
