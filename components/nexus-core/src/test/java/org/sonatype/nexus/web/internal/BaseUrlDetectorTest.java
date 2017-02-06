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
package org.sonatype.nexus.web.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(Parameterized.class)
public class BaseUrlDetectorTest
    extends TestSupport
{
  BaseUrlDetector underTest;

  @Mock
  GlobalRestApiSettings globalRestApiSettings;

  @Mock
  Provider<HttpServletRequest> httpServletRequestProvider;

  HttpServletRequest httpServletRequest;

  @Parameters
  public static List<Object[]> createData() {
    return Arrays.asList(new Object[][]{
        {
            "http://localhost:8081/nexus/content/groups/npm-all/@sonatype%2fnpm-test",
            "/content/groups/npm-all/@sonatype/npm-test",
            "/nexus/content/groups/npm-all/@sonatype%2fnpm-test",
            "/nexus",
            "http://localhost:8081/nexus",
            },
        {
            "http://localhost:8081/nexus/content/groups/npm-all/npm-test",
            "/content/groups/npm-all/npm-test",
            "/nexus/content/groups/npm-all/npm-test",
            "/nexus",
            "http://localhost:8081/nexus",
            },
        {
            "http://localhost:8081/nexus/",
            "/",
            "/nexus/",
            "/nexus",
            "http://localhost:8081/nexus",
            },
        {
            "http://localhost:8081/nexus/some/path",
            "/some/path",
            "/nexus/some/path",
            "/nexus",
            "http://localhost:8081/nexus",
            },
        });
  }

  String expectedBaseUrl;

  public BaseUrlDetectorTest(String url,
                             String servletPath,
                             String requestUri,
                             String contextPath,
                             String expectedBaseUrl)
  {
    httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer(url));
    when(httpServletRequest.getPathInfo()).thenReturn(null);
    when(httpServletRequest.getServletPath()).thenReturn(servletPath);
    when(httpServletRequest.getRequestURI()).thenReturn(requestUri);
    when(httpServletRequest.getContextPath()).thenReturn(contextPath);
    this.expectedBaseUrl = expectedBaseUrl;
  }

  @Before
  public void setup() {
    when(httpServletRequestProvider.get()).thenReturn(httpServletRequest);
    when(globalRestApiSettings.isEnabled()).thenReturn(false);
    underTest = new BaseUrlDetector(globalRestApiSettings, httpServletRequestProvider);
  }

  @Test
  public void run() throws Exception {
    assertThat(underTest.detect(), is(expectedBaseUrl));
  }
}
