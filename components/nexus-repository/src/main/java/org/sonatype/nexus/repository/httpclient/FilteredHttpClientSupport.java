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

import org.sonatype.goodies.common.ComponentSupport;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for wrapping {@link HttpClient}s.
 *
 * @since 3.1
 */
public abstract class FilteredHttpClientSupport
    extends ComponentSupport
    implements HttpClient, Closeable
{
  private final HttpClient delegate;

  public FilteredHttpClientSupport(final HttpClient delegate) {
    this.delegate = checkNotNull(delegate);
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
  public void close() throws IOException {
    if (delegate instanceof Closeable) {
      ((Closeable) delegate).close();
    }
  }

  protected abstract <T> T filter(final HttpHost target, final Filterable<T> filterable) throws IOException;

  @Override
  public HttpResponse execute(final HttpUriRequest request) throws IOException {
    return filter(determineTarget(request), () -> delegate.execute(request));
  }

  @Override
  public HttpResponse execute(final HttpUriRequest request,
                              final HttpContext context)
      throws IOException
  {
    return filter(determineTarget(request), () -> delegate.execute(request, context));
  }

  @Override
  public HttpResponse execute(final HttpHost target,
                              final HttpRequest request)
      throws IOException
  {
    return filter(target, () -> delegate.execute(target, request));
  }

  @Override
  public HttpResponse execute(final HttpHost target,
                              final HttpRequest request,
                              final HttpContext context)
      throws IOException
  {
    return filter(target, () -> delegate.execute(target, request, context));
  }

  @Override
  public <T> T execute(final HttpUriRequest request,
                       final ResponseHandler<? extends T> responseHandler)
      throws IOException
  {
    return filter(determineTarget(request), () -> delegate.execute(request, responseHandler));
  }

  @Override
  public <T> T execute(final HttpUriRequest request,
                       final ResponseHandler<? extends T> responseHandler,
                       final HttpContext context)
      throws IOException
  {
    return filter(determineTarget(request), () -> delegate.execute(request, responseHandler, context));
  }

  @Override
  public <T> T execute(final HttpHost target,
                       final HttpRequest request,
                       final ResponseHandler<? extends T> responseHandler)
      throws IOException
  {
    return filter(target, () -> delegate.execute(target, request, responseHandler));
  }

  @Override
  public <T> T execute(final HttpHost target,
                       final HttpRequest request,
                       final ResponseHandler<? extends T> responseHandler,
                       final HttpContext context) throws IOException
  {
    return filter(target, () -> delegate.execute(target, request, responseHandler, context));
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

  protected interface Filterable<T>
  {
    T call() throws IOException;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "delegate=" + delegate +
        '}';
  }
}
