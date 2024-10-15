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
package org.sonatype.nexus.repository.content.facet;

import java.io.IOException;
import java.net.URI;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;

import static com.google.common.base.Preconditions.checkState;

/**
 * Content {@link ProxyFacet} support.
 *
 * @since 3.25
 */
public abstract class ContentProxyFacetSupport
    extends ProxyFacetSupport
{
  @Override
  protected void indicateVerified(
      final Context context,
      final Content content,
      final CacheInfo cacheInfo) throws IOException
  {
    // refresh internal cache details to record that we know this asset is up-to-date
    Asset asset = content.getAttributes().get(Asset.class);
    if (asset != null) {
      facet(ContentFacet.class).assets().with(asset).markAsCached(cacheInfo);
    }
    else {
      log.debug("Proxied content has no attached asset; cannot refresh cache details");
    }
  }

  protected Payload getPayload(final Repository proxy, final URI uri) throws IOException {
    final HttpClient client = proxy.facet(HttpClientFacet.class).getHttpClient();

    HttpGet request = new HttpGet(uri);
    log.debug("Fetching: {}", request);

    HttpResponse response = client.execute(request);
    StatusLine status = response.getStatusLine();
    log.debug("Response: {}, status: {}", response, status);

    if (status.getStatusCode() == HttpStatus.SC_OK) {
      HttpEntity entity = response.getEntity();
      checkState(entity != null, "No http entity received from remote registry");

      return new HttpEntityPayload(response, entity);
    }
    log.warn("Status code {} contacting {}", status.getStatusCode(), uri);
    HttpClientUtils.closeQuietly(response);
    return null;
  }

}
