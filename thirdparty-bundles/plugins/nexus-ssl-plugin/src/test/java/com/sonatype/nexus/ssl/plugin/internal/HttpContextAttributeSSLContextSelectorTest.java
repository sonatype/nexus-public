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
package com.sonatype.nexus.ssl.plugin.internal;

import javax.net.ssl.SSLContext;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.httpclient.SSLContextSelector;
import org.sonatype.nexus.ssl.TrustStore;

import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

public class HttpContextAttributeSSLContextSelectorTest
    extends TestSupport
{
  @Mock
  private TrustStore trustStore;

  @Mock
  private SSLContext sslContext;

  private HttpContext httpContext;

  private HttpContextAttributeSSLContextSelector sslContextSelector;

  @Before
  public void setUp() {
    when(trustStore.getSSLContext()).thenReturn(sslContext);
    httpContext = new BasicHttpContext();
    sslContextSelector = new HttpContextAttributeSSLContextSelector(trustStore);
  }

  @Test
  public void testSelect_AttributeIsNull() {
    assertThat(sslContextSelector.select(httpContext), is(nullValue()));
  }

  @Test
  public void testSelect_AttributeIsFalse() {
    httpContext.setAttribute(SSLContextSelector.USE_TRUST_STORE, false);
    assertThat(sslContextSelector.select(httpContext), is(nullValue()));
  }

  @Test
  public void testSelect_AttributeIsTrue() {
    httpContext.setAttribute(SSLContextSelector.USE_TRUST_STORE, true);
    assertThat(sslContextSelector.select(httpContext), is(sslContext));
  }
}
