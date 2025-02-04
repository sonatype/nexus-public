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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * {@link LocationStrategy} for resolving locations to
 * {@link org.sonatype.nexus.blobstore.api.BlobStore#DIRECT_PATH_BLOB_HEADER} blobs.
 *
 * @since 3.8
 */
public class DirectPathLocationStrategy
    extends LocationStrategySupport
{
  public static final String DIRECT_PATH_ROOT = "directpath";

  public static final String DIRECT_PATH_PREFIX = "path$";

  @Override
  public String location(final BlobId blobId) {
    checkNotNull(blobId);
    String realBlobIdPath = blobId.asUniqueString().replace(DIRECT_PATH_PREFIX, "");
    checkArgument(!realBlobIdPath.contains(".."), "Traversal not allowed with direct blobs");
    return format("%s/%s", DIRECT_PATH_ROOT, realBlobIdPath);
  }
}
