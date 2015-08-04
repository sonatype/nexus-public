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
package org.sonatype.nexus.repositories.metadata;

import java.io.IOException;

import org.sonatype.nexus.repository.metadata.RawTransport;

import com.google.common.base.Preconditions;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

/**
 * RawTransport implementation using HC4, and hence, obeying global proxy settings.
 *
 * @since 2.3
 */
public class Hc4RawTransport
    implements RawTransport
{

  private final HttpClient httpClient;

  private final String baseUrl;

  public Hc4RawTransport(final HttpClient httpClient, final String baseUrl) {
    this.httpClient = Preconditions.checkNotNull(httpClient);
    this.baseUrl = Preconditions.checkNotNull(baseUrl);
  }

  @Override
  public byte[] readRawData(final String path)
      throws IOException
  {
    final HttpGet get = new HttpGet(createUrlWithPath(path));
    get.setHeader("Accept", ContentType.APPLICATION_XML.getMimeType());
    final HttpResponse response = httpClient.execute(get);
    try {
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 200 && response.getEntity() != null) {
        return EntityUtils.toByteArray(response.getEntity());
      }
      else if (statusCode == 404) {
        return null;
      }
      else {
        throw new IOException("The response was not successful: " + response.getStatusLine());
      }
    }
    finally {
      EntityUtils.consume(response.getEntity());
    }
  }

  @Override
  public void writeRawData(final String path, final byte[] bytes)
      throws IOException
  {
    final HttpPut put = new HttpPut(createUrlWithPath(path));
    put.setEntity(new ByteArrayEntity(bytes, ContentType.APPLICATION_XML));
    final HttpResponse response = httpClient.execute(put);
    try {
      if (response.getStatusLine().getStatusCode() > 299) {
        throw new IOException("The response was not successful: " + response.getStatusLine());
      }
    }
    finally {
      EntityUtils.consume(response.getEntity());
    }
  }

  // ==

  protected String createUrlWithPath(String path) {
    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + path;
  }
}
