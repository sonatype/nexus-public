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
package org.sonatype.nexus.blobstore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Supports the implementation of {@link BlobStore}.
 *
 * @since 3.next
 */
public abstract class BlobStoreSupport
    extends StateGuardLifecycleSupport
    implements BlobStore
{
  protected final BlobIdLocationResolver blobIdLocationResolver;

  protected final DryRunPrefix dryRunPrefix;

  protected BlobStoreConfiguration blobStoreConfiguration;

  public BlobStoreSupport(final BlobIdLocationResolver blobIdLocationResolver,
                          final DryRunPrefix dryRunPrefix)
  {
    this.blobIdLocationResolver = checkNotNull(blobIdLocationResolver);
    this.dryRunPrefix = checkNotNull(dryRunPrefix);
  }

  protected BlobId getBlobId(final Map<String, String> headers, @Nullable final BlobId blobId) {
    return Optional.ofNullable(blobId).orElseGet(() -> blobIdLocationResolver.fromHeaders(headers));
  }

  private void checkIsNotReadOnly() {
    checkState(!isReadOnly(), "Operation not permitted for a read only blob store");
  }

  @Override
  @Guarded(by = STARTED)
  public Blob create(final InputStream blobData, final Map<String, String> headers) {
    return create(blobData, headers, null);
  }

  @Override
  @Guarded(by = STARTED)
  public Blob create(final InputStream blobData, final Map<String, String> headers, @Nullable final BlobId blobId) {
    checkNotNull(blobData);
    checkNotNull(headers);
    checkIsNotReadOnly();

    checkArgument(headers.containsKey(BLOB_NAME_HEADER), "Missing header: %s", BLOB_NAME_HEADER);
    checkArgument(headers.containsKey(CREATED_BY_HEADER), "Missing header: %s", CREATED_BY_HEADER);

    return doCreate(blobData, headers, blobId);
  }

  protected abstract Blob doCreate(InputStream blobData, Map<String, String> headers, @Nullable BlobId blobId);

  @Override
  @Guarded(by = STARTED)
  public boolean delete(final BlobId blobId, final String reason) {
    checkIsNotReadOnly();
    checkNotNull(blobId);

    return doDelete(blobId, reason);
  }

  protected abstract boolean doDelete(BlobId blobId, String reason);

  @Override
  @Guarded(by = STARTED)
  public boolean undelete(@Nullable final BlobStoreUsageChecker inUseChecker, final BlobId blobId,
                          final BlobAttributes attributes,
                          final boolean isDryRun)
  {
    checkIsNotReadOnly();
    checkNotNull(attributes);
    checkIsNotReadOnly();
    checkNotNull(attributes);
    String logPrefix = isDryRun ? dryRunPrefix.get() : "";
    Optional<String> blobName = Optional.of(attributes)
        .map(BlobAttributes::getProperties)
        .map(p -> p.getProperty(HEADER_PREFIX + BLOB_NAME_HEADER));
    if (!blobName.isPresent()) {
      log.error("Property not present: {}, for blob id: {}, at path: {}", HEADER_PREFIX + BLOB_NAME_HEADER,
          blobId, attributePathString(blobId)); // NOSONAR
      return false;
    }
    if (attributes.isDeleted() && inUseChecker != null && inUseChecker.test(this, blobId, blobName.get())) {
      String deletedReason = attributes.getDeletedReason();
      if (!isDryRun) {
        attributes.setDeleted(false);
        attributes.setDeletedReason(null);
        try {
          doUndelete(blobId);
          attributes.store();
        }
        catch (IOException e) {
          log.error("Error while un-deleting blob id: {}, deleted reason: {}, blob store: {}, blob name: {}",
              blobId, deletedReason, blobStoreConfiguration.getName(), blobName.get(), e);
        }
      }
      log.warn(
          "{}Soft-deleted blob still in use, un-deleting blob id: {}, deleted reason: {}, blob store: {}, blob name: {}",
          logPrefix, blobId, deletedReason, blobStoreConfiguration.getName(), blobName.get());
      return true;
    }
    return false;
  }

  protected abstract String attributePathString(BlobId blobId);

  protected void doUndelete(final BlobId blobId) {
    // no-op
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void compact() {
    compact(null);
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void compact(@Nullable final BlobStoreUsageChecker inUseChecker) {
    checkIsNotReadOnly();
    doCompact(inUseChecker);
  }

  protected void doCompact(@Nullable final BlobStoreUsageChecker inUseChecker) {
    // no-op
  }

  @Override
  public void init(BlobStoreConfiguration configuration) {
    this.blobStoreConfiguration = configuration;
    doInit(this.blobStoreConfiguration);
  }

  protected abstract void doInit(BlobStoreConfiguration configuration);

  @Override
  public BlobStoreConfiguration getBlobStoreConfiguration() {
    return this.blobStoreConfiguration;
  }
}
