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

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.sequence.FibonacciNumberSequence;
import org.sonatype.nexus.common.sequence.NumberSequence;
import org.sonatype.nexus.repository.httpclient.FilteredHttpClientSupport;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusObserver;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.AUTO_BLOCKED_UNAVAILABLE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.AVAILABLE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.BLOCKED;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.READY;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.UNAVAILABLE;

/**
 * Wraps an {@link HttpClient} with manual and automatic blocking functionality.
 *
 * @since 3.0
 */
public class BlockingHttpClient
    extends FilteredHttpClientSupport
    implements HttpClient, Closeable
{
  private final boolean blocked;

  private HttpHost mainTarget;

  private DateTime blockedUntil;

  private Thread checkThread;

  private final boolean autoBlock;

  private final NumberSequence autoBlockSequence;

  private final RemoteConnectionStatusObserver statusObserver;

  private RemoteConnectionStatus status = new RemoteConnectionStatus(READY);

  public BlockingHttpClient(final HttpClient delegate,
                            final HttpClientFacetImpl.Config config,
                            final RemoteConnectionStatusObserver statusObserver)
  {
    super(delegate);
    checkNotNull(config);
    this.statusObserver = checkNotNull(statusObserver);
    blocked = config.blocked != null ? config.blocked : false;
    autoBlock = config.autoBlock != null ? config.autoBlock : false;
    updateStatus(blocked ? BLOCKED : READY);
    // TODO shall we use config.getConnectionConfig().getTimeout() * 2 as in NX2?
    autoBlockSequence = new FibonacciNumberSequence(Time.seconds(40).toMillis());
  }

  protected <T> T filter(final HttpHost target, final Filterable<T> filterable) throws IOException {
    // main target is the first accessed target
    if (mainTarget == null) {
      mainTarget = target;
    }
    // we only filter requests to our main target
    if (!target.equals(mainTarget)) {
      return filterable.call();
    }
    if (blocked) {
      updateStatus(BLOCKED);
      throw new RemoteBlockedIOException("Remote Manually Blocked");
    }
    DateTime blockedUntilCopy = this.blockedUntil;
    if (autoBlock && blockedUntilCopy != null && blockedUntilCopy.isAfterNow()) {
      throw new RemoteBlockedIOException(
          "Remote Auto Blocked until " + blockedUntilCopy +
              " with reason: " + status.getReason());
    }

    Optional<IOException> exception = Optional.empty();
    T result = null;
    try {
      result = filterable.call();
    }
    catch (IOException e) {
      exception = Optional.of(e);
    }

    synchronized (this) {
      if (!exception.isPresent()) {
        if (autoBlock && blockedUntil != null) {
            blockedUntil = null;
            checkThread.interrupt();
            checkThread = null;
            autoBlockSequence.reset();
        }
        updateStatus(AVAILABLE);
      }
      else {
        IOException e = exception.get();
        if (isRemoteUnavailable(e)) {
          if (autoBlock) {
            // avoid some other thread already increased the sequence
            if (blockedUntil == null || blockedUntil.isBeforeNow()) {
              blockedUntil = DateTime.now().plus(autoBlockSequence.next());
              if (checkThread != null) {
                checkThread.interrupt();
              }
              String uri = target.toURI();
              // TODO maybe find different means to schedule status checking
              checkThread = new Thread(new CheckStatus(uri, blockedUntil), "Check Status " + uri);
              checkThread.setDaemon(true);
              checkThread.start();
            }
            updateStatus(AUTO_BLOCKED_UNAVAILABLE, getReason(e));
          }
          else {
            updateStatus(UNAVAILABLE, getReason(e));
          }
        }
      }
    }

    blockedUntilCopy = blockedUntil;
    log.debug(
        "Remote status: {} {}",
        status,
        blockedUntilCopy != null ? "(blocked until " + blockedUntilCopy + ")" : ""
    );

    if (exception.isPresent()) {
      throw exception.get();
    }
    else {
      return result;
    }
  }

  private void updateStatus(final RemoteConnectionStatusType type, final String reason) {
    if (type != status.getType()) {
      RemoteConnectionStatus oldStatus = status;
      status = new RemoteConnectionStatus(type, reason);
      statusObserver.onStatusChanged(oldStatus, status);
    }
  }

  private void updateStatus(final RemoteConnectionStatusType type) {
    updateStatus(type, null);
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
    return e.getMessage();
  }

  @Override
  public void close() throws IOException {
    if (checkThread != null) {
      checkThread.interrupt();
    }
    super.close();
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
