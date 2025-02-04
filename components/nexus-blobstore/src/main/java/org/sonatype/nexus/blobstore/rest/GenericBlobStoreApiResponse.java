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
package org.sonatype.nexus.blobstore.rest;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.19
 */
public class GenericBlobStoreApiResponse
    extends BlobStoreApiModel
{
  private String name;

  private String type;

  private boolean unavailable;

  private long blobCount;

  private long totalSizeInBytes;

  private long availableSpaceInBytes;

  @SuppressWarnings("unused") // Required for ITs
  public GenericBlobStoreApiResponse() {
    super();
  }

  public GenericBlobStoreApiResponse(final BlobStore blobStore) {
    this(blobStore.getBlobStoreConfiguration(), blobStore);
  }

  public GenericBlobStoreApiResponse(final BlobStoreConfiguration configuration, @Nullable final BlobStore blobStore) {
    super(configuration);

    if (blobStore != null && blobStore.isStarted()) {
      BlobStoreMetrics metrics = blobStore.getMetrics();
      unavailable = metrics.isUnavailable();
      blobCount = metrics.getBlobCount();
      totalSizeInBytes = metrics.getTotalSize();
      availableSpaceInBytes = metrics.getAvailableSpace();
    }

    name = checkNotNull(configuration.getName());
    setType(checkNotNull(configuration.getType()));
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public boolean isUnavailable() {
    return unavailable;
  }

  public void setUnavailable(final boolean unavailable) {
    this.unavailable = unavailable;
  }

  public long getBlobCount() {
    return blobCount;
  }

  public void setBlobCount(final long blobCount) {
    this.blobCount = blobCount;
  }

  public long getTotalSizeInBytes() {
    return totalSizeInBytes;
  }

  public void setTotalSizeInBytes(final long totalSizeInBytes) {
    this.totalSizeInBytes = totalSizeInBytes;
  }

  public long getAvailableSpaceInBytes() {
    return availableSpaceInBytes;
  }

  public void setAvailableSpaceInBytes(final long availableSpaceInBytes) {
    this.availableSpaceInBytes = availableSpaceInBytes;
  }
}
