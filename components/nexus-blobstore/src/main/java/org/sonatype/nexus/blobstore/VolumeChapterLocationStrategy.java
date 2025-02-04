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

import org.sonatype.nexus.blobstore.api.BlobId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores blobs in a two-deep directory tree.
 *
 * The first layer, {@code vol}, having {@link #TIER_1_MODULO} directories,
 * and the second {@code chap} having {@link #TIER_2_MODULO}.
 *
 * @since 3.0
 */
public class VolumeChapterLocationStrategy
    extends LocationStrategySupport
{
  private static final int TIER_1_MODULO = 43;

  private static final int TIER_2_MODULO = 47;

  @Override
  public String location(final BlobId blobId) {
    checkNotNull(blobId);

    return String.format("vol-%02d/chap-%02d/%s",
        tier(blobId, TIER_1_MODULO),
        tier(blobId, TIER_2_MODULO),
        escapeFilename(blobId.asUniqueString()));
  }

  private int tier(final BlobId blobId, final int modulo) {
    return Math.abs(blobId.hashCode() % modulo) + 1;
  }
}
