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
package org.sonatype.nexus.testsuite.testsupport.maven;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;

import com.google.common.net.HttpHeaders;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Maven2 Client.
 */
public class Maven2Client
    extends FormatClientSupport
{
  public Maven2Client(final CloseableHttpClient httpClient,
                      final HttpClientContext httpClientContext,
                      final URI repositoryBaseUri)
  {
    super(httpClient, httpClientContext, repositoryBaseUri);
  }

  public HttpResponse getIfNewer(String path, Date date) throws IOException {
    final URI uri = resolve(path);
    final HttpGet get = new HttpGet(uri);
    get.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateUtils.formatDate(date));
    return execute(get);
  }

  public HttpResponse getIfNoneMatch(String path, String etag) throws IOException {
    final URI uri = resolve(path);
    final HttpGet get = new HttpGet(uri);
    get.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
    return execute(get);
  }

  public HttpResponse put(String path, HttpEntity entity) throws IOException {
    final URI uri = resolve(path);
    final HttpPut put = new HttpPut(uri);
    put.setEntity(entity);
    return execute(put);
  }

  public HttpResponse putIfUmmodified(String path, Date date, HttpEntity entity) throws IOException {
    final URI uri = resolve(path);
    final HttpPut put = new HttpPut(uri);
    put.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, DateUtils.formatDate(date));
    put.setEntity(entity);
    return execute(put);
  }

  public HttpResponse putIfMatches(String path, String etag, HttpEntity entity) throws IOException {
    final URI uri = resolve(path);
    final HttpPut put = new HttpPut(uri);
    put.addHeader(HttpHeaders.IF_MATCH, etag);
    put.setEntity(entity);
    return execute(put);
  }

  public HttpResponse delete(String path) throws IOException {
    final URI uri = resolve(path);
    final HttpDelete delete = new HttpDelete(uri);
    return execute(delete);
  }
}
