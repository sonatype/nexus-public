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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import static java.lang.String.format;

/**
 * Inform on the health of all BlobStores based on their lifecycle state.
 */
@Named("Blob Stores Ready")
@Singleton
public class BlobStoreStateHealthCheck
    extends HealthCheck
{
  private final Provider<BlobStoreManager> blobStoreManagerProvider;

  @Inject
  public BlobStoreStateHealthCheck(final Provider<BlobStoreManager> blobStoreManagerProvider)
  {
    this.blobStoreManagerProvider = Preconditions.checkNotNull(blobStoreManagerProvider);
  }

  @Override
  protected Result check() {
    final List<String> blobStores = StreamSupport.stream(blobStoreManagerProvider.get().browse().spliterator(), false)
        .map(blobStore -> {
          final String name = blobStore.getBlobStoreConfiguration().getName();
          if (!blobStore.isStarted()) {
            return format("Blob store '%s' reports as not started", name);
          }
          if (!blobStore.getBlobStoreConfiguration().getType().equals(BlobStoreGroup.TYPE) &&
              !blobStore.isWritable()) {
            return format("Blob store '%s' reports as not writeable", name);
          }
          if (!blobStore.isStorageAvailable()) {
            return format("Blob store '%s' reports as not available", name);
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    String message = format("%s/%s blob stores report issues<br>%s", blobStores.size(),
        Iterators.size(blobStoreManagerProvider.get().browse().iterator()),
        String.join("<br>", blobStores));

    return blobStores.isEmpty() ? Result.healthy(message): Result.unhealthy(message);
  }
}
