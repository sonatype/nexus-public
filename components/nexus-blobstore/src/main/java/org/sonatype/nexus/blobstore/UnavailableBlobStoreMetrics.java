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

import java.util.Collections;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;

/**
 * An implementation of {@link BlobStoreMetrics} indicating the metrics can't be loaded.
 *
 * @since 3.19
 */
public class UnavailableBlobStoreMetrics
    implements BlobStoreMetrics
{
  private static final UnavailableBlobStoreMetrics INSTANCE = new UnavailableBlobStoreMetrics();

  private UnavailableBlobStoreMetrics() {
  }

  public static UnavailableBlobStoreMetrics getInstance() {
    return INSTANCE;
  }

  @Override
  public long getBlobCount() {
    return 0L;
  }

  @Override
  public long getTotalSize() {
    return 0L;
  }

  @Override
  public long getAvailableSpace() {
    return 0L;
  }

  @Override
  public boolean isUnlimited() {
    return false;
  }

  @Override
  public Map<String, Long> getAvailableSpaceByFileStore() {
    return Collections.emptyMap();
  }

  @Override
  public boolean isUnavailable() {
    return true;
  }
}
