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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.repository.BadRequestException;
import org.sonatype.nexus.repository.httpbridge.HttpResponseSender;
import org.sonatype.nexus.repository.httpbridge.internal.describe.Description;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionHelper;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionRenderer;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;

import com.google.common.net.HttpHeaders;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for describe functionality of {@link ViewServlet}.
 */
public class ViewServletTest
    extends TestSupport
{
  @Mock
  private Request request;

  @Mock
  private ViewFacet facet;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private HttpServletResponse servletResponse;

  @Mock
  private DescriptionRenderer descriptionRenderer;

  @Mock(name = "facet-response", answer = RETURNS_DEEP_STUBS)
  private Response facetResponse;

  @Mock(name = "facet-exception", answer = RETURNS_DEEP_STUBS)
  private RuntimeException facetException;

  @Mock
  private HttpServletRequest httpServletRequest;

  private Parameters parameters;

  private DefaultHttpResponseSender defaultResponseSender;

  private ViewServlet underTest;

  @Before
  public void setUp() throws Exception {
    defaultResponseSender = spy(new DefaultHttpResponseSender());

    when(descriptionRenderer.renderHtml(any(Description.class))).thenReturn("HTML");
    when(descriptionRenderer.renderJson(any(Description.class))).thenReturn("JSON");

    underTest = spy(new ViewServlet(mock(RepositoryManager.class),
        new HttpResponseSenderSelector(Collections.<String, HttpResponseSender>emptyMap(), defaultResponseSender),
        mock(DescriptionHelper.class),
        descriptionRenderer, true
    ));

    when(request.getPath()).thenReturn("/test");

    parameters = new Parameters();
    when(request.getParameters()).thenReturn(parameters);

    BaseUrlHolder.set("http://placebo");
  }

  private void descriptionRequested(final String describe) {
    if (describe == null) {
      parameters.remove(ViewServlet.P_DESCRIBE);
    }
    else {
      parameters.set(ViewServlet.P_DESCRIBE, describe);
    }
  }

  @Test
  @Ignore("disabled, due to unknown heap issue with defaultResponseSender spy")
  public void normalRequestReturnsFacetResponse() throws Exception {
    descriptionRequested(null);
    facetThrowsException(false);

    underTest.dispatchAndSend(request, facet, defaultResponseSender, servletResponse);

    verify(underTest, never()).describe(
        any(Request.class),
        any(Response.class),
        any(Exception.class),
        any(String.class)
    );
    verify(defaultResponseSender).send(eq(request), any(Response.class), eq(servletResponse));
  }

  @Test
  public void describeRequestReturnsDescriptionResponse_HTML() throws Exception {
    descriptionRequested("HTML");
    facetThrowsException(false);

    underTest.dispatchAndSend(request, facet, defaultResponseSender, servletResponse);

    verify(underTest).describe(request, facetResponse, null, "HTML");
    verify(underTest).send(eq(request), any(Response.class), eq(servletResponse));
    verify(servletResponse).setContentType(ContentTypes.TEXT_HTML);
  }

  @Test
  public void describeRequestReturnsDescriptionResponse_JSON() throws Exception {
    descriptionRequested("JSON");
    facetThrowsException(false);

    underTest.dispatchAndSend(request, facet, defaultResponseSender, servletResponse);

    verify(underTest).describe(request, facetResponse, null, "JSON");
    verify(underTest).send(eq(request), any(Response.class), eq(servletResponse));
    verify(servletResponse).setContentType(ContentTypes.APPLICATION_JSON);
  }

  @Test(expected = RuntimeException.class)
  public void facetExceptionsReturnedNormally() throws Exception {
    descriptionRequested(null);
    facetThrowsException(true);

    underTest.dispatchAndSend(request, facet, defaultResponseSender, servletResponse);
  }

  @Test
  public void facetExceptionsAreDescribed() throws Exception {
    descriptionRequested("HTML");
    facetThrowsException(true);

    underTest.dispatchAndSend(request, facet, defaultResponseSender, servletResponse);

    // The exception got described
    verify(underTest).describe(request, null, facetException, "HTML");
    verify(underTest).send(eq(request), any(Response.class), eq(servletResponse));
  }

  @Test
  public void return400BadRequestOnBadRequestException() throws Exception {
    String message = "message";
    when(httpServletRequest.getPathInfo()).thenThrow(new BadRequestException(message));
    underTest.service(httpServletRequest, servletResponse);
    verify(servletResponse).sendError(SC_BAD_REQUEST, message);
  }

  @Test
  public void responseHasContentSecurityPolicy() throws Exception {
    underTest.service(httpServletRequest, servletResponse);

    verify(servletResponse).setHeader(HttpHeaders.CONTENT_SECURITY_POLICY,
        "sandbox allow-forms allow-modals allow-popups allow-presentation allow-scripts allow-top-navigation");
  }

  @Test
  public void responseHasXssProtection() throws Exception {
    underTest.service(httpServletRequest, servletResponse);

    verify(servletResponse).setHeader(HttpHeaders.X_XSS_PROTECTION, "1; mode=block");
  }

  private void facetThrowsException(final boolean facetThrowsException) throws Exception {
    if (facetThrowsException) {
      when(facet.dispatch(request)).thenThrow(facetException);
    }
    else {
      when(facet.dispatch(request)).thenReturn(facetResponse);
    }
  }
}