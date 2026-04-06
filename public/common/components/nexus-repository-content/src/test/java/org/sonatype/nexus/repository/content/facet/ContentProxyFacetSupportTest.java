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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
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
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ContentProxyFacetSupport}
 */
public class ContentProxyFacetSupportTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private FluentAsset fluentAsset;

  @Mock
  private Context context;

  @Mock
  private CacheInfo cacheInfo;

  @Mock
  private HttpClientFacet httpClientFacet;

  @Mock
  private HttpClient httpClient;

  @Mock
  private Format format;

  @Mock
  private Type type;

  private ContentProxyFacetSupport underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ContentProxyFacetSupport()
    {
      @Override
      protected Content getCachedContent(final Context context) {
        return null;
      }

      @Override
      protected Content store(final Context context, final Content content) {
        return content;
      }

      @Override
      protected String getUrl(final Context context) {
        return "/test/path";
      }
    };

    underTest.attach(repository);
  }

  // --- indicateVerified tests ---

  @Test
  public void indicateVerified_withAssetInAttributes_marksCached() throws IOException {
    Content content = mock(Content.class);
    AttributesMap attributes = new AttributesMap();
    Asset asset = mock(Asset.class);
    attributes.set(Asset.class, asset);
    when(content.getAttributes()).thenReturn(attributes);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.with(asset)).thenReturn(fluentAsset);

    underTest.indicateVerified(context, content, cacheInfo);

    verify(fluentAsset).markAsCached(cacheInfo);
  }

  @Test
  public void indicateVerified_withoutAssetInAttributes_doesNotThrow() throws IOException {
    Content content = mock(Content.class);
    AttributesMap attributes = new AttributesMap();
    when(content.getAttributes()).thenReturn(attributes);

    // Should not throw and should not interact with content facet
    underTest.indicateVerified(context, content, cacheInfo);

    verify(repository, never()).facet(ContentFacet.class);
  }

  @Test
  public void indicateVerified_withNullAsset_logsDebugAndSkips() throws IOException {
    Content content = mock(Content.class);
    AttributesMap attributes = new AttributesMap();
    // Asset.class key exists but is null
    attributes.set(Asset.class, null);
    when(content.getAttributes()).thenReturn(attributes);

    underTest.indicateVerified(context, content, cacheInfo);

    verify(repository, never()).facet(ContentFacet.class);
  }

  // --- getPayload tests ---

  @Test
  public void getPayload_withOkResponse_returnsHttpEntityPayload() throws IOException {
    URI uri = URI.create("http://remote.example.com/artifact.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    HttpEntity entity = mock(HttpEntity.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    when(response.getEntity()).thenReturn(entity);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    Payload result = underTest.getPayload(repository, uri);

    assertThat(result, is(instanceOf(HttpEntityPayload.class)));
  }

  @Test
  public void getPayload_withNonOkResponse_returnsNull() throws IOException {
    URI uri = URI.create("http://remote.example.com/missing.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    try (MockedStatic<HttpClientUtils> utils = mockStatic(HttpClientUtils.class)) {
      Payload result = underTest.getPayload(repository, uri);

      assertThat(result, is(nullValue()));
      utils.verify(() -> HttpClientUtils.closeQuietly(response));
    }
  }

  @Test
  public void getPayload_withServerError_returnsNullAndClosesResponse() throws IOException {
    URI uri = URI.create("http://remote.example.com/error");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("npm");
    when(type.getValue()).thenReturn("proxy");

    try (MockedStatic<HttpClientUtils> utils = mockStatic(HttpClientUtils.class)) {
      Payload result = underTest.getPayload(repository, uri);

      assertThat(result, is(nullValue()));
      utils.verify(() -> HttpClientUtils.closeQuietly(response));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void getPayload_withOkResponseButNullEntity_throwsIllegalState() throws IOException {
    URI uri = URI.create("http://remote.example.com/no-entity");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    when(response.getEntity()).thenReturn(null);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    underTest.getPayload(repository, uri);
  }

  @Test
  public void getPayload_withNullFormat_doesNotPopulateContext() throws IOException {
    URI uri = URI.create("http://remote.example.com/artifact.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    HttpEntity entity = mock(HttpEntity.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    when(response.getEntity()).thenReturn(entity);
    when(repository.getFormat()).thenReturn(null);
    when(repository.getType()).thenReturn(type);

    Payload result = underTest.getPayload(repository, uri);

    assertThat(result, is(instanceOf(HttpEntityPayload.class)));
  }

  @Test
  public void getPayload_withNullType_doesNotPopulateContext() throws IOException {
    URI uri = URI.create("http://remote.example.com/artifact.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    HttpEntity entity = mock(HttpEntity.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    when(response.getEntity()).thenReturn(entity);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(null);

    Payload result = underTest.getPayload(repository, uri);

    assertThat(result, is(instanceOf(HttpEntityPayload.class)));
  }

  @Test
  public void getPayload_withUnauthorized_returnsNull() throws IOException {
    URI uri = URI.create("http://remote.example.com/private.jar");
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    try (MockedStatic<HttpClientUtils> utils = mockStatic(HttpClientUtils.class)) {
      Payload result = underTest.getPayload(repository, uri);

      assertThat(result, is(nullValue()));
      utils.verify(() -> HttpClientUtils.closeQuietly(response));
    }
  }

  @Test(expected = IOException.class)
  public void getPayload_withClientExecuteException_propagatesIOException() throws IOException {
    URI uri = URI.create("http://remote.example.com/fail");

    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpGet.class), any(HttpContext.class)))
        .thenThrow(new IOException("Connection refused"));
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(format.getValue()).thenReturn("maven2");
    when(type.getValue()).thenReturn("proxy");

    underTest.getPayload(repository, uri);
  }
}
