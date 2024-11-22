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
package org.sonatype.nexus.internal.app;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BaseUrlManagerImpl}
 */
public class BaseUrlManagerImplTest
    extends TestSupport
{
  static final String NUGET_QUERY = "/repository/nuget.org-proxy/Packages(Id='jQuery',Version='2.1.4')";

  static final String ENCODED_NUGET_QUERY = "/repository/nuget.org-proxy/Packages(Id=%27jQuery%27,Version=%272.1.4%27)";

  static final String FAKE_URL = "http://example.com:1234/foo/bar";

  @Mock
  Provider<HttpServletRequest> requestProvider;

  @Mock
  HttpServletRequest request;

  BaseUrlManagerImpl underTest;

  @Before
  public void setUp() {
    underTest = new BaseUrlManagerImpl(requestProvider, false);
  }

  @Test
  public void forceUrl() {
    underTest.setForce(true);
    underTest.setUrl(FAKE_URL);
    assertThat(underTest.detectUrl(), equalTo(FAKE_URL));
  }

  @Test
  public void noRequest() {
    underTest.setUrl(FAKE_URL);

    when(requestProvider.get()).thenReturn(null);

    assertThat(underTest.detectUrl(), equalTo(FAKE_URL));
  }

  @Test
  public void noBaseUrl() {
    when(requestProvider.get()).thenReturn(null);

    assertThat(underTest.detectUrl(), nullValue());
  }

  @Test
  public void defaultContext() {
    testNuGetQuery("http://localhost:8081", "");
  }

  @Test
  public void nexusContext() {
    testNuGetQuery("http://localhost:8081", "/nexus");
  }

  @Test
  public void nexusFooBarBazContext() {
    testNuGetQuery("http://localhost:8081", "/nexus/foo/bar/baz");
  }

  void testNuGetQuery(String protocolHostPort, String context) {
    String expectedBaseUrl = protocolHostPort + context;

    when(requestProvider.get()).thenReturn(request);
    when(request.getRequestURL()).thenReturn(new StringBuffer(expectedBaseUrl + ENCODED_NUGET_QUERY));
    when(request.getContextPath()).thenReturn(context);
    when(request.getRequestURI()).thenReturn(context + ENCODED_NUGET_QUERY);
    when(request.getServletPath()).thenReturn(NUGET_QUERY);

    assertThat(underTest.detectUrl(), equalTo(expectedBaseUrl));
  }

  @Test
  public void testDetectRelativePath() {
    testRelativePath(".", "", "/");
    testRelativePath("..", "", "/foo/");
    testRelativePath("..", "", "/foo/bar");
    testRelativePath("../..", "", "/foo/bar/");
    testRelativePath("../..", "", "/foo//bar/");
  }

  @Test
  public void testDetectRelativePath_nonRoot() {
    testRelativePath(".", "/nexus", "/");
    testRelativePath("..", "/nexus", "/foo/");
    testRelativePath("..", "/nexus", "/foo/bar");
    testRelativePath("../..", "/nexus", "/foo/bar/");
    testRelativePath("../..", "/nexus", "/foo//bar/");
  }

  @Test
  public void testDetectRelativePath_errorDispatch() {
    when(request.getDispatcherType()).thenReturn(DispatcherType.ERROR);
    when(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).thenReturn("/nexus/foo/bar/");
    testRelativePath("../..", "/nexus", "/");
  }

  @Test
  public void testDetectRelativePath_forwardDispatch() {
    when(request.getDispatcherType()).thenReturn(DispatcherType.FORWARD);
    when(request.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH)).thenReturn("/nexus2");
    when(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)).thenReturn("/nexus2/foo/bar/");
    testRelativePath("../..", "/nexus", "/");
  }

  @Test
  public void testDetectRelativePath_fallback() {
    when(request.getDispatcherType()).thenReturn(DispatcherType.FORWARD);
    testRelativePath(".", "/nexus", "/");
  }

  private void testRelativePath(final String expectedPath, final String context, final String requestPath) {
    when(requestProvider.get()).thenReturn(request);
    when(request.getContextPath()).thenReturn(context);
    when(request.getRequestURI()).thenReturn(context + requestPath);

    assertThat(underTest.detectRelativePath(), equalTo(expectedPath));
  }
}
