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
package org.sonatype.nexus.blobstore.internal;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobIndex;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobsStore;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import org.springframework.core.Ordered;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

@Primary
@Component
@Qualifier("default")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SoftDeletedBlobIndexImpl
    extends StateGuardLifecycleSupport
    implements SoftDeletedBlobIndex
{
  protected final SoftDeletedBlobsStore softDeletedBlobsStore;

  protected String blobStoreName;

  @Autowired
  public SoftDeletedBlobIndexImpl(final SoftDeletedBlobsStore softDeletedBlobsStore) {
    this.softDeletedBlobsStore = checkNotNull(softDeletedBlobsStore);
  }

  @Override
  public void init(final BlobStore blobStore) {
    checkNotNull(blobStore);
    String newBlobStoreName = checkNotNull(blobStore.getBlobStoreConfiguration().getName());
    checkState(blobStoreName == null || Objects.equals(newBlobStoreName, blobStoreName), "Previously initialized");

    if (blobStoreName != null) {
      return;
    }

    blobStoreName = newBlobStoreName;
    try {
      start();
    }
    catch (Exception e) {
      if (e instanceof RuntimeException re) {
        throw re;
      }
      throw new IllegalArgumentException(e);
    }
  }

  @Guarded(by = STARTED)
  @Override
  public final void createRecord(final BlobId blobId) {
    softDeletedBlobsStore.createRecord(blobId, blobStoreName);
  }

  @Guarded(by = STARTED)
  @Override
  public final Stream<BlobId> getRecordsBefore(final OffsetDateTime blobsOlderThan) {
    return softDeletedBlobsStore.getBlobsBefore(blobStoreName, blobsOlderThan);
  }

  @Guarded(by = STARTED)
  @Override
  public final void deleteRecord(final BlobId blobId) {
    softDeletedBlobsStore.deleteRecord(blobStoreName, blobId);
  }

  @Guarded(by = STARTED)
  @Override
  public final void deleteAllRecords() {
    softDeletedBlobsStore.deleteAllRecords(blobStoreName);
  }

  @Guarded(by = STARTED)
  @Override
  public final int size() {
    return softDeletedBlobsStore.count(blobStoreName);
  }

  @Guarded(by = STARTED)
  @Override
  public int count(final OffsetDateTime blobsBefore) {
    return softDeletedBlobsStore.countBefore(blobStoreName, blobsBefore);
  }
}
