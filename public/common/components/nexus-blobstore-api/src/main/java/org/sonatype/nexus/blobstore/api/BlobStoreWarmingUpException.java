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
package org.sonatype.nexus.blobstore.api;

/**
 * Exception thrown when a blob store is still warming up connection pools during startup.
 *
 * This exception indicates a temporary, retry-able condition where the blob store's
 * connection pool (S3/Azure) is still being initialized. Clients should retry the request
 * after a brief delay (typically 1-2 seconds).
 *
 * This is distinct from {@link org.sonatype.nexus.common.stateguard.InvalidStateException}
 * which indicates permanent invalid states, and from {@link org.sonatype.nexus.repository.MissingBlobException}
 * which indicates data corruption.
 */
public class BlobStoreWarmingUpException
    extends RuntimeException
{
  private final String blobStoreName;

  public BlobStoreWarmingUpException(final String blobStoreName) {
    super("Blob store '" + blobStoreName + "' is still warming up connection pools, please retry in a moment");
    this.blobStoreName = blobStoreName;
  }

  public String getBlobStoreName() {
    return blobStoreName;
  }
}
