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
package org.sonatype.nexus.internal.blobstore;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreConfigurationStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.jmx.reflect.ManagedAttribute;
import org.sonatype.nexus.jmx.reflect.ManagedObject;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Default {@link BlobStoreManager} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
@ManagedObject
public class BlobStoreManagerImpl
    extends StateGuardLifecycleSupport
    implements BlobStoreManager
{
  private static final String BASEDIR = "blobs";

  private final Path basedir;

  private final Map<String, BlobStore> stores = Maps.newHashMap();

  private final BlobStoreConfigurationStore store;

  private final Map<String, Provider<BlobStore>> blobstorePrototypes;

  @Inject
  public BlobStoreManagerImpl(final ApplicationDirectories directories, final BlobStoreConfigurationStore store,
                              Map<String, Provider<BlobStore>> blobstorePrototypes)
  {
    checkNotNull(directories);
    this.basedir = directories.getWorkDirectory(BASEDIR).toPath();
    this.store = checkNotNull(store);
    this.blobstorePrototypes = checkNotNull(blobstorePrototypes);
  }

  @ManagedAttribute
  public File getBasedir() {
    return basedir.toFile();
  }

  @Override
  protected void doStart() throws Exception {
    store.start();
    List<BlobStoreConfiguration> configurations = store.list();
    if (configurations.isEmpty()) {
      log.debug("No BlobStores configured; provisioning default BlobStore");
      store.create(FileBlobStore.configure(
          DEFAULT_BLOBSTORE_NAME, basedir.toAbsolutePath().resolve(DEFAULT_BLOBSTORE_NAME).toString()
      ));
      configurations = store.list();
    }

    log.debug("Restoring {} BlobStores", configurations.size());
    for (BlobStoreConfiguration configuration : configurations) {
      log.debug("Restoring BlobStore: {}", configuration);
      BlobStore blobStore = newBlobStore(configuration);
      track(configuration.getName(), blobStore);

      // TODO - event publishing
    }

    log.debug("Starting {} BlobStores", stores.size());
    for (BlobStore blobStore : stores.values()) {
      log.debug("Starting BlobStore: {}", blobStore);
      blobStore.start();

      // TODO - event publishing
    }
  }

  @Override
  protected void doStop() throws Exception {
    if (stores.isEmpty()) {
      log.debug("No BlobStores defined");
      return;
    }

    log.debug("Stopping {} BlobStores", stores.size());
    for (Map.Entry<String, BlobStore> entry : stores.entrySet()) {
      String name = entry.getKey();
      BlobStore store = entry.getValue();
      log.debug("Stopping blob-store: {}", name);
      store.stop();
    }

    stores.clear();
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<BlobStore> browse() {
    return ImmutableList.copyOf(stores.values());
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStore create(final BlobStoreConfiguration configuration) throws Exception {
    checkNotNull(configuration);
    log.debug("Creating BlobStore: {} with attributes: {}", configuration.getName(),
        configuration.getAttributes());

    store.create(configuration);

    BlobStore blobStore;
    try {
      blobStore = newBlobStore(configuration);
    } catch (Exception e) {
      store.delete(configuration);
      throw e;
    }

    track(configuration.getName(), blobStore);

    blobStore.start();
    //TODO - event publishing

    return blobStore;
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(BlobStoreConfiguration configuration) throws Exception {
    checkNotNull(configuration);

    log.debug("Deleting BlobStore: {}", configuration);
    BlobStore blobStore = blobStore(configuration.getName());
    blobStore.stop();
    store.delete(configuration);
    untrack(configuration.getName());

    //TODO - event publishing
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStore get(final String name) {
    checkNotNull(name);

    return stores.get(name);
  }

  @Override
  public void delete(final String name) throws Exception {
    checkNotNull(name);
    BlobStore blobStore = blobStore(name);
    blobStore.stop();
    //TODO cleanup blobStore?
    untrack(name);
    store.delete(blobStore.getBlobStoreConfiguration());
  }

  private BlobStore newBlobStore(final BlobStoreConfiguration blobStoreConfiguration) throws Exception {
    BlobStore blobStore = blobstorePrototypes.get(blobStoreConfiguration.getType()).get();
    blobStore.init(blobStoreConfiguration);
    return blobStore;
  }

  @VisibleForTesting
  BlobStore blobStore(final String name) {
    BlobStore blobStore = stores.get(name);
    checkState(blobStore != null, "Missing BlobStore: %s", name);
    return blobStore;
  }

  private void track(final String name, final BlobStore blobStore) {
    log.debug("Tracking: {}", name);
    stores.put(name, blobStore);
  }

  private void untrack(final String name) {
    log.debug("Untracking: {}", name);
    stores.remove(name);
  }
}
