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
package org.sonatype.nexus.testsuite.testsupport.conda;

import java.io.IOException;
import java.net.URI;

import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import static com.google.common.net.HttpHeaders.IF_MODIFIED_SINCE;

/**
 * @since 3.19
 * Conda Client
 */
public class CondaClient
    extends FormatClientSupport
{
  public CondaClient(final CloseableHttpClient httpClient,
                     final HttpClientContext httpClientContext,
                     final URI repositoryBaseUri)
  {
    super(httpClient, httpClientContext, repositoryBaseUri);
  }

  public HttpResponse fetch(final String path) throws IOException {
    return execute(new HttpGet(resolve(path)));
  }

  public HttpResponse fetchAndClose(final String path) throws IOException {
    return consume(fetch(path));
  }

  public HttpResponse conditionalFetch(final String path, final String modified) throws IOException {
    return conditionalGet(resolve(path), modified);
  }

  public int status(final String path) throws IOException {
    return status(consume(fetch(path)));
  }

  public HttpResponse search(final String criteria) throws IOException {
    return execute(new HttpGet(resolve(criteria)));
  }

  static HttpResponse consume(final HttpResponse response) throws IOException {
    EntityUtils.consume(response.getEntity());
    return response;
  }

  public HttpResponse conditionalGet(final URI uri, final String modified) throws IOException {
    HttpGet get = new HttpGet(uri);
    get.setHeader(IF_MODIFIED_SINCE, modified);
    return consume(execute(get));
  }
}
