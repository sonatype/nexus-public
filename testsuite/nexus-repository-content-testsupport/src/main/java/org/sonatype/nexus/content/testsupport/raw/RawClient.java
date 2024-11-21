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
package org.sonatype.nexus.content.testsupport.raw;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.sonatype.nexus.content.testsupport.FormatClientSupport;
import org.sonatype.nexus.repository.http.HttpMethods;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple test client for Raw repositories.
 */
public class RawClient
    extends FormatClientSupport
{
  public RawClient(
      final CloseableHttpClient httpClient,
      final HttpClientContext httpClientContext,
      final URI repositoryBaseUri)
  {
    super(httpClient, httpClientContext, repositoryBaseUri);
  }

  public int put(final String path, final ContentType contentType, final File file) throws Exception {
    checkNotNull(path);
    checkNotNull(file);

    HttpPut put = new HttpPut(repositoryBaseUri.resolve(path));
    put.setEntity(EntityBuilder.create().setContentType(contentType).setFile(file).build());

    return status(execute(put));
  }

  public CloseableHttpResponse put(final String path, final HttpEntity entity) throws IOException {
    final URI uri = resolve(path);
    final HttpPut put = new HttpPut(uri);
    put.setEntity(entity);
    return execute(put);
  }

  public byte[] getBytes(final String path) throws Exception {
    return bytes(get(path));
  }

  public CloseableHttpResponse delete(final String path) throws Exception {
    return execute(new HttpDelete(resolve(path)));
  }

  public CloseableHttpResponse mkcol(final String path) throws Exception {
    HttpUriRequest mkcolRequest = RequestBuilder.create(HttpMethods.MKCOL)
        .setUri(resolve(path))
        .build();
    return execute(mkcolRequest);
  }
}
