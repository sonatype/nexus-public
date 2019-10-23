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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.sonatype.goodies.common.Locks;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link Blob} implementations with support for locking.
 *
 * @since 3.3
 */
public abstract class BlobSupport
    implements Blob
{
  private final BlobId blobId;

  private final Lock lock;

  private Map<String, String> headers;

  private BlobMetrics metrics;

  private volatile boolean stale;

  public BlobSupport(final BlobId blobId) {
    this.blobId = checkNotNull(blobId);
    lock = new ReentrantLock();
    stale = true;
  }

  public void refresh(final Map<String, String> headers, final BlobMetrics metrics) {
    this.headers = checkNotNull(headers);
    this.metrics = checkNotNull(metrics);
    stale = false;
  }

  public void markStale() {
    stale = true;
  }

  public boolean isStale() {
    return stale;
  }

  @Override
  public BlobId getId() {
    return blobId;
  }

  @Override
  public Map<String, String> getHeaders() {
    return headers;
  }

  @Override
  public BlobMetrics getMetrics() {
    return metrics;
  }

  public Lock lock() {
    return Locks.lock(lock);
  }

  @Override
  public InputStream getInputStream() {
    InputStream inputStream = doGetInputStream();
    if (!inputStream.markSupported()) {
      return new BufferedInputStream(inputStream);
    }
    return inputStream;
  }

  /**
   * Gets the natural input stream for the given blob
   *
   * @since 3.19
   */
  protected abstract InputStream doGetInputStream();
}
