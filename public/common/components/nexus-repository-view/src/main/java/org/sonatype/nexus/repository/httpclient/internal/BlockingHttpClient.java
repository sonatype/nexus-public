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

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.common.sequence.FibonacciNumberSequence;
import org.sonatype.nexus.common.sequence.NumberSequence;
import org.sonatype.nexus.repository.httpclient.AutoBlockConfiguration;
import org.sonatype.nexus.repository.httpclient.FilteredHttpClientSupport;
import org.sonatype.nexus.repository.httpclient.HttpClientConfig;
import org.sonatype.nexus.repository.httpclient.OutboundRequestMetricRecorder;
import org.sonatype.nexus.repository.httpclient.RemoteBlockedIOException;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusObserver;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Locale.ENGLISH;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
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

  private final OutboundRequestMetricRecorder outboundRequestRecorder;

  private RemoteConnectionStatus status = new RemoteConnectionStatus(UNINITIALISED);

  /**
   * Constructor without outbound request metric recording.
   */
  public BlockingHttpClient(
      final CloseableHttpClient delegate,
      final HttpClientConfig config,
      final RemoteConnectionStatusObserver statusObserver,
      final boolean repositoryOnline,
      final AutoBlockConfiguration autoBlockConfiguration)
  {
    this(delegate, config, statusObserver, repositoryOnline, autoBlockConfiguration, null);
  }

  public BlockingHttpClient(
      final CloseableHttpClient delegate,
      final HttpClientConfig config,
      final RemoteConnectionStatusObserver statusObserver,
      final boolean repositoryOnline,
      final AutoBlockConfiguration autoBlockConfiguration,
      final OutboundRequestMetricRecorder outboundRequestRecorder)
  {
    super(delegate);
    checkNotNull(config);
    this.statusObserver = checkNotNull(statusObserver);
    this.autoBlockConfiguration = checkNotNull(autoBlockConfiguration);
    this.outboundRequestRecorder = outboundRequestRecorder;

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
        if (!autoBlock && statusCode == SC_UNAUTHORIZED) {
          updateStatusToAvailableWithParams(getReason(statusCode), statusCode, target);
        }
        else {
          updateStatusToUnavailable(getReason(statusCode), statusCode, target);
        }
      }
      else {
        updateStatusToAvailable();
        // Record non-blocked outbound request for telemetry
        recordOutboundRequest(filterable);
      }
      return response;
    }
    catch (IOException e) {
      if (isRemoteUnavailable(e)) {
        updateStatusToUnavailable(getReason(e), null, target);
      }
      throw e;
    }
  }

  private void recordOutboundRequest(final Filterable filterable) {
    if (outboundRequestRecorder == null) {
      return;
    }
    try {
      HttpContext context = filterable.getContext();
      String format = "unknown";
      String repositoryType = "unknown";

      if (context != null) {
        Object formatAttr = context.getAttribute(OutboundRequestMetricRecorder.CONTEXT_FORMAT);
        format = formatAttr instanceof String ? (String) formatAttr : "unknown";

        Object repositoryTypeAttr = context.getAttribute(OutboundRequestMetricRecorder.CONTEXT_REPOSITORY_TYPE);
        repositoryType = repositoryTypeAttr instanceof String ? (String) repositoryTypeAttr : "unknown";
      }

      String httpMethod = filterable.getRequest().getRequestLine().getMethod();

      outboundRequestRecorder.record(format, repositoryType, httpMethod);
    }
    catch (Exception e) {
      log.warn("Failed to record outbound request metric", e);
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

  private synchronized void updateStatusToAvailableWithParams(
      final String reason,
      @Nullable final Integer statusCode,
      final HttpHost target)
  {
    if (autoBlock && blockedUntil != null) {
      blockedUntil = null;
      interruptCheckStatusThread();
      autoBlockSequence.reset();
    }
    updateStatus(AVAILABLE, format("(Last Request %s)", reason), statusCode, target.toURI(), false);
  }

  private synchronized void updateStatusToUnavailable(
      final String reason,
      @Nullable final Integer statusCode,
      final HttpHost target)
  {
    if (autoBlock) {
      // avoid some other thread already increased the sequence
      if (blockedUntil == null || blockedUntil.isBeforeNow()) {
        blockedUntil = DateTime.now().plus(autoBlockSequence.next());
        interruptCheckStatusThread();
        String uri = target.toURI();
        // TODO maybe find different means to schedule status checking
        scheduleCheckStatus(uri, blockedUntil);
      }
      updateStatus(AUTO_BLOCKED_UNAVAILABLE, reason, statusCode, target.toURI(),
          blockedUntil.isAfter(status.getBlockedUntil()));
    }
    else {
      updateStatus(UNAVAILABLE, reason, statusCode, target.toURI(), false);
    }
  }

  @VisibleForTesting
  void scheduleCheckStatus(final String uri, final DateTime until) {
    checkStatusThread = new Thread(new CheckStatus(uri, until), "Check Status " + uri);
    checkStatusThread.setDaemon(true);
    checkStatusThread.start();
  }

  private void updateStatus(
      final RemoteConnectionStatusType type,
      final String reason,
      @Nullable final Integer statusCode,
      @Nullable final String url,
      final boolean autoBlockTimeIncrease)
  {
    if (type != status.getType() || autoBlockTimeIncrease) {
      RemoteConnectionStatus oldStatus = status;
      status = new RemoteConnectionStatus(type, reason)
          .setStatusCode(statusCode)
          .setBlockedUntil(blockedUntil)
          .setRequestUrl(url);
      statusObserver.onStatusChanged(oldStatus, status);
    }
  }

  private void updateStatus(final RemoteConnectionStatusType type) {
    updateStatus(type, null, null, null, false);
  }

  public RemoteConnectionStatus getStatus() {
    return status;
  }

  public void setRemoteConnectionStatus(final RemoteConnectionStatus status) {
    this.status = status;
  }

  /**
   * Records a connection failure that occurred against the remote, mirroring the IOException
   * catch in {@link #filter(HttpHost, Filterable)} for facets that issue their own HTTP
   * requests outside {@code filter()}. When the failure signals the remote is unavailable
   * (see {@link #isRemoteUnavailable(Exception)}), the remote status is updated: if
   * auto-block is enabled, the auto-block timer is advanced and the status becomes
   * {@code AUTO_BLOCKED_UNAVAILABLE}; otherwise the status becomes {@code UNAVAILABLE}.
   *
   * <p>
   * Unlike {@link #filter(HttpHost, Filterable)}, this method does <b>not</b> gate on
   * {@code mainTarget}: the passed-in {@code target} is used verbatim both to display the
   * failing URL and as the host the reconnect-probe HEAD is issued against
   * (see {@link #scheduleCheckStatus(String, org.joda.time.DateTime)}). Callers must therefore
   * pass a host that is meaningful to auto-block — normally the repository's configured
   * remote (the {@code mainTarget}), not a helper endpoint on a different host (e.g. an
   * OAuth realm distinct from the registry). Recording against an unrelated host causes the
   * probe to check the wrong URL and the auto-block state's {@code requestUrl} to be
   * misleading.
   *
   * @param failure the IOException raised by the failing remote call
   * @param target the remote host the failed call was directed at; should be the repository's
   *          configured remote
   */
  public void recordConnectionFailure(final IOException failure, final HttpHost target) {
    if (isRemoteUnavailable(failure)) {
      updateStatusToUnavailable(getReason(failure), null, target);
    }
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
    return reason == null ? "Unrecognized HTTP error, code " + statusCode : reason;
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
