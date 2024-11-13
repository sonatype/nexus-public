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
package org.sonatype.nexus.coreui.internal.blobstore;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;

import static com.google.common.base.Preconditions.checkNotNull;

public class BlobStoreUIResponse
{
  private final String name;

  private final String typeId;

  private final String typeName;

  private final String path;

  private final boolean unavailable;

  private final long blobCount;

  private final long totalSizeInBytes;

  private final long availableSpaceInBytes;

  private final boolean unlimited;

  public BlobStoreUIResponse(
      final String typeId,
      final BlobStoreConfiguration configuration,
      @Nullable final BlobStoreMetrics metrics,
      final String path)
  {
    if (metrics != null) {
      unavailable = metrics.isUnavailable();
      blobCount = metrics.getBlobCount();
      totalSizeInBytes = metrics.getTotalSize();
      availableSpaceInBytes = metrics.getAvailableSpace();
      unlimited = metrics.isUnlimited();
    }
    else {
      unavailable = true;
      blobCount = 0;
      totalSizeInBytes = 0;
      availableSpaceInBytes = 0;
      unlimited = false;
    }

    name = checkNotNull(configuration.getName());

    this.typeId = checkNotNull(typeId);
    typeName = configuration.getType();
    this.path = checkNotNull(path);
  }

  public String getName() {
    return name;
  }

  public String getTypeId() {
    return typeId;
  }

  public String getTypeName() {
    return typeName;
  }

  public String getPath() {
    return path;
  }

  public boolean isUnavailable() {
    return unavailable;
  }

  public long getBlobCount() {
    return blobCount;
  }

  public long getTotalSizeInBytes() {
    return totalSizeInBytes;
  }

  public long getAvailableSpaceInBytes() {
    return availableSpaceInBytes;
  }

  public boolean isUnlimited() {
    return unlimited;
  }
}
