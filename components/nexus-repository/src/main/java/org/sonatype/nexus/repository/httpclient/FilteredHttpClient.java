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
package org.sonatype.nexus.repository.httpclient;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.sequence.FibonacciNumberSequence;
import org.sonatype.nexus.common.sequence.NumberSequence;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;

// FIXME: Describe what this class is for in javadocs instead of leaving it empty!

/**
 * @since 3.0
 */
public class FilteredHttpClient
    extends ComponentSupport
    implements HttpClient, Closeable
{

  private final HttpClient delegate;

  private final boolean blocked;

  private HttpHost mainTarget;

  private DateTime blockedUntil;

  private Thread checkThread;

  private final boolean autoBlock;

  private final NumberSequence autoBlockSequence;

  private RemoteConnectionStatus status;

  public FilteredHttpClient(final HttpClient delegate,
                            final HttpClientFacetImpl.Config config)
  {
    this.delegate = checkNotNull(delegate);
    checkNotNull(config);
    blocked = config.blocked != null ? config.blocked : false;
    autoBlock = config.autoBlock != null ? config.autoBlock : false;
    status = new RemoteConnectionStatus(blocked ? "Remote Manually Blocked" : "Remote Connection Pending...");
    // TODO shall we use config.getConnectionConfig().getTimeout() * 2 as in NX2?
    autoBlockSequence = new FibonacciNumberSequence(Time.seconds(40).toMillis());
  }

  private <T> T filter(final HttpHost target, final Filterable<T> filterable) throws IOException {
    // main target is the first accessed target
    if (mainTarget == null) {
      mainTarget = target;
    }
    // we only filter requests to our main target
    if (!target.equals(mainTarget)) {
      return filterable.call();
    }
    if (blocked) {
      throw new IOException("Remote Manually Blocked");
    }
    DateTime blockedUntilCopy = this.blockedUntil;
    if (autoBlock && blockedUntilCopy != null && blockedUntilCopy.isAfterNow()) {
      throw new IOException("Remote Auto Blocked");
    }
    try {
      T result = filterable.call();
      if (autoBlock) {
        synchronized (this) {
          if (blockedUntil != null) {
            blockedUntil = null;
            checkThread.interrupt();
            checkThread = null;
            autoBlockSequence.reset();
          }
        }
      }
      status = new RemoteConnectionStatus("Remote Available");
      return result;
    }
    catch (IOException e) {
      if (isRemoteUnavailable(e)) {
        if (autoBlock) {
          synchronized (this) {
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
          }
          status = new RemoteConnectionStatus("Remote Auto Blocked and Unavailable", getReason(e));
        }
        else {
          status = new RemoteConnectionStatus("Remote Unavailable", getReason(e));
        }
      }
      throw e;
    }
    finally {
      blockedUntilCopy = blockedUntil;
      log.debug(
          "Remote status: {} {}",
          status,
          blockedUntilCopy != null ? "(blocked until " + blockedUntilCopy + ")" : ""
      );
    }
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
  public HttpParams getParams() {
    return delegate.getParams();
  }

  @Override
  public ClientConnectionManager getConnectionManager() {
    return delegate.getConnectionManager();
  }

  @Override
  public HttpResponse execute(final HttpUriRequest request) throws IOException {
    return filter(determineTarget(request), new Filterable<HttpResponse>()
    {
      @Override
      public HttpResponse call() throws IOException {
        return delegate.execute(request);
      }
    });
  }

  @Override
  public HttpResponse execute(final HttpUriRequest request,
                              final HttpContext context)
      throws IOException
  {
    return filter(determineTarget(request), new Filterable<HttpResponse>()
    {
      @Override
      public HttpResponse call() throws IOException {
        return delegate.execute(request, context);
      }
    });
  }

  @Override
  public HttpResponse execute(final HttpHost target,
                              final HttpRequest request)
      throws IOException
  {
    return filter(target, new Filterable<HttpResponse>()
    {
      @Override
      public HttpResponse call() throws IOException {
        return delegate.execute(target, request);
      }
    });
  }

  @Override
  public HttpResponse execute(final HttpHost target,
                              final HttpRequest request,
                              final HttpContext context)
      throws IOException
  {
    return filter(target, new Filterable<HttpResponse>()
    {
      @Override
      public HttpResponse call() throws IOException {
        return delegate.execute(target, request, context);
      }
    });
  }

  @Override
  public <T> T execute(final HttpUriRequest request,
                       final ResponseHandler<? extends T> responseHandler)
      throws IOException
  {
    return filter(determineTarget(request), new Filterable<T>()
    {
      @Override
      public T call() throws IOException {
        return delegate.execute(request, responseHandler);
      }
    });
  }

  @Override
  public <T> T execute(final HttpUriRequest request,
                       final ResponseHandler<? extends T> responseHandler,
                       final HttpContext context)
      throws IOException
  {
    return filter(determineTarget(request), new Filterable<T>()
    {
      @Override
      public T call() throws IOException {
        return delegate.execute(request, responseHandler, context);
      }
    });
  }

  @Override
  public <T> T execute(final HttpHost target,
                       final HttpRequest request,
                       final ResponseHandler<? extends T> responseHandler)
      throws IOException
  {
    return filter(target, new Filterable<T>()
    {
      @Override
      public T call() throws IOException {
        return delegate.execute(target, request, responseHandler);
      }
    });
  }

  @Override
  public <T> T execute(final HttpHost target,
                       final HttpRequest request,
                       final ResponseHandler<? extends T> responseHandler,
                       final HttpContext context) throws IOException
  {
    return filter(target, new Filterable<T>()
    {
      @Override
      public T call() throws IOException {
        return delegate.execute(target, request, responseHandler, context);
      }
    });
  }

  private static HttpHost determineTarget(final HttpUriRequest request) throws ClientProtocolException {
    HttpHost target = null;
    final URI requestURI = request.getURI();
    if (requestURI.isAbsolute()) {
      target = URIUtils.extractHost(requestURI);
      if (target == null) {
        throw new ClientProtocolException("URI does not specify a valid host name: " + requestURI);
      }
    }
    return target;
  }

  @Override
  public void close() throws IOException {
    if (checkThread != null) {
      checkThread.interrupt();
    }
    if (delegate instanceof Closeable) {
      ((Closeable) delegate).close();
    }
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  private interface Filterable<T>
  {
    T call() throws IOException;
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
