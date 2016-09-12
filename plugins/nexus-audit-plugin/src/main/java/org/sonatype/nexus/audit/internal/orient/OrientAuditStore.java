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
package org.sonatype.nexus.audit.internal.orient;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.internal.AuditStore;
import org.sonatype.nexus.common.collect.AutoClosableIterable;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OrientDB implementation of {@link AuditStore}.
 *
 * @since 3.1
 */
@Named("orient")
@Singleton
public class OrientAuditStore
    extends LifecycleSupport
    implements AuditStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final AuditDataEntityAdapter entityAdapter;

  private final String dbJournalClusterPrefix;

  private volatile int currentJournalClusterId;

  @Inject
  public OrientAuditStore(@Named("audit") final Provider<DatabaseInstance> databaseInstance,
                          final AuditDataEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    this.dbJournalClusterPrefix = entityAdapter.getTypeName() + "_journal";
  }

  @Override
  protected void doStart() throws Exception {
    // create database
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      // register schema
      entityAdapter.register(db);

      // create next journal
      createNextJournal(db);
    }
  }

  /**
   * Open a database connection using the pool.
   */
  private ODatabaseDocumentTx openDb() {
    return databaseInstance.get().acquire();
  }

  @Override
  public void add(final AuditData data) throws Exception {
    checkNotNull(data);
    ensureStarted();

    try (ODatabaseDocumentTx db = openDb()) {
      entityAdapter.addEntity(db, data);
    }
  }

  @Override
  public void clear() throws Exception {
    ensureStarted();

    try (ODatabaseDocumentTx db = openDb()) {
      // create a new journal
      createNextJournal(db);

      // drop all other journals
      for (int id : exportableJournalIds(db)) {
        dropJournal(db, id);
      }
    }

    log.debug("Cleared");
  }

  @Override
  public long approximateSize() throws Exception {
    ensureStarted();

    try (ODatabaseDocumentTx db = openDb()) {
      return entityAdapter.count.execute(db);
    }
  }

  @Override
  public AutoClosableIterable<AuditData> browse(final long offset, @Nullable Long limit) throws Exception {
    ensureStarted();

    ODatabaseDocumentTx db = openDb();
    Iterable<ODocument> results = entityAdapter.browseDocuments(db, offset, limit);
    if (results == null) {
      db.close();
      return AutoClosableIterable.Factory.emptyIterable();
    }
    return iterate(db, results);
  }

  //
  // Journal management
  //

  private void createNextJournal(final ODatabaseDocumentTx db) {
    OClass type = entityAdapter.getSchemaType();
    String name = String.format("%s_%s", dbJournalClusterPrefix, System.currentTimeMillis());
    int cid = db.addCluster(name);
    type.addClusterId(cid);
    type.setDefaultClusterId(cid);

    log.debug("Created new journal cluster; id: {}, name: {}", cid, name);
    this.currentJournalClusterId = cid;
  }

  /**
   * Create the next journal (cluster) and set it as the default.
   */
  void createNextJournal() {
    try (ODatabaseDocumentTx db = openDb()) {
      createNextJournal(db);
    }
  }

  private List<Integer> exportableJournalIds(final ODatabaseDocumentTx db) {
    List<Integer> ids = Lists.newArrayList();

    OClass type = entityAdapter.getSchemaType();
    for (int cid : type.getClusterIds()) {
      // skip current journal cluster
      if (cid == currentJournalClusterId) {
        continue;
      }
      // only include clusters with journal prefix
      String name = db.getClusterNameById(cid);
      if (name.startsWith(dbJournalClusterPrefix)) {
        ids.add(cid);
      }
    }

    return ids;
  }

  /**
   * Return a list of exportable journal (cluster) ids.
   */
  List<Integer> exportableJournalIds() {
    try (ODatabaseDocumentTx db = openDb()) {
      return exportableJournalIds(db);
    }
  }

  /**
   * Browse journal (cluster).
   */
  AutoClosableIterable<AuditData> browseJournal(final int id) {
    log.debug("Browsing journal: {}", id);
    ODatabaseDocumentTx db = openDb();
    String name = db.getClusterNameById(id);
    Iterable<ODocument> results = db.browseCluster(name);
    return iterate(db, results);
  }

  private void dropJournal(final ODatabaseDocumentTx db, final int id) {
    log.debug("Dropping journal: {}", id);
    OClass type = entityAdapter.getSchemaType();
    type.removeClusterId(id);
    db.dropCluster(id, true);
  }

  /**
   * Drop journal (cluster).
   */
  void dropJournal(final int id) {
    try (ODatabaseDocumentTx db = openDb()) {
      dropJournal(db, id);
    }
  }

  //
  // Entity iteration
  //

  /**
   * Convert document results into {@link AuditData} iterable.
   */
  private AutoClosableIterable<AuditData> iterate(final ODatabaseDocumentTx db, final Iterable<ODocument> documents) {
    // convert documents to events
    final Iterable<AuditData> converter = entityAdapter.transform(documents);

    // adapt converter in auto-closable
    return new AutoClosableIterable<AuditData>()
    {
      private volatile boolean closed = false;

      @Override
      public void close() throws Exception {
        db.close();
        closed = true;
      }

      @Override
      public Iterator<AuditData> iterator() {
        return converter.iterator();
      }

      @Override
      protected void finalize() throws Throwable {
        try {
          if (!closed) {
            log.warn("Leaked database connection: {}", db);
            db.close();
          }
        }
        finally {
          super.finalize();
        }
      }
    };
  }
}