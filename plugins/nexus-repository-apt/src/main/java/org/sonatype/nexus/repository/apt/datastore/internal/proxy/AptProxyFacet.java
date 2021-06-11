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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotHandler;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.ContentSpecifier;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyServiceException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.debian.Utils.isDebPackageContentType;

/**
 * The implementation of a logic specific to the proxy repository
 *
 * @since 3.31
 */
@Named
@Facet.Exposed
public class AptProxyFacet
    extends ContentProxyFacetSupport
{
  public List<SnapshotItem> getSnapshotItems(final List<ContentSpecifier> specs) throws IOException {
    return fetchLatest(specs);
  }

  @Override
  protected Content getCachedContent(final Context context) {
    return facet(AptContentFacet.class).get(assetPath(context)).orElse(null);
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    if (assetPath(context).endsWith(RELEASE)) {
      // Whenever we fetch a new release file, make sure we get the signature
      // and package files that go along with it.
      cacheControllerHolder.getMetadataCacheController().invalidateCache();
    }
    return facet(AptContentFacet.class).put(assetPath(context), content).markAsCached(content).download();
  }

  @Override
  protected String getUrl(final Context context) {
    return assetPath(context);
  }

  @Override
  protected CacheController getCacheController(final Context context) {
    if (isDebPackageContentType(assetPath(context))) {
      return cacheControllerHolder.getContentCacheController();
    }
    return cacheControllerHolder.getMetadataCacheController();
  }

  private String assetPath(final Context context) {
    return context.getAttributes().require(AptSnapshotHandler.State.class).assetPath;
  }

  private List<SnapshotItem> fetchLatest(final List<ContentSpecifier> specs) throws IOException {
    List<SnapshotItem> list = new ArrayList<>();
    for (ContentSpecifier spec : specs) {
      Optional<SnapshotItem> item = fetchLatest(spec);
      if (item.isPresent()) {
        list.add(item.get());
      }
    }
    return list;
  }

  private Optional<SnapshotItem> fetchLatest(final ContentSpecifier spec) throws IOException {
    ProxyFacet proxyFacet = facet(ProxyFacet.class);
    HttpClientFacet httpClientFacet = facet(HttpClientFacet.class);
    HttpClient httpClient = httpClientFacet.getHttpClient();
    CacheController cacheController = cacheControllerHolder.getMetadataCacheController();
    CacheInfo cacheInfo = cacheController.current();
    Content oldVersion = facet(AptContentFacet.class).get(spec.path).orElse(null);

    URI fetchUri = proxyFacet.getRemoteUrl().resolve(spec.path);
    HttpGet getRequest = buildFetchRequest(oldVersion, fetchUri);

    HttpResponse response = httpClient.execute(getRequest);
    StatusLine status = response.getStatusLine();

    if (status.getStatusCode() == HttpStatus.SC_OK) {
      HttpEntity entity = response.getEntity();
      Content fetchedContent = new Content(new HttpEntityPayload(response, entity));
      AttributesMap contentAttrs = fetchedContent.getAttributes();
      contentAttrs.set(Content.CONTENT_LAST_MODIFIED, getDateHeader(response, HttpHeaders.LAST_MODIFIED));
      contentAttrs.set(Content.CONTENT_ETAG, getQuotedStringHeader(response, HttpHeaders.ETAG));
      contentAttrs.set(CacheInfo.class, cacheInfo);
      Content storedContent = facet(AptContentFacet.class).put(spec.path, fetchedContent).download();
      return Optional.of(new SnapshotItem(spec, storedContent));
    }

    try {
      if (status.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
        checkState(oldVersion != null, "Received 304 without conditional GET (bad server?) from %s", fetchUri);
        doIndicateVerified(oldVersion, cacheInfo, spec.path);
        return Optional.of(new SnapshotItem(spec, oldVersion));
      }
      throwProxyExceptionForStatus(response);
    }
    finally {
      HttpClientUtils.closeQuietly(response);
    }

    return Optional.empty();
  }

  private HttpGet buildFetchRequest(final Content oldVersion, final URI fetchUri) {
    HttpGet getRequest = new HttpGet(fetchUri);
    if (oldVersion != null) {
      DateTime lastModified = oldVersion.getAttributes().get(Content.CONTENT_LAST_MODIFIED, DateTime.class);
      if (lastModified != null) {
        getRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateUtils.formatDate(lastModified.toDate()));
      }
      final String etag = oldVersion.getAttributes().get(Content.CONTENT_ETAG, String.class);
      if (etag != null) {
        getRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "\"" + etag + "\"");
      }
    }
    return getRequest;
  }

  private DateTime getDateHeader(final HttpResponse response, final String name) {
    Header h = response.getLastHeader(name);
    if (h != null) {
      try {
        return new DateTime(DateUtils.parseDate(h.getValue()).getTime());
      }
      catch (Exception ex) {
        log.warn("Invalid date '{}', will skip. {}", h, log.isDebugEnabled() ? ex : null);
      }
    }
    return null;
  }

  private String getQuotedStringHeader(final HttpResponse response, final String name) {
    Header h = response.getLastHeader(name);
    if (h != null) {
      String value = h.getValue();
      if (!Strings.isNullOrEmpty(value)) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
          return value.substring(1, value.length() - 1);
        }
        else {
          return value;
        }
      }
    }
    return null;
  }

  protected void doIndicateVerified(final Content content, final CacheInfo cacheInfo, final String assetPath) {
    AptContentFacet contentFacet = facet(AptContentFacet.class);

    Asset asset = content.getAttributes().get(Asset.class);
    if (asset != null) {
      contentFacet.assets().with(asset).markAsCached(cacheInfo);
      return;
    }
    facet(AptContentFacet.class).getAsset(assetPath)
        .ifPresent(a -> contentFacet.assets().with(a).markAsCached(cacheInfo));
  }

  private void throwProxyExceptionForStatus(final HttpResponse httpResponse) {
    final StatusLine status = httpResponse.getStatusLine();
    if (HttpStatus.SC_UNAUTHORIZED == status.getStatusCode()
        || HttpStatus.SC_PAYMENT_REQUIRED == status.getStatusCode()
        || HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED == status.getStatusCode()
        || HttpStatus.SC_INTERNAL_SERVER_ERROR <= status.getStatusCode()) {
      throw new ProxyServiceException(httpResponse);
    }
  }
}
