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
package org.sonatype.nexus.internal.httpclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicListHeaderIterator;
import org.apache.http.protocol.HttpContext;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Test for {@link NexusConnectionKeepAliveStrategy}.
 */
public class NexusConnectionKeepAliveStrategyTest
    extends TestSupport
{

  @Mock
  protected HttpResponse httpResponse;

  @Mock
  protected HttpContext httpContext;

  protected NexusConnectionKeepAliveStrategy subject;

  @Before
  public void prepare() {
    subject = new NexusConnectionKeepAliveStrategy(5000l);
    Mockito.when(httpResponse.headerIterator(Mockito.anyString()))
        .thenReturn(new BasicListHeaderIterator(
            Collections.<Header>emptyList(), null));
  }

  @Test
  public void constructorValueValidationBad1() {
    doConstructorValueValidation(-100, true);
  }

  @Test
  public void constructorValueValidationBad2() {
    doConstructorValueValidation(-1, true);
  }

  @Test
  public void constructorValueValidationGood1() {
    doConstructorValueValidation(0, false);
  }

  @Test
  public void constructorValueValidationGood2() {
    doConstructorValueValidation(30000, false);
  }

  protected void doConstructorValueValidation(final long maxKeepAlive, final boolean shouldFail) {
    try {
      new NexusConnectionKeepAliveStrategy(maxKeepAlive);
      MatcherAssert.assertThat("Value " + maxKeepAlive + " does not fails but should!", !shouldFail);
    }
    catch (IllegalArgumentException e) {
      MatcherAssert.assertThat("Value " + maxKeepAlive + " fails but should not!", shouldFail);
    }
  }

  // ==

  @Test
  public void keepAliveDefaulted() {
    // server response says nothing
    // nexus default wins
    final long keepAlive =
        subject.getKeepAliveDuration(httpResponse, httpContext);
    MatcherAssert.assertThat(keepAlive, Matchers.is(5000l));
  }

  @Test
  public void keepAliveServerValueIfLess() {
    // server response says 3s
    // server wins
    final List<Header> headers = new ArrayList<Header>(1);
    headers.add(new BasicHeader("Keep-Alive", "timeout=3"));
    Mockito.when(httpResponse.headerIterator(Mockito.anyString()))
        .thenReturn(new BasicListHeaderIterator(
            headers, null));
    final long keepAlive =
        subject.getKeepAliveDuration(httpResponse, httpContext);
    MatcherAssert.assertThat(keepAlive, Matchers.is(3000l));
  }

  @Test
  public void keepAliveServerValueIfLessWithMaxConnCount() {
    // server response says 3s
    // server wins
    final List<Header> headers = new ArrayList<Header>(1);
    headers.add(new BasicHeader("Keep-Alive", "timeout=3, max=100"));
    Mockito.when(httpResponse.headerIterator(Mockito.anyString()))
        .thenReturn(new BasicListHeaderIterator(
            headers, null));
    final long keepAlive =
        subject.getKeepAliveDuration(httpResponse, httpContext);
    MatcherAssert.assertThat(keepAlive, Matchers.is(3000l));
  }

  @Test
  public void keepAliveDefaultValueIfMore() {
    // server response says 8s
    // nexus wins (is capped)
    final List<Header> headers = new ArrayList<Header>(1);
    headers.add(new BasicHeader("Keep-Alive", "timeout=8"));
    Mockito.when(httpResponse.headerIterator(Mockito.anyString()))
        .thenReturn(new BasicListHeaderIterator(
            headers, null));
    final long keepAlive =
        subject.getKeepAliveDuration(httpResponse, httpContext);
    MatcherAssert.assertThat(keepAlive, Matchers.is(5000l));
  }

}
