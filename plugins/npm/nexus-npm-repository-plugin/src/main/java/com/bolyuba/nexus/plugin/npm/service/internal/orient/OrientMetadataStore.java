/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal.orient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.sisu.goodies.common.SimpleFormat;
import org.sonatype.sisu.goodies.lifecycle.LifecycleSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.bolyuba.nexus.plugin.npm.service.internal.MetadataStore;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OrientDB backed {@link MetadataStore} implementation.
 */
@Singleton
@Named("default")
public class OrientMetadataStore
    extends LifecycleSupport
    implements MetadataStore
{
  private static final String DB_LOCATION = "db/npm";

  private static final String BACKUP_LOCATION = "backup/npm";

  private final File databaseDirectory;

  private final File backupDirectory;

  private final Map<Class<?>, EntityHandler<?>> entityHandlers;

  private final int poolMaxSize;

  private OPartitionedDatabasePool pool;

  @Inject
  public OrientMetadataStore(final ApplicationDirectories applicationDirectories,
                             final @Named("${nexus.npm.poolMaxSize:-100}") int poolMaxSize)
  {
    checkArgument(poolMaxSize >= 1, "Pool max size must be greater or equal to 1");
    this.databaseDirectory = applicationDirectories.getWorkDirectory(DB_LOCATION);
    this.backupDirectory = applicationDirectories.getWorkDirectory(BACKUP_LOCATION);
    this.entityHandlers = Maps.newHashMap();
    this.poolMaxSize = poolMaxSize;
  }

  // Lifecycle
  @Override
  protected void doStart() throws Exception {
    final String dbUri = "plocal:" + databaseDirectory.getAbsolutePath();
    try (ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUri)) {
      if (!db.exists()) {
        // latest advice is to disable DB compression as it doesn't buy much,
        // also snappy has issues with use of native lib (unpacked under tmp)
        OGlobalConfiguration.STORAGE_COMPRESSION_METHOD.setValue("nothing");
        db.create();
        log.info("Created database: {}", databaseDirectory);
      }
      else {
        db.open("admin", "admin");
        log.info("Opened database: {}", db);
      }
      registerHandler(new PackageRootHandler(db));
    }

    pool = new OPartitionedDatabasePool(dbUri, "admin", "admin", poolMaxSize);
    log.info("Created pool (maxSize={}): {}", poolMaxSize, pool);
  }

  @Override
  public void doStop() throws Exception {
    log.info("Closing pool: {}", pool);
    pool.close();
    pool = null;
  }

  /**
   * Performs DB backup, during which database is "frozen" for data modification attempts.
   *
   * @since 2.11
   */
  public void backupDatabase() throws IOException {
    try (final ODatabaseDocumentTx db = db()) {
      final File backupFile = createBackupFile();
      log.info("Started npm DB backup to {} (all DB write operations are frozen)",
          backupFile.getAbsolutePath());
      final OutputStream outputStream = new FileOutputStream(backupFile);
      db.backup(outputStream, null, null, new OCommandOutputListener()
      {
        @Override
        public void onMessage(final String msg) {
          log.debug("Backup: {}", msg);
        }
      }, 9, 1024 * 1024);
      log.info("Finished npm DB backup to {}", backupFile.getAbsolutePath());
    }
  }

  /**
   * Creates a unique file for backup. It's guaranteed that file does not exists.
   *
   * @since 2.11
   */
  private File createBackupFile() throws IOException {
    DirSupport.mkdir(backupDirectory);
    int counter = 1;
    final String timestamp = new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());
    File backupFile;
    do {
      final String backupFileName = SimpleFormat.format("nexus-npm-db-backup-%s-%s.zip", timestamp, counter++);
      backupFile = new File(backupDirectory, backupFileName);
    }
    while (backupFile.exists());
    return backupFile;
  }

  /**
   * Acquires a pooled DB, close it to release back to pool.
   */
  private ODatabaseDocumentTx db() {
    ensureStarted();
    return pool.acquire();
  }

  @SuppressWarnings("unchecked")
  private <T> EntityHandler<T> getHandlerFor(Class<T> schema) {
    final EntityHandler<T> result = (EntityHandler<T>) entityHandlers.get(schema);
    checkArgument(result != null, "Schema %s has no registered handler!", schema.getName());
    return result;
  }

  private void registerHandler(final EntityHandler<?> entityHandler) {
    log.debug("Registering entity handler for type {}", entityHandler.getJavaType().getName());
    entityHandlers.put(entityHandler.getJavaType(), entityHandler);
  }

  // == API

  @Override
  public List<String> listPackageNames(final NpmRepository repository) {
    checkNotNull(repository);
    final EntityHandler<PackageRoot> entityHandler = getHandlerFor(PackageRoot.class);
    try (ODatabaseDocumentTx db = db()) {
      db.begin();
      try {
        final OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
            "select from " + entityHandler.getSchemaName() + " where repositoryId='" + repository.getId() +
                "' and @rid > ? limit 1000");
        ORID last = new ORecordId();
        final List<String> result = Lists.newArrayList();
        List<ODocument> resultset = db.query(query, last);
        while (!resultset.isEmpty()) {
          result.addAll(Lists.transform(resultset, new Function<ODocument, String>()
          {
            public String apply(@Nullable final ODocument input) {
              return input.field("name", OType.STRING);
            }
          }));
          last = resultset.get(resultset.size() - 1).getIdentity();
          resultset = db.query(query, last);
        }
        return result;
      }
      finally {
        db.commit();
      }
    }
  }

  @Override
  public PackageRoot getPackageByName(final NpmRepository repository, final String packageName) {
    checkNotNull(repository);
    checkNotNull(packageName);
    final EntityHandler<PackageRoot> entityHandler = getHandlerFor(PackageRoot.class);
    try (ODatabaseDocumentTx db = db()) {
      db.begin();
      try {
        final ODocument doc = doGetPackageByName(db, entityHandler, repository, packageName);
        if (doc == null) {
          return null;
        }
        return entityHandler.toEntity(doc);
      }
      finally {
        db.commit();
      }
    }
  }

  @Override
  public boolean deletePackages(final NpmRepository repository) {
    checkNotNull(repository);
    final EntityHandler<PackageRoot> entityHandler = getHandlerFor(PackageRoot.class);
    try (ODatabaseDocumentTx db = db()) {
      db.begin();
      try {
        int recordsDeleted = db.command(
            new OCommandSQL(
                "delete from " + entityHandler.getSchemaName() + " where repositoryId='" + repository.getId() + "'"
            )
        ).execute();
        return recordsDeleted > 0;
      }
      finally {
        db.commit();
      }
    }
  }

  @Override
  public boolean deletePackageByName(final NpmRepository repository, final String packageName) {
    checkNotNull(repository);
    checkNotNull(packageName);
    final EntityHandler<PackageRoot> entityHandler = getHandlerFor(PackageRoot.class);
    try (ODatabaseDocumentTx db = db()) {
      db.begin();
      try {
        final ODocument doc = doGetPackageByName(db, entityHandler, repository, packageName);
        if (doc == null) {
          return false;
        }
        db.delete(doc);
        return true;
      }
      finally {
        db.commit();
      }
    }
  }

  @Override
  public PackageRoot replacePackage(final NpmRepository repository, final PackageRoot packageRoot) {
    return updatePackage(repository, packageRoot, false);
  }

  @Override
  public PackageRoot updatePackage(final NpmRepository repository, final PackageRoot packageRoot) {
    return updatePackage(repository, packageRoot, true);
  }

  private PackageRoot updatePackage(final NpmRepository repository, final PackageRoot packageRoot, final boolean overlay) {
    checkNotNull(repository);
    checkNotNull(packageRoot);
    final EntityHandler<PackageRoot> entityHandler = getHandlerFor(PackageRoot.class);
    // TODO: reuse existing UID lock "/packageName" or introduce new one "/npmMetadata/packageName"?
    final RepositoryItemUidLock lock = repository.createUid(packageRoot.getName()).getLock();
    lock.lock(Action.update);
    try (ODatabaseDocumentTx db = db()) {
      db.begin();
      try {
        return doUpdatePackage(db, entityHandler, repository, packageRoot, overlay);
      }
      finally {
        db.commit();
      }
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public int updatePackages(final NpmRepository repository, final Iterator<PackageRoot> packageRootIterator) {
    checkNotNull(repository);
    checkNotNull(packageRootIterator);
    final EntityHandler<PackageRoot> entityHandler = getHandlerFor(PackageRoot.class);
    try (ODatabaseDocumentTx db = db()) {
      int count = 0;
      try {
        while (packageRootIterator.hasNext()) {
          final PackageRoot packageRoot = packageRootIterator.next();
          db.begin();
          doUpdatePackage(db, entityHandler, repository, packageRoot, true);
          db.commit();
          count++;
        }
      }
      catch (Exception e) {
        db.rollback();
        throw Throwables.propagate(e);
      }
      return count;
    }
  }

  @Override
  public int updatePackages(final NpmRepository repository,
                            final Predicate<PackageRoot> predicate,
                            final Function<PackageRoot, PackageRoot> function)
  {
    final int pageSize = 1000;
    final int retries = 3;

    checkNotNull(repository);
    checkNotNull(function);
    final EntityHandler<PackageRoot> entityHandler = getHandlerFor(PackageRoot.class);
    int count = 0;
    try (ODatabaseDocumentTx db = db()) {
      final OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
          "select @rid as orid from " + entityHandler.getSchemaName() + " where repositoryId='" + repository.getId() +
              "' and @rid > ? limit " + pageSize);
      ORID lastProcessedOrid = new ORecordId();
      List<ODocument> resultset = db.query(query, lastProcessedOrid);
      int retry = 0;
      while (!resultset.isEmpty()) {
        try {
          db.begin();
          for (ODocument d : resultset) {
            lastProcessedOrid = d.field("orid", ORID.class);
            final ODocument npmDoc = db.load(lastProcessedOrid);
            if (npmDoc == null) {
              continue;
            }
            PackageRoot root = entityHandler.toEntity(npmDoc);
            if (predicate != null && !predicate.apply(root)) {
              continue;
            }
            root = function.apply(root);
            db.save(entityHandler.toDocument(root, npmDoc));
            count++;
          }
          db.commit();
          retry = 0;
        }
        catch (OConcurrentModificationException e) {
          db.rollback();
          retry++;
          if (retry < retries) {
            log.info("Failed update on {} packages for repository {} due to concurrent access to record {}, retrying {}/{}",
                pageSize, repository, e.getRid(), retry, retries);
          }
          else {
            retry = 0;
            log.info("Failed update {} times on {} packages for repository {} due to concurrent access to record {}, skipping page",
                retries, pageSize, repository, e.getRid());
          }
        }
        if (retry == 0) {
          resultset = db.query(query, lastProcessedOrid);
        }
      }
    }
    return count;
  }

  // ==

  private ODocument doGetPackageByName(final ODatabaseDocumentTx db,
                                       final EntityHandler<PackageRoot> entityHandler,
                                       final NpmRepository repository,
                                       final String packageName)
  {
    final List<ODocument> entries = db.query(new OSQLSynchQuery<>(
        "select * from " + entityHandler.getSchemaName() + " where componentId='" + repository.getId() + ":" +
            packageName + "'"));
    for (ODocument entry : entries) {
      return entry; // we expect only one
    }
    return null;
  }

  private PackageRoot doUpdatePackage(final ODatabaseDocumentTx db,
                                      final EntityHandler<PackageRoot> entityHandler,
                                      final NpmRepository repository,
                                      final PackageRoot packageRoot,
                                      final boolean overlay)
  {
    ODocument doc = doGetPackageByName(db, entityHandler, repository, packageRoot.getName());
    if (doc == null) {
      doc = db.newInstance(entityHandler.getSchemaName());
    }
    else if (overlay) {
      final PackageRoot existing = entityHandler.toEntity(doc);
      existing.overlay(packageRoot);
      db.save(entityHandler.toDocument(existing, doc));
      return existing;
    }
    db.save(entityHandler.toDocument(packageRoot, doc));
    return packageRoot;
  }

}
