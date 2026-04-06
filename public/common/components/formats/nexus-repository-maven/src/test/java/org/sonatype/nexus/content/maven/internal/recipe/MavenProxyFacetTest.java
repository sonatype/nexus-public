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

package org.sonatype.nexus.content.maven.internal.recipe;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.maven.MavenProxyRequestHeaderSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MavenProxyFacetTest
    extends TestSupport
{
  @Mock
  private ConstraintViolationFactory constraintViolationFactory;

  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private MavenProxyRequestHeaderSupport mavenProxyRequestHeaderSupport;

  private MavenProxyFacet underTest;

  @Before
  public void setUp() {
    this.underTest = new MavenProxyFacet(constraintViolationFactory, mavenProxyRequestHeaderSupport);
    when(applicationVersion.getEdition()).thenReturn("edition");
  }

  @Test
  public void testNonMavenCentralHostAndVerifyRequestHeader() throws URISyntaxException {
    URI uri = new URI("schema", "host", "/path/test", "fragment");
    Context context = mock(Context.class);

    HttpRequestBase request = underTest.buildFetchHttpRequest(uri, context);
    assertEquals(request.getAllHeaders().length, 0);
  }

  @Test
  public void testMavenCentralHostAndVerifyRequestHeader() throws URISyntaxException {
    URI uri = new URI("schema", "repo1.maven.org", "/path/test", "fragment");
    Context context = mock(Context.class);
    HttpRequestBase request = underTest.buildFetchHttpRequest(uri, context);
    assertEquals(request.getAllHeaders().length, 1);
  }

  /**
   * Verifies that if a response has OK status, and the response etag matches the content etag we consider it not
   * modified. This is the main test for the blob reuse optimization.
   */
  @Test
  public void testIsNotModified_OK_NOT_MODIFIED() {
    HttpResponse response = mockResponse(HttpStatus.SC_OK, "abc123");
    Content content = mockContent("abc123");
    assertTrue(underTest.isNotModified(response, content));
    verify(response, times(2)).getStatusLine();
    verify(response).containsHeader(HttpHeaders.ETAG);
    verify(response).getFirstHeader(HttpHeaders.ETAG);
    verify(content).getAttributes();
    verifyNoMoreInteractions(response, content);
  }

  /**
   * Verifies that if a response has OK status but there is no stale content, we return false.
   */
  @Test
  public void testIsNotModified_OK_noStale() {
    HttpResponse response = mockResponse(HttpStatus.SC_OK, "abc123");
    assertFalse(underTest.isNotModified(response, null));
    verify(response).getStatusLine();
    verifyNoMoreInteractions(response);
  }

  /**
   * Verifies that if a response has OK status, and the response etag does NOT match the content etag,
   * we consider it modified.
   */
  @Test
  public void testIsNotModified_OK_etagMismatch() {
    HttpResponse response = mockResponse(HttpStatus.SC_OK, "abc123");
    Content content = mockContent("xyz789");
    assertFalse(underTest.isNotModified(response, content));
    verify(response, times(2)).getStatusLine();
    verify(response).containsHeader(HttpHeaders.ETAG);
    verify(response).getFirstHeader(HttpHeaders.ETAG);
    verify(content).getAttributes();
    verifyNoMoreInteractions(response, content);
  }

  /**
   * Verifies that if a response has OK status but no ETag header, we return false.
   */
  @Test
  public void testIsNotModified_OK_noResponseEtag() {
    HttpResponse response = mockResponse(HttpStatus.SC_OK, null);
    Content content = mockContent("abc123");
    assertFalse(underTest.isNotModified(response, content));
    verify(response, times(2)).getStatusLine();
    verify(response).containsHeader(HttpHeaders.ETAG);
    verify(response).containsHeader("etag"); // Case-insensitive fallback check
    verifyNoMoreInteractions(response, content);
  }

  /**
   * Verifies that if a response has OK status but the content has no ETag stored, we return false.
   */
  @Test
  public void testIsNotModified_OK_noContentEtag() {
    HttpResponse response = mockResponse(HttpStatus.SC_OK, "abc123");
    Content content = mockContent(null);
    assertFalse(underTest.isNotModified(response, content));
    verify(response, times(2)).getStatusLine();
    verify(response).containsHeader(HttpHeaders.ETAG);
    verify(response).getFirstHeader(HttpHeaders.ETAG);
    verify(content).getAttributes();
    verifyNoMoreInteractions(response, content);
  }

  /**
   * Verifies that requests that are NOT_MODIFIED (304) returns true with minimal interactions.
   * This tests the parent class behavior.
   */
  @Test
  public void testIsNotModified_NOT_MODIFIED() {
    HttpResponse response = mockResponse(HttpStatus.SC_NOT_MODIFIED, null);
    assertTrue(underTest.isNotModified(response, null));
    verify(response).getStatusLine();
    verifyNoMoreInteractions(response);
  }

  /**
   * Verifies that requests that aren't OK or NOT_MODIFIED returns false.
   */
  @Test
  public void testIsNotModified_NOT_OK() {
    HttpResponse response = mockResponse(HttpStatus.SC_NOT_FOUND, null);
    Content content = mockContent(null);
    assertFalse(underTest.isNotModified(response, content));
    verify(response, times(2)).getStatusLine();
    verifyNoMoreInteractions(response);
  }

  private static Content mockContent(@Nullable final String etag) {
    Content content = mock(Content.class);
    AttributesMap map = new AttributesMap();
    lenient().when(content.getAttributes()).thenReturn(map);
    if (etag != null) {
      map.set(Content.CONTENT_ETAG, etag);
    }
    return content;
  }

  private static HttpResponse mockResponse(final int statusCode, @Nullable final String etag) {
    HttpResponse response = mock(HttpResponse.class);
    StatusLine status = mock(StatusLine.class);
    lenient().when(status.getStatusCode()).thenReturn(statusCode);
    lenient().when(response.getStatusLine()).thenReturn(status);

    if (etag != null) {
      lenient().when(response.containsHeader(HttpHeaders.ETAG)).thenReturn(true);
      lenient().when(response.getFirstHeader(HttpHeaders.ETAG)).thenReturn(new BasicHeader(HttpHeaders.ETAG, etag));
      lenient().when(response.getLastHeader(HttpHeaders.ETAG)).thenReturn(new BasicHeader(HttpHeaders.ETAG, etag));
    }

    return response;
  }
}
