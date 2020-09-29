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
package org.sonatype.nexus.testsuite.testsupport.helm;

import java.io.IOException;
import java.net.URI;

import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * A simple test client for Helm repositories.
 *
 * @since 3.28
 */
public class HelmClient
    extends FormatClientSupport
{
  public HelmClient(final CloseableHttpClient httpClient,
                    final HttpClientContext httpClientContext,
                    final URI repositoryBaseUri)
  {
    super(httpClient, httpClientContext, repositoryBaseUri);
  }

  public HttpResponse fetch(final String path, String contentType) throws IOException {
    HttpGet request = new HttpGet(resolve(path));
    request.addHeader("Content-Type", contentType);
    return execute(request);
  }

  public HttpResponse put(String path, HttpEntity entity) throws IOException {
    final URI resolve = resolve(path);
    final HttpPut put = new HttpPut(resolve);
    put.setEntity(entity);
    return execute(put);
  }
}
