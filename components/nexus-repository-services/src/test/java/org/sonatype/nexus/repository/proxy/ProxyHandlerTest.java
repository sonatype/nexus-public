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
package org.sonatype.nexus.repository.proxy;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.io.CooperationException;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.httpclient.RemoteBlockedIOException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.PROXY_REMOTE_FETCH_SKIP_MARKER;

public class ProxyHandlerTest
    extends TestSupport
{
  @Mock
  private HttpResponse httpResponse;

  @Mock
  private StatusLine statusLine;

  @Mock
  private Context context;

  @Mock
  private Repository repository;

  @Mock
  private ProxyFacet proxyFacet;

  @Mock
  private Content content;

  @Mock
  private Request request;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private Format format;

  @InjectMocks
  private final ProxyHandler underTest = new ProxyHandler();

  @Before
  public void setUp() {
    when(context.getRequest()).thenReturn(request);
    when(context.getRepository()).thenReturn(repository);
    when(repository.facet(ProxyFacet.class)).thenReturn(proxyFacet);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.toString()).thenReturn("status line");
    when(nodeAccess.getId()).thenReturn("123-456-789");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("npm");
  }

  @Test
  public void testMethodNotAllowedReturns405Response() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.LOCK);
    assertStatusCode(underTest.handle(context), HttpStatus.METHOD_NOT_ALLOWED);
  }

  @Test
  public void testPayloadPresentReturns200Response() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);
    when(proxyFacet.get(context)).thenReturn(content);
    assertStatusCode(underTest.handle(context), HttpStatus.OK);
  }

  @Test
  public void testPayloaAbsentReturns404Response() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);
    assertStatusCode(underTest.handle(context), HttpStatus.NOT_FOUND);
  }

  @Test
  public void testPayloaAbsentSkipMarkerTrueReturns402Response() throws Exception {
    AttributesMap attributes = new AttributesMap();
    attributes.set(PROXY_REMOTE_FETCH_SKIP_MARKER, TRUE);
    when(request.getAction()).thenReturn(HttpMethods.GET);
    when(context.getAttributes()).thenReturn(attributes);
    assertStatusCode(underTest.handle(context), HttpStatus.FORBIDDEN);
  }

  @Test
  public void testProxyServiceExceptionReturns503Response() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);
    doThrow(new ProxyServiceException(httpResponse)).when(proxyFacet).get(context);
    assertStatusCode(underTest.handle(context), HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  public void testCooperationExceptionReturns503ResponseWithMessage() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);
    doThrow(new CooperationException("Cooperation failed")).when(proxyFacet).get(context);
    Response response = underTest.handle(context);
    assertStatusCode(response, HttpStatus.SERVICE_UNAVAILABLE);
    assertStatusMessage(response, "Cooperation failed");
  }

  @Test
  public void testIOExceptionReturns404Response() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);
    doThrow(new RemoteBlockedIOException("message")).when(proxyFacet).get(context);
    assertStatusCode(underTest.handle(context), HttpStatus.NOT_FOUND);
  }

  @Test
  public void testIOExceptionReturns502Response() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);
    doThrow(new IOException("message")).when(proxyFacet).get(context);
    assertStatusCode(underTest.handle(context), HttpStatus.BAD_GATEWAY);
  }

  @Test
  public void testUncheckedIOExceptionReturns502Response() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.GET);
    doThrow(new UncheckedIOException(new IOException("message"))).when(proxyFacet).get(context);
    assertStatusCode(underTest.handle(context), HttpStatus.BAD_GATEWAY);
  }

  @Test
  public void testBypassHttpErrorExceptionPropagatesCodeWithMessageWithHeader() throws Exception {
    final int httpStatus = HttpStatus.FORBIDDEN;
    final String errorMessage = "Error Message";
    final String headerName = "Header Name";
    final String headerValue = "Header Value";

    ListMultimap<String, String> headers = ArrayListMultimap.create();
    headers.put(headerName, headerValue);
    BypassHttpErrorException bypassException = new BypassHttpErrorException(httpStatus, errorMessage, headers);

    when(request.getAction()).thenReturn(HttpMethods.GET);
    doThrow(bypassException).when(proxyFacet).get(context);

    Response response = underTest.handle(context);

    assertStatusCode(response, httpStatus);
    assertStatusMessage(response, errorMessage);
    assertThat(response.getHeaders().get(headerName), is(headerValue));
  }

  private void assertStatusCode(final Response response, final int statusCode) {
    assertThat(response.getStatus().getCode(), is(statusCode));
  }

  private void assertStatusMessage(final Response response, final String statusMessage) {
    assertThat(response.getStatus().getMessage(), is(statusMessage));
  }
}
