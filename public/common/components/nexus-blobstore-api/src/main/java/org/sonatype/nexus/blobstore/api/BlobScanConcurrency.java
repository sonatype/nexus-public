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

import java.time.Duration;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * How a caller is willing to fund concurrent blob attribute reads during a bulk scan.
 *
 * @param executor runs the reads, and stays owned by the caller: a blob store submits to it and never shuts it down.
 * @param maxReadsInFlight reads a blob store may leave outstanding, which bounds the heap a scan holds.
 * @param readTimeout how long the scan waits with no read finishing before giving up on those still outstanding. It
 *          bounds the scan stalling, not any one read. Blobs given up on are kept, since the caller reads their
 *          attributes again anyway.
 */
public record BlobScanConcurrency(Executor executor, int maxReadsInFlight, Duration readTimeout)
{
  /**
   * For a caller with no particular executor to size against. Submitting into a bounded pool should instead derive this
   * from that pool, since queueing past what the pool accepts leaves the excess to the thread consuming the scan.
   */
  public static final int DEFAULT_MAX_READS_IN_FLIGHT = 100;

  /**
   * A stalled-scan backstop, not a latency ceiling: one wedged read costs nothing while its neighbours keep finishing.
   * Set above any pause the storage client's own retries can produce. Giving up keeps the blobs, so erring short costs
   * redundant work rather than correctness.
   */
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

  public BlobScanConcurrency {
    requireNonNull(executor);
    checkArgument(maxReadsInFlight >= 1, "maxReadsInFlight must be at least 1, got %s", maxReadsInFlight);
    requireNonNull(readTimeout);
    checkArgument(!readTimeout.isNegative() && !readTimeout.isZero(), "readTimeout must be positive, got %s",
        readTimeout);
  }

  public static BlobScanConcurrency of(final Executor executor) {
    return new BlobScanConcurrency(executor, DEFAULT_MAX_READS_IN_FLIGHT, DEFAULT_READ_TIMEOUT);
  }
}
