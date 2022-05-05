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
package org.sonatype.nexus.httpclient.internal;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.LOCATION;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.sonatype.nexus.httpclient.internal.NexusRedirectStrategy.CONTENT_RETRIEVAL_MARKER_KEY;

/**
 * Tests for {@link NexusRedirectStrategy}.
 */
public class NexusRedirectStrategyTest
    extends TestSupport
{
  @Mock
  private HttpResponse response;

  @Mock
  private StatusLine statusLine;

  private HttpGet request;

  @Test
  public void doNotFollowRedirectsToDirIndex() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);

    final RedirectStrategy underTest = new NexusRedirectStrategy();
    HttpContext httpContext;

    // no location header
    request = new HttpGet("http://localhost/dir/fileA");
    httpContext = new BasicHttpContext();
    httpContext.setAttribute(CONTENT_RETRIEVAL_MARKER_KEY, Boolean.TRUE);
    when(statusLine.getStatusCode()).thenReturn(SC_OK);
    assertThat(underTest.isRedirected(request, response, httpContext), is(false));

    // redirect to file
    request = new HttpGet("http://localhost/dir/fileA");
    httpContext = new BasicHttpContext();
    httpContext.setAttribute(CONTENT_RETRIEVAL_MARKER_KEY, Boolean.TRUE);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://localhost/dir/fileB"));
    assertThat(underTest.isRedirected(request, response, httpContext), is(true));

    // redirect to dir
    request = new HttpGet("http://localhost/dir");
    httpContext = new BasicHttpContext();
    httpContext.setAttribute(CONTENT_RETRIEVAL_MARKER_KEY, Boolean.TRUE);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://localhost/dir/"));
    assertThat(underTest.isRedirected(request, response, httpContext), is(false));
  }

  @Test
  public void doFollowCrossSiteRedirects() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);

    final RedirectStrategy underTest = new NexusRedirectStrategy();

    // simple cross redirect
    request = new HttpGet("http://hostA/dir");
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://hostB/dir"));
    assertThat(underTest.isRedirected(request, response, new BasicHttpContext()), is(true));

    // cross redirect to dir (failed coz NEXUS-5744)
    request = new HttpGet("http://hostA/dir/");
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://hostB/dir/"));
    assertThat(underTest.isRedirected(request, response, new BasicHttpContext()), is(true));
  }
}
