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
package org.sonatype.nexus.testsuite.testsupport.golang;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class GolangClient
    extends FormatClientSupport
{
  public GolangClient(final CloseableHttpClient httpClient,
                      final HttpClientContext httpClientContext,
                      final URI repositoryBaseUri)
  {
    super(httpClient, httpClientContext, repositoryBaseUri);
  }

  public HttpResponse fetch(final String path) throws IOException {
    final URI uri = resolve(path);
    return execute(new HttpGet(uri));
  }

  public HttpResponse fetchAndClose(final String path) throws Exception {
    return consume(fetch(path));
  }

  public HttpResponse deploy(final String packageName) throws IOException {
    final URI uri = resolve(packageName);
    return execute(new HttpPut(uri));
  }

  public HttpResponse put(final String path, final File file) throws Exception {
    checkNotNull(path);
    checkNotNull(file);

    HttpPut put = new HttpPut(repositoryBaseUri.resolve(path));
    put.setEntity(new FileEntity(file, ContentType.create("application/octet-stream")));

    return execute(put);
  }

  static HttpResponse consume(final HttpResponse response) throws IOException {
    EntityUtils.consume(response.getEntity());
    return response;
  }
}
