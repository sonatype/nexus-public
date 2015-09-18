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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.File;
import java.util.Iterator;
import java.util.NavigableSet;

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.file.FileBlobMetadata;
import org.sonatype.nexus.blobstore.file.FileBlobMetadataStore;
import org.sonatype.nexus.blobstore.file.FileBlobState;
import org.sonatype.nexus.common.collect.AutoClosableIterable;
import org.sonatype.nexus.common.guice.TcclWrapperFactory;
import org.sonatype.nexus.common.io.DirSupport;

import com.google.common.collect.Maps;
import org.mapdb.Atomic;
import org.mapdb.BTreeKeySerializer.BasicKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.HTreeMap;
import org.mapdb.TxBlock;
import org.mapdb.TxMaker;
import org.mapdb.TxRollbackException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * MapDB implementation of {@link FileBlobMetadataStore}.
 *
 * @since 3.0
 */
public class FileBlobMetadataStoreImpl
    extends LifecycleSupport
    implements FileBlobMetadataStore
{
  private static final String ID_SEQUENCE_NAME = "id_sequence";

  private static final String ENTRIES_NAME = "entries";

  private static final String STATE_PREFIX = "state_";

  private final File file;

  private TxMaker database;

  /**
   * The number of records counted when last cacheing size
   */
  private long numRecords = 0L;

  /**
   * The cached number of bytes stored.
   */
  private long cachedSize = 0L;

  private FileBlobMetadataStoreImpl(final File directory) {
    checkNotNull(directory);
    this.file = new File(directory, directory.getName() + ".db");
    log.debug("File: {}", file);
  }

  /**
   * MapDB uses a classloading strategy incompatible with OSGi (it uses the current thread's context class loader).
   * This method produces a BlobMetadataStore that has been wrapped with a proxy that ensures the right classloader
   * is used for mapdb's serializing/deserializing operations.
   */
  public static FileBlobMetadataStore create(final File directory) {
    return TcclWrapperFactory.create(
        FileBlobMetadataStore.class,
        new FileBlobMetadataStoreImpl(directory),
        FileBlobMetadataStoreImpl.class.getClassLoader()
    );
  }

  /**
   * Returns the primary database file.  MapDB has additional files which are based on this filename.
   */
  public File getFile() {
    return file;
  }

  @Override
  protected void doStart() throws Exception {
    DirSupport.mkdir(file.getParentFile());

    this.database = DBMaker.newFileDB(file)
        .checksumEnable()
        .mmapFileEnableIfSupported()
        .makeTxMaker();

    // prepare the database tables, non-lazy so that use of DB.snapshot() will not fail if table does not exist yet
    database.execute(new TxBlock()
    {
      @Override
      public void tx(final DB db) {
        db.getAtomicLong(ID_SEQUENCE_NAME);

        db.createHashMap(ENTRIES_NAME)
            .counterEnable()
            .keySerializer(new BlobIdSerializer())
            .valueSerializer(new MetadataRecord.SerializerImpl())
            .makeOrGet();

        for (FileBlobState state : FileBlobState.values()) {
          db.createTreeSet(STATE_PREFIX + state.name())
              .serializer(new BasicKeySerializer(new BlobIdSerializer()))
              .makeOrGet();
        }
      }
    });
  }

  @Override
  protected void doStop() throws Exception {
    database.close();
    database = null;
  }

  /**
   * Access id-sequence counter.
   */
  private Atomic.Long idSequence(final DB db) {
    return db.getAtomicLong(ID_SEQUENCE_NAME);
  }

  /**
   * Access entries table.
   */
  private HTreeMap<BlobId, MetadataRecord> entries(final DB db) {
    return db.getHashMap(ENTRIES_NAME);
  }

  /**
   * Access blob-state table.
   */
  private NavigableSet<BlobId> states(final DB db, final FileBlobState state) {
    return db.getTreeSet(STATE_PREFIX + state.name());
  }

  private MetadataRecord convert(final FileBlobMetadata source) {
    return new MetadataRecord(source);
  }

  private FileBlobMetadata convert(final MetadataRecord source) {
    FileBlobMetadata target = new FileBlobMetadata(source.state, Maps.newHashMap(source.headers));
    if (source.metrics) {
      target.setMetrics(new BlobMetrics(source.created, source.sha1, source.size));
    }
    return target;
  }

  /**
   * Generate a new blob identifier.
   */
  private BlobId newId(final DB db) {
    long id = idSequence(db).incrementAndGet();
    return new BlobId(String.format("%016x", id));
  }

  @Override
  public BlobId add(final FileBlobMetadata metadata) {
    checkNotNull(metadata);
    ensureStarted();

    final MetadataRecord record = convert(metadata);

    return database.execute(new Fun.Function1<BlobId, DB>()
    {
      @Override
      public BlobId run(final DB db) {
        BlobId id = newId(db);
        log.trace("Add: {}={}", id, record);

        MetadataRecord prev = entries(db).put(id, record);
        checkState(prev == null, "Duplicate blob-id: %s", id);

        // track state
        states(db, record.state).add(id);

        return id;
      }
    });
  }

  @Nullable
  @Override
  public FileBlobMetadata get(final BlobId id) {
    checkNotNull(id);
    ensureStarted();

    log.trace("Get: {}", id);

    DB db = database.makeTx();
    try {
      MetadataRecord record = entries(db).get(id);
      if (record != null) {
        return convert(record);
      }
      return null;
    }
    finally {
      db.close();
    }
  }

  @Override
  public void update(final BlobId id, final FileBlobMetadata metadata) {
    checkNotNull(id);
    checkNotNull(metadata);
    ensureStarted();

    final MetadataRecord record = convert(metadata);
    log.trace("Update: {}={}", id, record);

    database.execute(new TxBlock()
    {
      @Override
      public void tx(final DB db) {
        MetadataRecord prev = entries(db).put(id, record);
        checkState(prev != null, "Can not update non-existent blob-id: %s", id);

        // replace state
        states(db, prev.state).remove(id);
        states(db, record.state).add(id);
      }
    });
  }

  @Override
  public void delete(final BlobId id) {
    checkNotNull(id);
    ensureStarted();

    log.trace("Delete: {}", id);

    database.execute(new TxBlock()
    {
      @Override
      public void tx(final DB db) {
        MetadataRecord prev = entries(db).remove(id);
        checkState(prev != null, "Can not delete non-existent blob-id: %s", id);

        // remove state
        states(db, prev.state).remove(id);
      }
    });
  }

  @Override
  public AutoClosableIterable<BlobId> findWithState(final FileBlobState state) {
    checkNotNull(state);
    ensureStarted();

    log.trace("Find with state: {}", state);

    final DB db = database.makeTx().snapshot();

    return new AutoClosableIterable<BlobId>()
    {
      private volatile boolean closed = false;

      @Override
      public Iterator<BlobId> iterator() {
        return states(db, state).iterator();
      }

      @Override
      public void close() throws Exception {
        db.close();
        closed = true;
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

  private File[] listFiles() {
    File[] files = file.getParentFile().listFiles();
    if (files == null) {
      // should never happen
      return new File[0];
    }
    return files;
  }

  @Override
  public long getBlobCount() {
    ensureStarted();
    DB db = database.makeTx();
    try {
      return entries(db).sizeLong();
    }
    finally {
      db.close();
    }
  }

  @Override
  public long getTotalSize() {
    ensureStarted();

    return getMetadataSize() + getBlobSize();
  }

  @Override
  public long getMetadataSize() {
    ensureStarted();

    // sum all file bytes in the database root
    long bytes = 0;
    for (File file : listFiles()) {
      bytes += file.length();
    }
    return bytes;
  }

  @Override
  public long getBlobSize() {
    ensureStarted();
    return database.execute(new Fun.Function1<Long, DB>()
    {
      @Override
      public Long run(final DB db) {
        long totalSize = 0L;
        HTreeMap<BlobId, MetadataRecord> entries = entries(db);
        if (entries.sizeLong() == numRecords) {
          log.debug("Returning cached blob size");
          return cachedSize;
        }

        log.debug("Cached data is invalid, recalculating size of blobs");

        for (MetadataRecord metadataRecord : entries.values()) {
          if (metadataRecord.size != null) {
            totalSize += metadataRecord.size;
          }
          else {
            log.warn("Blob metadata has no size indicated for sha1: {}", metadataRecord.sha1);
          }
        }
        numRecords = entries.sizeLong();
        cachedSize = totalSize;
        return cachedSize;
      }
    });
  }

  @Override
  public void compact() {
    ensureStarted();

    database.execute(new TxBlock()
    {
      @Override
      public void tx(final DB db) throws TxRollbackException {
        log.trace("Compacting");
        db.compact();
      }
    });
  }
}
