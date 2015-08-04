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

import javax.servlet.http.HttpServletRequest;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BrowserDetector}.
 */
public class BrowserDetectorTest
  extends TestSupport
{
  private BrowserDetector underTest;

  @Mock
  private HttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    underTest = new BrowserDetector(false, null);
  }

  private void whenUserAgent(final String userAgent) {
    when(request.getHeader(BrowserDetector.USER_AGENT)).thenReturn(userAgent);
  }

  @Test
  public void chrome() {
    whenUserAgent("User-Agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36");
    assertThat(underTest.isBrowserInitiated(request), is(true));
  }

  @Test
  public void chrome_whenDisabled() {
    underTest = new BrowserDetector(true, null);
    whenUserAgent("User-Agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36");
    assertThat(underTest.isBrowserInitiated(request), is(false));
  }

  @Test
  public void firefox() {
    whenUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:25.0) Gecko/20100101 Firefox/25.0");
    assertThat(underTest.isBrowserInitiated(request), is(true));
  }

  @Test
  public void firefox_whenExcluded() {
    underTest = new BrowserDetector(false, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:25.0) Gecko/20100101 Firefox/25.0");
    whenUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:25.0) Gecko/20100101 Firefox/25.0");
    assertThat(underTest.isBrowserInitiated(request), is(false));
  }

  @Test
  public void firefox_whenMultipleExcludedWithExtraWhitespace() {
    underTest = new BrowserDetector(false, "foo\n Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:25.0) Gecko/20100101 Firefox/25.0 \nbar");
    whenUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:25.0) Gecko/20100101 Firefox/25.0");
    assertThat(underTest.isBrowserInitiated(request), is(false));
  }

  @Test
  public void httpclient() {
    whenUserAgent("Apache-HttpClient/release (java 1.5)");
    assertThat(underTest.isBrowserInitiated(request), is(false));
  }

  @Test
  public void missingUserAgent() {
    assertThat(underTest.isBrowserInitiated(request), is(false));
  }
}
