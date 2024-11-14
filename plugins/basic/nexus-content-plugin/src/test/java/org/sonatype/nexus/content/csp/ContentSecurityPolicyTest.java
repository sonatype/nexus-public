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
package org.sonatype.nexus.content.csp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContentSecurityPolicyTest
    extends TestSupport
{
  private static final String CSP_HEADER = "Content-Security-Policy";

  private static final String XCSP_HEADER = "X-Content-Security-Policy";

  private static final String
      SANDBOX =
      "sandbox allow-forms allow-modals allow-popups allow-presentation allow-scripts allow-top-navigation";

  @Mock
  HttpServletRequest httpServletRequest;

  @Mock
  HttpServletResponse httpServletResponse;

  private DefaultContentSecurityPolicyExclusion
      defaultContentSecurityPolicyExclusion =
      new DefaultContentSecurityPolicyExclusion(null);

  private ContentSecurityPolicy underTest;

  @Before
  public void setup() {
    underTest = new ContentSecurityPolicy(singletonList(defaultContentSecurityPolicyExclusion), true);
  }

  @Test
  public void testDefaultState() {
    when(httpServletRequest.getRequestURI()).thenReturn("/nexus/service/local/repositories");
    underTest.apply(httpServletRequest, httpServletResponse);
    verify(httpServletResponse).setHeader(
        CSP_HEADER,
        SANDBOX);
    verify(httpServletResponse).setHeader(
        XCSP_HEADER,
        SANDBOX);
  }

  @Test
  public void testDefaultWithProperty() {
    defaultContentSecurityPolicyExclusion = new DefaultContentSecurityPolicyExclusion("/service/local/repositories/");
    underTest = new ContentSecurityPolicy(singletonList(defaultContentSecurityPolicyExclusion), true);
    when(httpServletRequest.getRequestURI()).thenReturn("/nexus/service/local/repositories/something");
    underTest.apply(httpServletRequest, httpServletResponse);
    verify(httpServletResponse, never()).setHeader(
        CSP_HEADER,
        SANDBOX);
    verify(httpServletResponse, never()).setHeader(
        XCSP_HEADER,
        SANDBOX);
  }

  @Test
  public void testDefaultWithProperty_noContextPath() {
    defaultContentSecurityPolicyExclusion = new DefaultContentSecurityPolicyExclusion("/service/local/repositories/");
    underTest = new ContentSecurityPolicy(singletonList(defaultContentSecurityPolicyExclusion), true);
    when(httpServletRequest.getRequestURI()).thenReturn("/service/local/repositories/something");
    underTest.apply(httpServletRequest, httpServletResponse);
    verify(httpServletResponse, never()).setHeader(
        CSP_HEADER,
        SANDBOX);
    verify(httpServletResponse, never()).setHeader(
        XCSP_HEADER,
        SANDBOX);
  }

  @Test
  public void testMultipleContributors() {
    defaultContentSecurityPolicyExclusion =
        new DefaultContentSecurityPolicyExclusion("/service/local/repositories/,/service/local/groups");
    ContentSecurityPolicyExclusion exclusion = () -> singletonList("/service/local/some/path");
    underTest = new ContentSecurityPolicy(asList(defaultContentSecurityPolicyExclusion, exclusion), true);

    when(httpServletRequest.getRequestURI()).thenReturn("/nexus/service/local/repositories/something");
    underTest.apply(httpServletRequest, httpServletResponse);
    when(httpServletRequest.getRequestURI()).thenReturn("/nexus/service/local/groups/something");
    underTest.apply(httpServletRequest, httpServletResponse);
    when(httpServletRequest.getRequestURI()).thenReturn("/nexus/service/local/some/path/something");

    verify(httpServletResponse, never()).setHeader(
        CSP_HEADER,
        SANDBOX);
    verify(httpServletResponse, never()).setHeader(
        XCSP_HEADER,
        SANDBOX);
  }
}
