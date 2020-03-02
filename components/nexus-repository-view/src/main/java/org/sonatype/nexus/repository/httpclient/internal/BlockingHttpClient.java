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
package org.sonatype.nexus.repository.httpclient.internal;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.sequence.FibonacciNumberSequence;
import org.sonatype.nexus.common.sequence.NumberSequence;
import org.sonatype.nexus.repository.httpclient.AutoBlockConfiguration;
import org.sonatype.nexus.repository.httpclient.FilteredHttpClientSupport;
import org.sonatype.nexus.repository.httpclient.RemoteBlockedIOException;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusObserver;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType;
import org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.Config;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.client.CloseableHttpClient;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.currentThread;
import static java.util.Locale.ENGLISH;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.AUTO_BLOCKED_UNAVAILABLE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.AVAILABLE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.BLOCKED;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.OFFLINE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.READY;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.UNAVAILABLE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.UNINITIALISED;

/**
 * Wraps an {@link CloseableHttpClient} with manual and automatic blocking functionality.
 *
 * @since 3.0
 */
public class BlockingHttpClient
    extends FilteredHttpClientSupport
{
  private static final Logger log = LoggerFactory.getLogger(BlockingHttpClient.class);

  private final boolean blocked;

  @VisibleForTesting
  final AutoBlockConfiguration autoBlockConfiguration;

  private HttpHost mainTarget;

  private DateTime blockedUntil;

  private Thread checkStatusThread;

  private final boolean autoBlock;

  private final NumberSequence autoBlockSequence;

  private final RemoteConnectionStatusObserver statusObserver;

  private RemoteConnectionStatus status = new RemoteConnectionStatus(UNINITIALISED);

  public BlockingHttpClient(final CloseableHttpClient delegate,
                            final Config config,
                            final RemoteConnectionStatusObserver statusObserver,
                            final boolean repositoryOnline,
                            final AutoBlockConfiguration autoBlockConfiguration)
  {
    super(delegate);
    checkNotNull(config);
    this.statusObserver = checkNotNull(statusObserver);
    this.autoBlockConfiguration = checkNotNull(autoBlockConfiguration);
    
    blocked = config.blocked != null ? config.blocked : false;
    autoBlock = config.autoBlock != null ? config.autoBlock : false;
    if (repositoryOnline) {
      updateStatus(blocked ? BLOCKED : READY);
    }
    else {
      updateStatus(OFFLINE);
    }
    // TODO shall we use config.getConnectionConfig().getTimeout() * 2 as in NX2?
    autoBlockSequence = new FibonacciNumberSequence(Time.seconds(40).toMillis());
  }

  protected CloseableHttpResponse filter(final HttpHost target, final Filterable filterable) throws IOException {
    // main target is the first accessed target
    if (mainTarget == null) {
      mainTarget = target;
    }
    // we only filter requests to our main target
    if (!target.equals(mainTarget)) {
      return filterable.call();
    }
    if (blocked) {
      throw new RemoteBlockedIOException("Remote Manually Blocked");
    }
    DateTime blockedUntilCopy = this.blockedUntil;
    if (autoBlock && blockedUntilCopy != null && blockedUntilCopy.isAfterNow()) {
      throw new RemoteBlockedIOException("Remote Auto Blocked until " + blockedUntilCopy);
    }

    try {
      CloseableHttpResponse response = filterable.call();
      int statusCode = response.getStatusLine().getStatusCode();

      if (autoBlockConfiguration.shouldBlock(statusCode)) {
        updateStatusToUnavailable(getReason(statusCode), target);
      }
      else {
        updateStatusToAvailable();
      }
      return response;
    }
    catch (IOException e) {
      if (isRemoteUnavailable(e)) {
        updateStatusToUnavailable(getReason(e), target);
      }
      throw e;
    }
  }

  private synchronized void updateStatusToAvailable() {
    if (autoBlock && blockedUntil != null) {
      blockedUntil = null;
      interruptCheckStatusThread();
      autoBlockSequence.reset();
    }
    updateStatus(AVAILABLE);
  }

  private synchronized void updateStatusToUnavailable(final String reason, final HttpHost target) {
    if (autoBlock) {
      // avoid some other thread already increased the sequence
      if (blockedUntil == null || blockedUntil.isBeforeNow()) {
        blockedUntil = DateTime.now().plus(autoBlockSequence.next());
        interruptCheckStatusThread();
        String uri = target.toURI();
        // TODO maybe find different means to schedule status checking
        scheduleCheckStatus(uri, blockedUntil);
      }
      updateStatus(AUTO_BLOCKED_UNAVAILABLE, reason, target.toURI(), blockedUntil.isAfter(status.getBlockedUntil()));
    }
    else {
      updateStatus(UNAVAILABLE, reason, target.toURI(), false);
    }
  }

  @VisibleForTesting
  void scheduleCheckStatus(final String uri, final DateTime until) {
    checkStatusThread = new Thread(new CheckStatus(uri, until), "Check Status " + uri);
    checkStatusThread.setDaemon(true);
    checkStatusThread.start();
  }

  private void updateStatus(final RemoteConnectionStatusType type,
                            final String reason,
                            @Nullable final String url,
                            final boolean autoBlockTimeIncrease)
  {
    if (type != status.getType() || autoBlockTimeIncrease) {
      RemoteConnectionStatus oldStatus = status;
      status = new RemoteConnectionStatus(type, reason)
          .setBlockedUntil(blockedUntil)
          .setRequestUrl(url);
      statusObserver.onStatusChanged(oldStatus, status);
    }
  }

  private void updateStatus(final RemoteConnectionStatusType type) {
    updateStatus(type, null, null, false);
  }

  public RemoteConnectionStatus getStatus() {
    return status;
  }

  private boolean isRemoteUnavailable(final Exception e) {
    if (e instanceof ConnectionPoolTimeoutException) {
      return false;
    }
    return true;
  }

  private String getReason(final Exception e) {
    if (e instanceof SSLPeerUnverifiedException) {
      return "Untrusted Remote";
    }
    return e.getClass().getName() + ": " + e.getMessage();
  }

  private String getReason(final int statusCode) {
    String reason = EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, ENGLISH);
    return reason == null ? "Unrecognized HTTP error" : reason;
  }

  @Override
  public void close() throws IOException {
    interruptCheckStatusThread();
    super.close();
  }

  private void interruptCheckStatusThread() {
    if (checkStatusThread != null) {
      // avoid self-interrupt (status may change during thread's HEAD request)
      if (checkStatusThread != currentThread()) {
        checkStatusThread.interrupt();
      }
      checkStatusThread = null;
    }
  }

  private class CheckStatus
      implements Runnable
  {
    private final String uri;

    private final DateTime fireAt;

    private CheckStatus(final String uri, final DateTime fireAt) {
      this.uri = uri;
      this.fireAt = fireAt;
    }

    @Override
    public void run() {
      if (fireAt.isAfterNow()) {
        try {
          long durationTillFire = new Duration(DateTime.now(), fireAt).getMillis();
          if (durationTillFire > 0) {
            log.debug("Wait until {} to check status of {}", fireAt, uri);
            Thread.sleep(durationTillFire);
            log.debug("Time is up. Checking status of {}", uri);
            execute(new HttpHead(uri));
          }
        }
        catch (InterruptedException e) {
          log.debug("Stopped checking status of {}", uri);
        }
        catch (IOException e) {
          // ignore as we just want to access the host
        }
      }
    }
  }

}
