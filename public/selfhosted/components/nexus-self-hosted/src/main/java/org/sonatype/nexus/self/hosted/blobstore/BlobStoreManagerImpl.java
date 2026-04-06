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
package org.sonatype.nexus.self.hosted.blobstore;

import java.util.List;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.DefaultBlobStoreProvider;
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.internal.blobstore.BaseBlobStoreManager;
import org.sonatype.nexus.repository.internal.blobstore.BlobStoreOverride;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.replication.ReplicationBlobStoreStatusManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * The {@link org.sonatype.nexus.blobstore.api.BlobStoreManager} implementation for Self-Hosted deployments.
 *
 * No overrides needed from the {@link BaseBlobStoreManager}, just annotations needed for DI.
 */
@Component
@Singleton
@ManagedObject
@ManagedLifecycle(phase = Phase.STORAGE)
public class BlobStoreManagerImpl
    extends BaseBlobStoreManager
{
  @Inject
  public BlobStoreManagerImpl(
      final EventManager eventManager, // NOSONAR
      final BlobStoreConfigurationStore store,
      final @Lazy List<BlobStoreDescriptor> blobStoreDescriptors,
      final List<Provider<BlobStore>> blobStorePrototypes,
      final @Lazy Provider<RepositoryManager> repositoryManagerProvider,
      final NodeAccess nodeAccess,
      @Nullable @Value("${nexus.blobstore.provisionDefaults:#{null}}") final Boolean provisionDefaults,
      @Nullable final DefaultBlobStoreProvider blobstoreProvider,
      final BlobStoreTaskService blobStoreTaskService,
      final Provider<BlobStoreOverride> blobStoreOverrideProvider,
      final ReplicationBlobStoreStatusManager replicationBlobStoreStatusManager,
      final SecretsService secretService)
  {
    super(eventManager, store, blobStoreDescriptors, blobStorePrototypes,
        repositoryManagerProvider, nodeAccess, provisionDefaults, blobstoreProvider, blobStoreTaskService,
        blobStoreOverrideProvider, replicationBlobStoreStatusManager, secretService);
  }
}
