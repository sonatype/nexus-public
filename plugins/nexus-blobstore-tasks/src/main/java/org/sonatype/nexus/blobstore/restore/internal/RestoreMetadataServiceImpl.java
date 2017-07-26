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
package org.sonatype.nexus.blobstore.restore.internal;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.blobstore.restore.RestoreMetadataService;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.storage.Bucket.REPO_NAME_HEADER;

/**
 * @since 3.4
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class RestoreMetadataServiceImpl
    extends StateGuardLifecycleSupport
    implements RestoreMetadataService
{
  private final BlobStoreManager blobStoreManager;

  private final RepositoryManager repositoryManager;

  private final Map<String, RestoreBlobStrategy> restoreBlobStrategies;

  @Inject
  public RestoreMetadataServiceImpl(final BlobStoreManager blobStoreManager,
                                    final RepositoryManager repositoryManager,
                                    final Map<String, RestoreBlobStrategy> restoreBlobStrategies)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.restoreBlobStrategies = checkNotNull(restoreBlobStrategies);
  }

  @Override
  @Guarded(by = STARTED)
  public void restore(final String blobStoreName) {
    BlobStore store = blobStoreManager.get(blobStoreName);

    if (store instanceof FileBlobStore) {
      FileBlobStore fileBlobStore = (FileBlobStore) store;
      fileBlobStore.getBlobIdStream().forEach(blobId ->
          buildContext(blobStoreName, fileBlobStore, blobId)
          .ifPresent(c -> c.restoreBlobStrategy.restore(c.properties, c.blob, c.blobStoreName))
      );
    }
    else {
      log.error("Blob store does not support rebuild: {}", blobStoreName);
    }
  }

  private Optional<Context> buildContext(final String blobStoreName, final FileBlobStore fileBlobStore,
                                         final BlobId blobId)
  {
    return Optional.of(new Context(blobStoreName, fileBlobStore, blobId))
        .map(c -> c.blob(c.fileBlobStore.get(c.blobId)))
        .map(c -> c.blobAttributes(c.fileBlobStore.getBlobAttributes(c.blobId)))
        .map(c -> c.properties(c.blobAttributes.getProperties()))
        .map(c -> c.repositoryName(c.properties.getProperty(HEADER_PREFIX + REPO_NAME_HEADER)))
        .map(c -> c.repository(repositoryManager.get(c.repositoryName)))
        .map(c -> c.restoreBlobStrategy(restoreBlobStrategies.get(c.repository.getFormat().getValue())));
  }

  private static class Context {
    final String blobStoreName;

    final FileBlobStore fileBlobStore;

    final BlobId blobId;

    Blob blob;

    BlobAttributes blobAttributes;

    Properties properties;

    String repositoryName;

    Repository repository;

    RestoreBlobStrategy restoreBlobStrategy;

    Context(final String blobStoreName, final FileBlobStore fileBlobStore, final BlobId blobId) {
      this.blobStoreName = checkNotNull(blobStoreName);
      this.fileBlobStore = checkNotNull(fileBlobStore);
      this.blobId = checkNotNull(blobId);
    }

    Context blob(final Blob blob) {
      if (blob == null) {
        return null;
      }
      else {
        this.blob = blob;
        return this;
      }
    }

    Context blobAttributes(final BlobAttributes blobAttributes) {
      if (blobAttributes == null || blobAttributes.isDeleted()) {
        return null;
      }
      else {
        this.blobAttributes = blobAttributes;
        return this;
      }
    }

    Context properties(final Properties properties) {
      if (properties == null) {
        return null;
      }
      else {
        this.properties = properties;
        return this;
      }
    }

    Context repositoryName(final String repositoryName) {
      if (repositoryName == null) {
        return null;
      }
      else {
        this.repositoryName = repositoryName;
        return this;
      }
    }

    Context repository(final Repository repository) {
      if (repository == null) {
        return null;
      }
      else {
        this.repository = repository;
        return this;
      }
    }

    Context restoreBlobStrategy(final RestoreBlobStrategy restoreBlobStrategy) {
      if (restoreBlobStrategy == null) {
        return null;
      }
      else {
        this.restoreBlobStrategy = restoreBlobStrategy;
        return this;
      }
    }
  }
}
