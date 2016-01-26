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
package org.sonatype.nexus.blobstore.file.internal;

import java.nio.file.Path;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;

/**
 * A service to store blobstore-level metrics.
 *
 * @since 3.0
 */
public interface BlobStoreMetricsStore
    extends Lifecycle
{
  void setStorageDir(Path blobstoreRoot);

  /**
   * Provide the current metrics. This is an estimate.
   */
  BlobStoreMetrics getMetrics();

  /**
   * Indicate that a blob of the given size has been added.
   */
  void recordAddition(long size);

  /**
   * Indicate that a blob of the given size has been removed.
   */
  void recordDeletion(long size);
}
