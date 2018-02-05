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
package org.sonatype.nexus.internal.httpclient;

import java.util.concurrent.TimeUnit;

import org.sonatype.goodies.common.Time;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.conn.HttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Low priority daemon thread responsible to evict connection manager pooled connections.
 *
 * @since 2.2
 */
class ConnectionEvictionThread
    extends Thread
{
  private static final Logger log = LoggerFactory.getLogger(ConnectionEvictionThread.class);

  private final HttpClientConnectionManager connectionManager;

  private final long idleTimeMillis;

  private final long evictingDelayMillis;

  @VisibleForTesting
  ConnectionEvictionThread(final HttpClientConnectionManager connectionManager,
                           final long idleTimeMillis,
                           final long evictingDelayMillis)
  {
    super("nexus-httpclient-eviction-thread");
    checkArgument(idleTimeMillis > -1, "Keep alive period in milliseconds cannot be negative");
    checkArgument(evictingDelayMillis > 0, "Evicting delay period in milliseconds must be greater than 0");
    this.connectionManager = checkNotNull(connectionManager);
    this.idleTimeMillis = idleTimeMillis;
    this.evictingDelayMillis = evictingDelayMillis;
    setDaemon(true);
    setPriority(MIN_PRIORITY);
  }

  ConnectionEvictionThread(final HttpClientConnectionManager connectionManager, final Time idleTime, final Time evictingDelayTime) {
    this(connectionManager, idleTime.toMillis(), evictingDelayTime.toMillis());
  }

  @Override
  public void run() {
    log.debug("Starting '{}' (delay {} millis)", getName(), evictingDelayMillis);
    try {
      while (true) {
        synchronized (this) {
          wait(evictingDelayMillis);

          try {
            connectionManager.closeExpiredConnections();
          }
          catch (Exception e) {
            log.warn("Failed to close expired connections", e);
          }

          try {
            connectionManager.closeIdleConnections(idleTimeMillis, TimeUnit.MILLISECONDS);
          }
          catch (Exception e) {
            log.warn("Failed to close idle connections", e);
          }
        }
      }
    }
    catch (InterruptedException e) {
      // ignore
    }

    log.debug("Stopped '{}'", getName());
  }
}
