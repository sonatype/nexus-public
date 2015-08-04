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
package org.sonatype.nexus.plugins.lvo.strategy;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.plugins.lvo.DiscoveryRequest;

import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

public abstract class AbstractRemoteDiscoveryStrategy
    extends AbstractDiscoveryStrategy
{
  private final Hc4Provider hc4Provider;

  public AbstractRemoteDiscoveryStrategy(final Hc4Provider hc4Provider) {
    this.hc4Provider = Preconditions.checkNotNull(hc4Provider);
  }

  protected RequestResult handleRequest(String url) {
    final HttpClient client = hc4Provider.createHttpClient();
    final HttpGet method = new HttpGet(url);

    RequestResult result = null;
    try {
      final HttpResponse response = client.execute(method);
      if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
        result = new RequestResult(method, response);
      }
    }
    catch (IOException e) {
      log.debug("Error retrieving lvo data", e);
    }

    return result;
  }

  protected String getRemoteUrl(DiscoveryRequest request) {
    return request.getLvoKey().getRemoteUrl();
  }

  protected static final class RequestResult
  {
    private final HttpGet method;

    private final InputStream is;

    public RequestResult(HttpGet method, HttpResponse response)
        throws IOException
    {
      this.method = Preconditions.checkNotNull(method);
      this.is = response.getEntity().getContent();
    }

    public InputStream getInputStream() {
      return is;
    }

    public void close() {
      IOUtils.closeQuietly(is);
      method.releaseConnection();
    }
  }
}
